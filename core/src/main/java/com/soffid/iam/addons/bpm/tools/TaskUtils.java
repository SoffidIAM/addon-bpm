package com.soffid.iam.addons.bpm.tools;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.apache.commons.beanutils.PropertyUtils;

import com.soffid.iam.EJBLocator;
import com.soffid.iam.ServiceLocator;
import com.soffid.iam.addons.bpm.common.RoleRequestInfo;
import com.soffid.iam.api.Account;
import com.soffid.iam.api.Application;
import com.soffid.iam.api.DataType;
import com.soffid.iam.api.MetadataScope;
import com.soffid.iam.api.Role;
import com.soffid.iam.api.RoleAccount;
import com.soffid.iam.api.RoleGrant;
import com.soffid.iam.api.User;
import com.soffid.iam.bpm.api.TaskInstance;
import com.soffid.iam.service.ApplicationService;

import es.caib.seycon.ng.comu.AccountType;
import es.caib.seycon.ng.exception.InternalErrorException;
import es.caib.seycon.ng.utils.Security;

public class TaskUtils {
	public static void loadUserDetails (Map variables, String user) throws Exception
	{
		User u = com.soffid.iam.EJBLocator.getUserService().findUserByUserName(user);
		if (u != null)
		{
			Map<String, Object> atts = com.soffid.iam.EJBLocator.getUserService().findUserAttributes(user);
			if (atts == null)
				atts = new HashMap<String, Object>();
			for (DataType dt: ServiceLocator.instance().getAdditionalDataService().findDataTypes2(MetadataScope.USER))
			{
				if (dt.getBuiltin() != null && dt.getBuiltin().booleanValue())
				{
					Object o = PropertyUtils.getProperty(u, dt.getCode());
					variables.put(dt.getCode(), o);
				}
				else
				{
					variables.put(dt.getCode(), atts.get(dt.getCode()));
				}
			}
			populatePermissions(variables, u);
			if (! "D".equals(variables.get("action")))
			{
				if (u.getActive().booleanValue())
					variables.put("action", "E");
				else
					variables.put("action", "M");
			}
		}

	}
	
	public static void populatePermissions(Map variables, User u) throws Exception {
		// Elevo los permisos:
		Security.nestedLogin(Security.getCurrentAccount(), new String[] { Security.AUTO_METADATA_UPDATE_ALL,
				Security.AUTO_ACCOUNT_QUERY, Security.AUTO_ACCOUNT_QUERY + Security.AUTO_ALL,
				Security.AUTO_USER_QUERY + Security.AUTO_ALL, 
				Security.AUTO_USER_ROLE_QUERY + Security.AUTO_ALL,
				Security.AUTO_USER_ROLE_CREATE + Security.AUTO_ALL,
				Security.AUTO_USER_ROLE_DELETE + Security.AUTO_ALL, Security.AUTO_USER_CREATE + Security.AUTO_ALL,
				Security.AUTO_USER_UPDATE + Security.AUTO_ALL, Security.AUTO_USER_GROUP_CREATE + Security.AUTO_ALL,
				Security.AUTO_USER_GROUP_DELETE + Security.AUTO_ALL,
				Security.AUTO_USER_SET_PASSWORD + Security.AUTO_ALL,
				Security.AUTO_USER_UPDATE_PASSWORD + Security.AUTO_ALL,
				Security.AUTO_APPLICATION_QUERY + Security.AUTO_ALL, Security.AUTO_GROUP_CREATE + Security.AUTO_ALL,
				Security.AUTO_GROUP_QUERY + Security.AUTO_ALL, Security.AUTO_ROLE_QUERY + Security.AUTO_ALL });
		try {

			// es.caib.seycon.ng.servei.ejb.AplicacioService appSvc =
			// EJBLocator.getAplicacioService();

			ApplicationService appSvc = ServiceLocator.instance().getApplicationService();

			Collection<RoleAccount> userGrants = u == null ? new LinkedList<RoleAccount>()
					: appSvc.findUserRolesByUserName(u.getUserName());
			
			LinkedList<RoleRequestInfo> grants = (LinkedList<RoleRequestInfo>) variables.get("grants");
			if (grants == null)
			{
				grants = new LinkedList<RoleRequestInfo>();
				variables.put("grants", grants);
			}
			grants.clear();

			Long role = null;
			for (RoleAccount ra : userGrants) {
				RoleRequestInfo ri = null;
				ri = generateRoleRequestInfo(ra);
				if (ri != null) {
					grants.add(ri);
					createChildRolesNoRefresh(grants, grants.size()-1);
				}
			}
		} finally {
			Security.nestedLogoff();
		}
	}

