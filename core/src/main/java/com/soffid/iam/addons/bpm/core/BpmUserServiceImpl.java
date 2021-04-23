package com.soffid.iam.addons.bpm.core;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.resource.spi.work.ExecutionContext;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;

import com.soffid.iam.ServiceLocator;
import com.soffid.iam.addons.bpm.common.Attribute;
import com.soffid.iam.addons.bpm.common.Constants;
import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.Node;
import com.soffid.iam.addons.bpm.common.NodeType;
import com.soffid.iam.addons.bpm.common.PageInfo;
import com.soffid.iam.addons.bpm.common.RoleRequestInfo;
import com.soffid.iam.addons.bpm.common.Trigger;
import com.soffid.iam.api.Account;
import com.soffid.iam.api.Group;
import com.soffid.iam.api.RoleGrant;
import com.soffid.iam.api.User;
import com.soffid.iam.api.UserAccount;
import com.soffid.iam.bpm.api.ProcessInstance;
import com.soffid.iam.bpm.api.TaskInstance;
import com.soffid.iam.common.security.SoffidPrincipal;
import com.soffid.iam.model.UserEntity;
import com.soffid.iam.security.SoffidPrincipalImpl;
import com.soffid.iam.service.AuthorizationService;
import com.soffid.iam.utils.Security;

import es.caib.seycon.ng.comu.AccountType;
import es.caib.seycon.ng.exception.InternalErrorException;
import es.caib.seycon.ng.exception.UnknownUserException;

public class BpmUserServiceImpl extends BpmUserServiceBase {

	@Override
	protected PageInfo handleGetPageInfo(ProcessInstance proc) throws Exception {
		JbpmContext ctx = getBpmEngine().getContext();
		try {
			org.jbpm.graph.exe.ProcessInstance process = ctx.loadProcessInstance(proc.getId());
			ProcessDefinition def = process.getProcessDefinition();
			InputStream in = def.getFileDefinition().getInputStream("task#process");
			if (in == null)
			{
				PageInfo pi = new PageInfo();
				pi.setFields(new Field[0]);
				pi.setAttributes(new Attribute[0]);
				pi.setTriggers(new Trigger[0]);
				return pi;
			}
			ObjectInputStream o = new ObjectInputStream(in);
			PageInfo pi = (PageInfo) o.readObject();
			
			return pi;
		} finally {
			ctx.close();
		}
	}

	@Override
	protected PageInfo handleGetPageInfo(TaskInstance task) throws Exception {
		JbpmContext ctx = getBpmEngine().getContext();
		try {
			InputStream in;
			if (task.isDummyTask())
			{
				ProcessDefinition def = ctx.getGraphSession().getProcessDefinition( task.getProcessDefinition() ) ;
				in = def.getFileDefinition().getInputStream("task#start");
			}
			else
			{
				org.jbpm.taskmgmt.exe.TaskInstance ti = ctx.getTaskInstance(task.getId());
				long nodeId = ti.getTask().getParent().getId();
				org.jbpm.graph.exe.ProcessInstance process = ti.getToken().getProcessInstance();
				ProcessDefinition def = process.getProcessDefinition();
				in = def.getFileDefinition().getInputStream("task#"+nodeId);
			}
			if (in == null)
			{
				PageInfo pi = new PageInfo();
				pi.setFields(new Field[0]);
				pi.setAttributes(new Attribute[0]);
				pi.setTriggers(new Trigger[0]);
				return pi;
			}
			ObjectInputStream o = new ObjectInputStream(in);
			PageInfo pi = (PageInfo) o.readObject();
			
			return pi;
		} finally {
			ctx.close();
		}
	}

	@Override
	protected PageInfo handleGetPageInfoByNodeId(Long nodeId) throws Exception {
		JbpmContext ctx = getBpmEngine().getContext();
		try {
			InputStream in;

			org.jbpm.graph.def.Node node = (org.jbpm.graph.def.Node) ctx.getSession().get(org.jbpm.graph.def.Node.class, nodeId);
			ProcessDefinition def = node.getProcessDefinition();
			in = def.getFileDefinition().getInputStream("task#"+nodeId);
			if (in == null)
			{
				PageInfo pi = new PageInfo();
				pi.setFields(new Field[0]);
				pi.setAttributes(new Attribute[0]);
				pi.setTriggers(new Trigger[0]);
				return pi;
			}
			ObjectInputStream o = new ObjectInputStream(in);
			PageInfo pi = (PageInfo) o.readObject();
			
			return pi;
		} finally {
			ctx.close();
		}
	}

