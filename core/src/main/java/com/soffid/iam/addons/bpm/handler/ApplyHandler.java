package com.soffid.iam.addons.bpm.handler;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

import com.soffid.iam.ServiceLocator;
import com.soffid.iam.addons.bpm.common.Constants;
import com.soffid.iam.addons.bpm.common.RoleRequestInfo;
import com.soffid.iam.api.DataType;
import com.soffid.iam.api.MetadataScope;
import com.soffid.iam.api.Role;
import com.soffid.iam.api.RoleAccount;
import com.soffid.iam.api.User;
import com.soffid.iam.service.ApplicationService;
import com.soffid.iam.service.UserService;
import com.soffid.iam.utils.Security;

import es.caib.bpm.toolkit.exception.UserWorkflowException;
import es.caib.seycon.ng.exception.InternalErrorException;

public class ApplyHandler implements ActionHandler {
	UserService userService = ServiceLocator.instance().getUserService();
	ApplicationService appService = ServiceLocator.instance().getApplicationService();
	
	String applyUserChanges;
	String applyEntitlements;
	
	public String getApplyUserChanges() {
		return applyUserChanges;
	}

	public void setApplyUserChanges(String applyUserChanges) {
		this.applyUserChanges = applyUserChanges;
	}

	public String getApplyEntitlements() {
		return applyEntitlements;
	}

	public void setApplyEntitlements(String applyEntitlements) {
		this.applyEntitlements = applyEntitlements;
	}

	@Override
	public void execute(ExecutionContext executionContext) throws Exception {
		Security.nestedLogin(Security.ALL_PERMISSIONS);
		try {
			if ( "true".equals(applyUserChanges))
			{
				applyUserChanges(executionContext);
			}
			if ( "true".equals(applyEntitlements))
			{
				applyEntitlements(executionContext);
			}
			executionContext.leaveNode();
		} finally {
			Security.nestedLogoff();
		}
	}

	private void applyEntitlements(ExecutionContext executionContext) throws UserWorkflowException, InternalErrorException {
		List<RoleRequestInfo> grants = (List<RoleRequestInfo>) executionContext.getVariable( Constants.ROLES_VAR );
		for ( RoleRequestInfo grant: grants)
		{
			if (grant.getUserName() == null)
				grant.setUserName( (String) executionContext.getVariable("UserName"));
			if (grant.getUserName() != null && !grant.isDenied() && grant.getParentRole() == null)
			{
				if (grant.getPreviousRoleId() != null && grant.getPreviousRoleId().equals(grant.getRoleId()))
				{
					// Do nothing
				}
				else if ( grant.getRoleId() == null )
				{
					revoke (grant);
				}
				else if ( grant.getPreviousRoleId() == null)
				{
					grant (grant);
				}
				else
				{
					revoke(grant);
					grant(grant);
				}
			}
		}
	}

	private void grant(RoleRequestInfo grant) throws InternalErrorException, UserWorkflowException {
		Role role = appService.findRoleById(grant.getRoleId());
		if (role == null)
			throw new UserWorkflowException("Unable to find role with id "+grant.getRoleId());
		RoleAccount ra = new RoleAccount();
		ra.setUserCode(grant.getUserName());
		ra.setRoleName(role.getName());
		ra.setSystem(role.getSystem());
		ra.setStartDate(new Date());
		appService.create(ra);
	}

	private void revoke(RoleRequestInfo grant) throws InternalErrorException, UserWorkflowException {
		Role role = appService.findRoleById(grant.getPreviousRoleId());
		if (role == null)
			throw new UserWorkflowException("Unable to find role with id "+grant.getRoleId());
		for ( RoleAccount ra: appService.findUserRolesByUserNameNoSoD(grant.getUserName()))
		{
			if (ra.getRoleName().equals(role.getName()) && ra.getSystem().equals(role.getSystem()))
				appService.delete(ra);
		}
	}

	private void applyUserChanges(ExecutionContext executionContext) throws InternalErrorException, UserWorkflowException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		String action = (String) executionContext.getVariable("action");
		String userSelector = (String) executionContext.getVariable("executionContext");
		User user = userSelector == null ? null: userService.findUserByUserName(userSelector);
		Map<String, Object> attributes = user == null? null: userService.findUserAttributes(user.getUserName());
		if ("D".equals(action))
		{
			if (user == null)
				throw new UserWorkflowException("No user has been selected");
			user.setActive(false);
			updateAttributes (user, attributes, executionContext);
			userService.update(user);
			userService.updateUserAttributes(user.getUserName(), attributes);
		}
		else if ("A".equals(action))
		{
			User u = new User();
			user.setActive(true);
			attributes = new HashMap<String, Object>();
			updateAttributes (user, attributes, executionContext);
			user = userService.create(user);
			userService.updateUserAttributes(user.getUserName(), attributes);
			
		}
		else if ("M".equals(action))
		{
			if (user == null)
				throw new UserWorkflowException("No user has been selected");
			updateAttributes (user, attributes, executionContext);
			userService.update(user);
			userService.updateUserAttributes(user.getUserName(), attributes);
		}
		else if ("E".equals(action))
		{
			if (user == null)
				throw new UserWorkflowException("No user has been selected");
			user.setActive(true);
			updateAttributes (user, attributes, executionContext);
			userService.update(user);
			userService.updateUserAttributes(user.getUserName(), attributes);
		}
	}

	private void updateAttributes(User user, Map<String, Object> attributes, ExecutionContext executionContext) throws InternalErrorException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		ContextInstance ci = executionContext.getContextInstance();
		for( DataType metadata: ServiceLocator.instance().getAdditionalDataService().findDataTypes2(MetadataScope.USER))
		{
			if (ci.hasVariable(metadata.getCode(), executionContext.getToken()))
			{
				Object value = ci.getVariable(metadata.getCode());
				if (metadata.getBuiltin() != null && metadata.getBuiltin().booleanValue())
					PropertyUtils.setProperty(user, metadata.getCode(), value);
				else
					attributes.put(metadata.getCode(), value);
			}
		}
	}
	
}