	public static RoleRequestInfo generateRoleRequestInfo(RoleAccount ra) 
			throws InternalErrorException, NamingException, CreateException {
		RoleRequestInfo ri = null;
		ApplicationService appSvc = ServiceLocator.instance().getApplicationService();
		Role r = appSvc.findRoleByNameAndSystem(ra.getRoleName(), ra.getSystem());
		if (r.getBpmEnforced() != null && r.getBpmEnforced().booleanValue()) {
			Application app = appSvc.findApplicationByApplicationName(r.getInformationSystemName());
			if (app.getBpmEnforced() != null && app.getBpmEnforced().booleanValue()) {
				ri  = new RoleRequestInfo();
				ri.setApplicationName( r.getInformationSystemName());
				ri.setPreviousRoleId(r.getId());
				ri.setPreviousRoleDescription(r.getDescription());
				ri.setRoleId(r.getId());
				ri.setUserName(ra.getUserCode());
				ri.setUserFullName( app.getDescription());
				ri.setRoleDescription(r.getDescription());
				ri.setDelegateTo(ra.getDelegateAccount());
				ri.setDelegateUntil(ra.getDelegateUntil());
				ri.setPreviousDelegateTo(ra.getDelegateAccount());
				ri.setPreviousDelegateUntil(ra.getDelegateUntil());
				ri.setApplicationDescription(app.getDescription());
				ri.setRoleAccountId(ra.getId());
				if (ri.getDelegateTo() != null) {
					Account account = EJBLocator.getAccountService().findAccount(ri.getDelegateTo(), ra.getSystem());
					if (account != null && account.getType() == AccountType.USER) 
						ri.setDelegateToUser(account.getOwnerUsers().iterator().next());
						
				}
			}
		}
		return ri;
	}

	public static void createChildRolesNoRefresh(List<RoleRequestInfo> grants, int i) throws Exception {
		RoleRequestInfo app = grants.get(i);
		Long roleId = (Long) app.getRoleId();
		if (roleId != null)
		{
			Security.nestedLogin(Security.getCurrentAccount(), 
					new String [] {
				Security.AUTO_ROLE_QUERY+Security.AUTO_ALL,
				Security.AUTO_APPLICATION_QUERY+Security.AUTO_ALL
			});
			try {
				Role r = ServiceLocator.instance().getApplicationService().findRoleById(roleId);
				int first = i+1;
				List<RoleGrant> ownedRoles = new LinkedList<RoleGrant>( r.getOwnedRoles() );
				Collections.sort(ownedRoles, new Comparator<RoleGrant>() {
					public int compare(RoleGrant o1, RoleGrant o2) {
						int o = o1.getInformationSystem().compareTo(o2.getInformationSystem());
						if (o == 0)
							o = o1.getRoleName().compareTo(o2.getRoleName());
						return o;
					}
					
				});
				for ( RoleGrant grant: ownedRoles)
				{
					if (!alreadyIncluded(grants, grant)) {
						if (grant.getMandatory() || ! roleId.equals(app.getPreviousRoleId())) {
							Role r2 = ServiceLocator.instance().getApplicationService().findRoleById(grant.getRoleId());
							Application appDesc = ServiceLocator.instance().getApplicationService().findApplicationByApplicationName(r2.getInformationSystemName());
							RoleRequestInfo ri = new RoleRequestInfo();
							ri.setApplicationName(r2.getInformationSystemName());
							ri.setApplicationDescription(appDesc.getDescription());
							ri.setUserName(null);
							ri.setRoleId(r2.getId());
							ri.setSuggestedRoleId(r2.getId());
							ri.setParentRole(roleId);
							ri.setRoleDescription(r2.getDescription());
							ri.setMandatory (Boolean.TRUE.equals(grant.getMandatory()));
							grants.add(++i, ri);
						}
					}
				}
			} finally {
				Security.nestedLogoff();
			}
		}
	}

	private static boolean alreadyIncluded(List<RoleRequestInfo> grants, RoleGrant grant) {
		for (RoleRequestInfo gg: grants) {
			if (gg.getRoleId() != null && gg.getRoleId().equals(grant.getRoleId()))
				return true;
		}
		return false;
	}
	

}
