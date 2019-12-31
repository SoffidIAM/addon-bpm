package com.soffid.iam.addons.bpm.handler;

import java.util.LinkedList;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

import com.soffid.iam.ServiceLocator;
import com.soffid.iam.addons.bpm.common.Constants;
import com.soffid.iam.addons.bpm.common.RoleRequestInfo;
import com.soffid.iam.api.Role;
import com.soffid.iam.api.RoleAccount;
import com.soffid.iam.api.User;
import com.soffid.iam.service.ApplicationService;
import com.soffid.iam.service.UserService;
import com.soffid.iam.utils.Security;

public class StartHandler implements ActionHandler {
	UserService userService = ServiceLocator.instance().getUserService();
	ApplicationService appService = ServiceLocator.instance().getApplicationService();
	

	@Override
	public void execute(ExecutionContext executionContext) throws Exception {
		Security.nestedLogin(Security.ALL_PERMISSIONS);
		try {
			if (executionContext.getVariable(Constants.REQUESTER_VAR) == null &&
					executionContext.getVariable("request") != null &&
					executionContext.getVariable("requesterAccount") != null &&
					executionContext.getVariable("requesterUser") != null &&
					executionContext.getVariable("action") != null)
			{
				String userName = (String) executionContext.getVariable("requesterUser");
				executionContext.setVariable("$$RoleApproval$$", true);
				executionContext.setVariable(Constants.REQUESTER_VAR, userName);
				if (userName != null)
				{
					User u = userService.findUserByUserName(userName);
					if (u != null)
						executionContext.setVariable(Constants.REQUESTER_NAME_VAR, u.getFirstName());
				}
				RoleAccount ra = (RoleAccount) executionContext.getVariable("request");
				Role role = appService.findRoleByNameAndSystem(ra.getRoleName(), ra.getSystem());
				RoleRequestInfo rri = new RoleRequestInfo();
				rri.setApplicationName(ra.getInformationSystemName());
				rri.setApplicationDescription( com.soffid.iam.EJBLocator.getApplicationService().findApplicationByApplicationName(ra.getInformationSystemName()).getDescription() );
				rri.setApproved(false);
				rri.setComments(null);
				rri.setDenied(false);
				rri.setSodRisk ( ra.getSodRisk());
				rri.setOwners(null);
				rri.setOwnersString(null);
				rri.setParentRole(null);
				rri.setRoleAccount(ra);
				if ( "revoke".equals( executionContext.getVariable("action")))
				{
					rri.setPreviousRoleDescription(ra.getRoleDescription());
					rri.setPreviousRoleId(role.getId());
				} else {
					rri.setRoleDescription(ra.getRoleDescription());
					rri.setRoleId(role.getId());
				}
				rri.setUserName(ra.getUserCode());
				rri.setUserFullName(ra.getUserFullName());
				LinkedList<RoleRequestInfo> grants = new LinkedList<RoleRequestInfo>();
				grants.add(rri);
				executionContext.setVariable("grants", grants);
			}
		} finally {
			Security.nestedLogoff();
		}
	}

	
}