	@Override
	protected String handleProcessAnonymousAction(String hash) throws Exception {
		String actionType = null;
		String split[] = hash.split("\\.");
		// 0 = process
		// 1 = task
		// 2 = transition hast
		// 3 = user
		if (split.length != 4)
			throw new InternalErrorException("Wrong token");
		
		JbpmContext ctx = getBpmEngine().getContext();
		try {
			InputStream in;
			
			org.jbpm.graph.exe.ProcessInstance p = ctx.getProcessInstance(Long.parseLong(split[0]));
			org.jbpm.taskmgmt.exe.TaskInstance ti = ctx.getTaskInstance(Long.parseLong(split[1]));
			if (ti.hasEnded() || ti == null || ti.getProcessInstance() != p)
				throw new InternalErrorException ("This task has already been finished");
			
			
			org.jbpm.graph.exe.ExecutionContext executionContext = new org.jbpm.graph.exe.ExecutionContext(p.getRootToken());
			
			String action = (String) executionContext.getVariable("actions_"+split[1]+"_"+split[2]);
			if (action == null)
			{
				throw new InternalErrorException ("This task has already been finished");
			}
			
			if (ti.getStart() != null)
				throw new InternalErrorException("This task is already started by "+ti.getActorId());
			
			ti.start();
			String user = (String) executionContext.getVariable("actions_user_translator_"+split[3]);
			if (user == null)
				throw new InternalErrorException("Wrong link");
			
			SoffidPrincipal principal = handleGetAnonymousActionPrincipal(hash);
			Security.nestedLogin(principal);
			try {
				Set<String> perms = new HashSet<String>();
				for ( String r: principal.getRoles()) perms.add("auth:"+r);
				for ( String r: principal.getGroupsAndRoles()) perms.add(r);
				int i = action.indexOf(":");
				actionType = i >= 0? action.substring(0,i): "";
				String transition = i >= 0? action.substring(i+1): action;
				if (actionType.equalsIgnoreCase("approve") || actionType.equalsIgnoreCase("deny")) {
					List<RoleRequestInfo> roles = (List<RoleRequestInfo>) executionContext.getVariable(Constants.ROLES_VAR); // $NON-NLS-1$
					for (RoleRequestInfo role : roles) {
						Set<String> owners = role.getOwners();
						if ( ! Collections.disjoint(perms, owners)) {
							if (! role.isApproved() && !role.isDenied()) {
								if (actionType.equalsIgnoreCase("approve"))
									role.setApproved(true);
								else
									role.setApproved(false);
							}
						}
					}
					executionContext.setVariable(Constants.ROLES_VAR, roles);
				} 
				ti.end(transition);
				
				ctx.save(p);
			} finally {
				Security.nestedLogoff();
			}
			return actionType;
		} finally {
			ctx.close();
		}
	}

	private List<String> getUserGroups(String user) throws InternalErrorException, UnknownUserException {
    	List<String> result = new LinkedList<String>();
		User u = ServiceLocator.instance().getUserService().findUserByUserName( user );
		Collection<Group> groups;
		groups = ServiceLocator.instance().getUserService().getUserGroupsHierarchy(u.getId());
		for ( Group g: groups)
			result.add(g.getName());
    	return result;
	}


    private List<String> getUserRoles(String user) throws InternalErrorException {
    	List<String> result = new LinkedList<String>();
    	Collection<RoleGrant> groups;
		User u = ServiceLocator.instance().getUserService().findUserByUserName( user );
		result.add(u.getUserName());
		groups = ServiceLocator.instance().getApplicationService().findEffectiveRoleGrantByUser(u.getId());
    	
    	com.soffid.iam.api.System soffidSystem = ServiceLocator.instance().getDispatcherService().findSoffidDispatcher();
    	for ( RoleGrant grant: groups)
    	{
    		if (grant.getSystem().equals(soffidSystem.getName()))
    			result.add(grant.getRoleName());
    		result.add(grant.getRoleName()+"@"+grant.getSystem());
    		if (grant.getDomainValue() != null)
    		{
    			if (grant.getSystem().equals(soffidSystem.getName()))
    				result.add(grant.getRoleName()+"/"+grant.getDomainValue());
    			result.add(grant.getRoleName()+"/"+grant.getDomainValue()+"@"+grant.getSystem());
    		}
    	}
    	return result;
	}

	@Override
	protected SoffidPrincipal handleGetAnonymousActionPrincipal(String hash) throws Exception {
		String actionType = null;
		String split[] = hash.split("\\.");
		// 0 = process
		// 1 = task
		// 2 = transition hast
		// 3 = user
		if (split.length != 4)
			throw new InternalErrorException("Wrong token");
		
		JbpmContext ctx = getBpmEngine().getContext();
		try {
			org.jbpm.graph.exe.ProcessInstance p = ctx.getProcessInstance(Long.parseLong(split[0]));
			org.jbpm.taskmgmt.exe.TaskInstance ti = ctx.getTaskInstance(Long.parseLong(split[1]));
			if (ti.hasEnded() || ti == null || ti.getProcessInstance() != p)
				throw new InternalErrorException ("This task has already been finished");
			
			
			org.jbpm.graph.exe.ExecutionContext executionContext = new org.jbpm.graph.exe.ExecutionContext(p.getRootToken());
			
			String action = (String) executionContext.getVariable("actions_"+split[1]+"_"+split[2]);
			if (action == null)
			{
				throw new InternalErrorException ("This task has already been finished");
			}
			
			String user = (String) executionContext.getVariable("actions_user_translator_"+split[3]);
			if (user == null)
				throw new InternalErrorException("Wrong link");
			
			Security.nestedLogin(user, Security.ALL_PERMISSIONS);
			try {
				List<UserAccount> accounts = getAccountService().findUsersAccounts(user, getDispatcherService().findSoffidDispatcher().getName());
				if (accounts == null)
					return null;
				
				List<String> g = getUserGroups(user);
				List<String> r = getUserRoles(user);
				UserEntity u = getUserEntityDao().findByUserName(user);
				List<String> perms = getRoles(accounts.get(0));
				return new SoffidPrincipalImpl(accounts.get(0).getName(), user, u.getFullName(), null, perms, g, r);
			} finally {
				Security.nestedLogoff();
			}
		} finally {
			ctx.close();
		}
	}
	
	private List<String> getRoles(Account acc) throws InternalErrorException {
		AuthorizationService us = ServiceLocator.instance()
				.getAuthorizationService();

		String[] rolesArray = getAuthorizationService().getUserAuthorizationsString(acc.getName(),
					new HashMap<String, String>());
		return new LinkedList<String>(Arrays.asList(rolesArray));
	}

}
