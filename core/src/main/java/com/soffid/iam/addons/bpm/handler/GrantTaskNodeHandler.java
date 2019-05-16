package com.soffid.iam.addons.bpm.handler;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.DelegationException;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.jbpm.jpdl.el.impl.JbpmVariableResolver;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

import com.soffid.iam.ServiceLocator;
import com.soffid.iam.addons.bpm.common.Constants;
import com.soffid.iam.addons.bpm.common.RoleRequestInfo;
import com.soffid.iam.api.Application;
import com.soffid.iam.api.Role;
import com.soffid.iam.service.ApplicationService;
import com.soffid.iam.utils.Security;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.ParseException;
import bsh.TargetError;
import es.caib.bpm.toolkit.exception.SystemWorkflowException;
import es.caib.seycon.ng.exception.InternalErrorException;

public class GrantTaskNodeHandler implements ActionHandler {
	Log log = LogFactory.getLog(getClass());

	String script;
	String actor;
	final ApplicationService appService = ServiceLocator.instance().getApplicationService();

	@Override
	public void execute(ExecutionContext executionContext) throws Exception {
		TaskNode tn = (TaskNode) executionContext.getNode();
		List<RoleRequestInfo> roles = (List<RoleRequestInfo>) executionContext.getVariable(Constants.ROLES_VAR); // $NON-NLS-1$
		Security.nestedLogin(Security.ALL_PERMISSIONS);
		try {
			HashSet<String> ownersSet = new HashSet<String>();
			for (RoleRequestInfo role : roles) {

				Long roleId = (Long) role.getRoleId();
				Long previousRoleId = (Long) role.getPreviousRoleId();
				if ( role.getParentRole() != null)
				{
					for (RoleRequestInfo role2 : roles)
					{
						Long roleId2 = (Long) role.getRoleId();
						if (role.getParentRole().equals(roleId2))
						{
							role.setOwners(role2.getOwners());
							role.setOwnersString(role2.getOwnersString());
						}
					}
				} 
				else if ( role.isDenied() )
				{
					// Already denied grant => ignore
				}
				else if (roleId == null? previousRoleId == null : roleId.equals(previousRoleId))
				{
					// No change => ignore
				} else {
					Role r = appService.findRoleById(roleId == null ? previousRoleId : roleId);
					if (r != null) {
						Application app = appService.findApplicationByApplicationName(r.getInformationSystemName());
						String[] owners = findOwner(r, app, executionContext);
						HashSet<String> s = new HashSet<String>();
						for (String owner: owners) s.add(owner);
						role.setOwners( s );
						StringBuffer sb = new StringBuffer();
						for (String owner : owners)
							sb.append(owner).append(" ");
						if (sb.length() > 0)
						{
							ownersSet.add (sb.toString());
							role.setOwnersString(sb.toString());
						}
					}
				}

			}

			if (ownersSet.isEmpty())
			{
				executionContext.leaveNode();
			} else {
				Token token = executionContext.getToken();
				TaskMgmtInstance taskManagementInstance = (TaskMgmtInstance) token.getProcessInstance().getInstance(TaskMgmtInstance.class);
				for (String s: ownersSet )
				{
					TaskInstance ti = taskManagementInstance.createTaskInstance((Task) tn.getTasks().iterator().next(),
							executionContext);
					ti.setToken(token);
					ti.setTaskMgmtInstance(taskManagementInstance);
					executionContext.getJbpmContext().getSession().save(ti);
					String[] owners = null;
					List<RoleRequestInfo> grants = new LinkedList<RoleRequestInfo>();
					for (RoleRequestInfo role : roles) {
						if (s.equals(role.getOwnersString()))
						{
							owners = (String[]) role.getOwners().toArray(new String[role.getOwners().size()]);
							role.setTaskInstance( ti.getId());
						}
					}
					ti.setPooledActors(owners);
				}
				roles = new LinkedList<RoleRequestInfo> ( roles);
				executionContext.setVariable(Constants.ROLES_VAR, roles);
			}
		} catch (ParseException e) {
			throw new SystemWorkflowException(
					"Error parsing custom script " + executionContext.getNode().getName() + ": " + e.getMessage());
		} catch (TargetError e) {
			throw new InternalErrorException("Error executing custom script " + executionContext.getNode().getName()
					+ " at " + e.getScriptStackTrace(), e.getTarget());
		} catch (EvalError e) {
			String msg;
			try {
				msg = e.getMessage() + "[ " + e.getErrorText() + "] ";
			} catch (Exception e2) {
				msg = e.getMessage();
			}
			throw new InternalErrorException(
					"Error parsing custom script " + executionContext.getNode().getName() + ": " + msg);
		} finally {
			Security.nestedLogoff();
		}
	}

	private String[] findOwner(Role role, Application application, ExecutionContext executionContext) throws Exception {
		if (actor != null)
			return findActorOwner(role, application, executionContext);
		else
			return findScriptOwner(role, application, executionContext);
	}

	private String[] findScriptOwner(Role role, Application application, ExecutionContext executionContext) throws Exception {
		try {
			Interpreter interpreter = new Interpreter();
			ContextInstance contextInstance = executionContext.getContextInstance();
			// we copy all the variableInstances of the context into the interpreter
			Map<String, Object> variables = contextInstance.getVariables(executionContext.getToken());
			if (variables != null) {
				for (Map.Entry<String, Object> entry : variables.entrySet()) {
					String variableName = entry.getKey();
					Object variableValue = entry.getValue();
					interpreter.set(variableName, variableValue);
				}
			}
			interpreter.set("executionContext", executionContext);
			interpreter.set("token", executionContext.getToken());
			interpreter.set("node", executionContext.getNode());
			interpreter.set("task", executionContext.getTask());
			interpreter.set("taskInstance", executionContext.getTaskInstance());
			interpreter.set("role", role);
			interpreter.set("application", application);

			Object o = interpreter.eval(getScript());

			if (o == null)
				return null;
			else if (o instanceof String[])
				return (String[]) o;
			else
				return o.toString().split("[, ]+");

		} catch (TargetError e) {
			throw new DelegationException("script evaluation exception", e.getTarget());
		} catch (Exception e) {
			log.warn("exception during evaluation of script expression", e);
			// try to throw the cause of the EvalError
			if (e.getCause() instanceof Exception) {
				throw (Exception) e.getCause();
			} else if (e.getCause() instanceof Error) {
				throw (Error) e.getCause();
			} else {
				throw e;
			}
		}
	}

	private String[] findActorOwner(Role role, Application application, ExecutionContext executionContext) {
		JbpmVariableResolver variableResolver = new JbpmVariableResolver();
		RoleVariableResolver variableResolver2 = new RoleVariableResolver(role, application, variableResolver);
		Object o = JbpmExpressionEvaluator.evaluate(actor, executionContext, variableResolver2,
				JbpmExpressionEvaluator.getUsedFunctionMapper());
		if (o == null)
			return null;
		else if (o instanceof String[])
			return (String[]) o;
		else
			return o.toString().split("[, ]+");
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public String getActor() {
		return actor;
	}

	public void setActor(String actor) {
		this.actor = actor;
	}

}
