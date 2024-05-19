package com.soffid.iam.addons.bpm.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Date;
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
import org.jbpm.graph.def.Event;
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
import com.soffid.iam.api.RoleAccount;
import com.soffid.iam.bpm.mail.Mail;
import com.soffid.iam.interp.Evaluator;
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

	String shortcut;
	String script;
	String actor;
	String type;
	final ApplicationService appService = ServiceLocator.instance().getApplicationService();

	@Override
	public void execute(ExecutionContext executionContext) throws Exception {
		loadFile(executionContext);
		TaskNode tn = (TaskNode) executionContext.getNode();
		log.info("Creating tasks for node " +tn.getName()+" type "+type);
		List<RoleRequestInfo> roles = (List<RoleRequestInfo>) executionContext.getVariable(Constants.ROLES_VAR); // $NON-NLS-1$
		Security.nestedLogin("bpm-engine", Security.ALL_PERMISSIONS);
		try {
			HashSet<String> ownersSet = new HashSet<String>();
			for (RoleRequestInfo role : roles) {
				log.info("Checking "+role);
				if ( applies (role) &&  !  role.isMandatory())
				{
					log.info("Applies");
					Long roleId = (Long) role.getRoleId();
					Long previousRoleId = (Long) role.getPreviousRoleId();
					if ( role.getParentRole() != null)
					{
						for (RoleRequestInfo role2 : roles)
						{
							Long roleId2 = (Long) role2.getRoleId();
							if (role.getParentRole().equals(roleId2))
							{
								role.setOwners(role2.getOwners());
								role.setOwnersString(role2.getOwnersString());
							}
						}
					} 
					else if (roleId == null? previousRoleId == null : roleId.equals(previousRoleId))
					{
						// No change => ignore
					} else {
						Role r = appService.findRoleById(roleId == null ? previousRoleId : roleId);
						log.info("Got role definition "+r);
						if (r != null) {
							Application app = appService.findApplicationByApplicationName(r.getInformationSystemName());
							String[] owners = findOwner(role, r, app, executionContext);
							HashSet<String> s = new HashSet<String>();
							if (owners != null)
							{
								for (String owner: owners) s.add(owner.trim());
								role.setOwners( s );
								StringBuffer sb = new StringBuffer();
								for (String owner : owners)
									sb.append(owner).append(" ");
								if (sb.length() > 0)
								{
									ownersSet.add (sb.toString());
									role.setOwnersString(sb.toString());
								}
							} else {
								role.setOwnersString(null);
								role.setOwners(s);
							}
						}
					}
				}

			}

			if (ownersSet == null || ownersSet.isEmpty())
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
					if (owners != null && owners.length > 0) {
						ActionHandler handler;
						if ("true".equals(shortcut)) {
							MailShortcut ms = new MailShortcut();
							ms.setTemplate("delegate");
							handler = ms;
						} else {
							Mail ms = new Mail();
							ms.setTemplate("delegate");
							handler = ms;
						}
						ExecutionContext ctx2 = new ExecutionContext(token);
						ctx2.setTaskInstance(ti);
						handler.execute(ctx2);
					}
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

	/*
	 *
	 * 											<listitem value="enter" label="${c:l('bpm.grantTypeList') }"/>
											<listitem value="request" label="${c:l('bpm.grantTypeRequest') }"/>
											<listitem value="displayPending" label="${c:l('bpm.grantTypeDisplayPending') }"/>
											<listitem value="displayAll" label="${c:l('bpm.grantTypeDisplayAll') }"/>
											<listitem value="displayApproved" label="${c:l('bpm.grantDisplayApproved') }"/>
											<listitem value="displayRejected" label="${c:l('bpm.grantTypeDisplayRejected') }"/>

	 */
	private boolean applies(RoleRequestInfo role) {
		return 
			type == null ? true : 
			role.isApproved() ? "enter".equals(type) || "displayAll".equals(type) || "displayApproved".equals(type) :
			role.isDenied() ? "enter".equals(type) || "displayAll".equals(type) || "displayRejected".equals(type) :
				"enter".equals(type) || "displayAll".equals(type) || "displayPending".equals(type) ;
	}

	private String[] findOwner(RoleRequestInfo request, Role role, Application application, ExecutionContext executionContext) throws Exception {
		if (actor != null && !actor.trim().isEmpty())
			return findActorOwner(role, application, executionContext);
		else if (script != null && !script.trim().isEmpty())
			return findScriptOwner(request, role, application, executionContext);
		else return null;
	}

	private String[] findScriptOwner(RoleRequestInfo request, Role role, Application application, ExecutionContext executionContext) throws Exception {
		try {
			if (request.getSodRisk() == null)
			{
				List<RoleAccount> ra;
				if ( request.getUserName() != null)
					ra = new LinkedList<RoleAccount> (
							ServiceLocator
								.instance()
								.getApplicationService()
								.findUserRolesByUserName(request.getUserName()));
				else
					ra = new LinkedList<RoleAccount>();
				RoleAccount grant = new RoleAccount();
				grant.setUserCode(request.getUserName());
				grant.setRoleName(role.getName());
				grant.setSystem(role.getSystem());
				grant.setStartDate(new Date());
				ra.add(grant);
				ServiceLocator.instance().getSoDRuleService().qualifyRolAccountList(ra);
				request.setSodRisk(grant.getSodRisk());
			}
			log.info("Evaluating script " + script);
			Map<String,Object> vars = new HashMap<>();
			ContextInstance contextInstance = executionContext.getContextInstance();
			// we copy all the variableInstances of the context into the interpreter
			Map<String, Object> variables = contextInstance.getVariables(executionContext.getToken());
			if (variables != null) {
				for (Map.Entry<String, Object> entry : variables.entrySet()) {
					String variableName = entry.getKey();
					Object variableValue = entry.getValue();
					vars.put(variableName, variableValue);
				}
			}
			vars.put("executionContext", executionContext);
			vars.put("serviceLocator", ServiceLocator.instance());
			vars.put("token", executionContext.getToken());
			vars.put("node", executionContext.getNode());
			vars.put("task", executionContext.getTask());
			vars.put("taskInstance", executionContext.getTaskInstance());
			vars.put("role", role);
			vars.put("request", request);
			vars.put("application", application);

			Object o = Evaluator.instance().evaluate(getScript(), vars, executionContext.getNode().getName() );

			if (o == null || o.toString().trim().isEmpty())
				return null;
			else if (o.getClass().isArray()) {
				String[] r = new String[Array.getLength(o)];
				for ( int i = 0; i < r.length; i++) {
					r[i] = Array.get(o, i).toString();					
				}
				return r;
			}
			else if (o instanceof List)
				return (String[]) ((List)o).toArray(new String[0]);
			else
				return o.toString().split(" *, *");

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
		log.info("Evaluating "+actor);
		JbpmVariableResolver variableResolver = new JbpmVariableResolver();
		RoleVariableResolver variableResolver2 = new RoleVariableResolver(role, application, variableResolver);
		Object o = JbpmExpressionEvaluator.evaluate(actor, executionContext, variableResolver2,
				JbpmExpressionEvaluator.getUsedFunctionMapper());
		if (o == null || o.toString().trim().isEmpty())
			return null;
		else if (o instanceof String[])
			return (String[]) o;
		else
			return o.toString().split(" *, *");
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getShortcut() {
		return shortcut;
	}

	public void setShortcut(String shortcut) {
		this.shortcut = shortcut;
	}

	String file; 
	private void loadFile(ExecutionContext executionContext) throws InternalErrorException, IOException {
		if (file != null) {
			InputStream in = executionContext.getProcessDefinition().getFileDefinition().getInputStream(file);
			if (in == null)
				throw new InternalErrorException("Cannot find resource "+file);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			for (int read = in.read(); read >= 0; read = in.read())
				out.write(read);
			in.close();
			out.close();
			script = out.toString("UTF-8");
		}
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}
	

}
