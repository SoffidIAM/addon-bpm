package com.soffid.iam.addons.bpm.handler;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.LogFactory;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.module.exe.ModuleInstance;

import com.soffid.iam.EJBLocator;
import com.soffid.iam.ServiceLocator;
import com.soffid.iam.addons.bpm.common.Constants;
import com.soffid.iam.addons.bpm.common.RoleRequestInfo;
import com.soffid.iam.api.BpmUserProcess;
import com.soffid.iam.api.Configuration;
import com.soffid.iam.api.DataType;
import com.soffid.iam.api.MetadataScope;
import com.soffid.iam.api.Role;
import com.soffid.iam.api.RoleAccount;
import com.soffid.iam.api.System;
import com.soffid.iam.api.User;
import com.soffid.iam.api.UserAccount;
import com.soffid.iam.common.security.SoffidPrincipal;
import com.soffid.iam.security.SoffidPrincipalImpl;
import com.soffid.iam.service.ApplicationService;
import com.soffid.iam.service.UserService;
import com.soffid.iam.utils.ConfigurationCache;
import com.soffid.iam.utils.Security;

import es.caib.bpm.toolkit.exception.UserWorkflowException;
import es.caib.seycon.ng.exception.AccountAlreadyExistsException;
import es.caib.seycon.ng.exception.InternalErrorException;
import es.caib.seycon.ng.exception.NeedsAccountNameException;

public class ApplyDelegationHandler implements ActionHandler {
	org.apache.commons.logging.Log log = LogFactory.getLog(getClass());
	
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
			String userName = (String) executionContext.getVariable("userName");
			if (userName != null)
			{
				if ( "true".equals(applyEntitlements))
				{
					applyEntitlements(userName, executionContext);
				}
				BpmUserProcess proc = new BpmUserProcess();
				proc.setProcessId(executionContext.getProcessInstance().getId());
				proc.setUserCode(userName);
				proc.setTerminated(false);
				ServiceLocator.instance().getUserService().create(proc);
			}
		} finally {
			Security.nestedLogoff();
		}
		executionContext.leaveNode();
	}

	private void applyEntitlements(String userName, ExecutionContext executionContext) throws UserWorkflowException, InternalErrorException, NamingException, CreateException, NeedsAccountNameException, AccountAlreadyExistsException {
		log.info("Applying delegations");
		// Ensure the configuration parameter is enabled
		String t = ConfigurationCache.getProperty("soffid.delegation.disable");
		if ("true".equals(t)) {
			Configuration c = EJBLocator.getConfigurationService().findParameterByNameAndNetworkName("soffid.delegation.disable", null);
			if (c != null) {
				c.setValue("false");
				EJBLocator.getConfigurationService().update(c);
			}
		}
		List<RoleRequestInfo> grants = (List<RoleRequestInfo>) executionContext.getVariable( Constants.ROLES_VAR );
		Collection<RoleAccount> ra = EJBLocator.getApplicationService().findUserRolesByUserName(userName);
		final ApplicationService applicationService = ServiceLocator.instance().getApplicationService();
		SoffidPrincipal old = Security.getSoffidPrincipal();
		Security.nestedLogin(Security.ALL_PERMISSIONS);
		try {
			for ( RoleRequestInfo grant: grants)
			{
				log.info("Processing grant "+grant);
				Role role = applicationService.findRoleById(grant.getRoleId());
				RoleAccount roleAccount = applicationService.findRoleAccountById(grant.getRoleAccountId());
				if (roleAccount != null) {
					SoffidPrincipal p = new SoffidPrincipalImpl(old.getName(), 
							userName, 
							userName, 
							null,
							Arrays.asList(old.getRoles()),
							Arrays.asList(old.getGroups()),
							Arrays.asList(old.getSoffidRoles()));
					Security.nestedLogin(p);
					try {
								
						if (roleAccount.getDelegateAccount() == null && grant.getDelegateToUser() != null) {
							// Delegate
							String accountName = getTargetAccountName(grant, roleAccount);
							EJBLocator.getEntitlementDelegationService().delegate(roleAccount, grant.getDelegateToUser(), accountName, new Date(), grant.getDelegateUntil());
						}
						else if (roleAccount.getDelegateAccount() != null && grant.getDelegateToUser() == null) {
							EJBLocator.getEntitlementDelegationService().cancelDelegation(roleAccount);
						}
						else if (roleAccount.getDelegateAccount() == null && grant.getDelegateTo() == null) {
							// Nothing to do
						}
						else if (roleAccount.getDelegateAccount().equals(grant.getDelegateTo()) && 
								roleAccount.getDelegateUntil().equals(grant.getDelegateUntil())) {
							// Nothing to do
						}
						else
						{
							EJBLocator.getEntitlementDelegationService().cancelDelegation(roleAccount);						
							String accountName = getTargetAccountName(grant, roleAccount);
							EJBLocator.getEntitlementDelegationService().delegate(roleAccount, grant.getDelegateToUser(), accountName, new Date(), grant.getDelegateUntil());
						}
					} finally {
						Security.nestedLogoff();
					}
				}
			}
		} finally {
			Security.nestedLogoff();
		}
	}

	private String getTargetAccountName(RoleRequestInfo grant, RoleAccount roleAccount) throws InternalErrorException,
			NamingException, CreateException, NeedsAccountNameException, AccountAlreadyExistsException {
		String accountName = grant.getDelegateTo();
		if (accountName == null) {
			User user = EJBLocator.getUserService().findUserByUserName(grant.getDelegateToUser());
			if (user == null)
				throw new InternalErrorException("Cannot find user "+grant.getDelegateToUser());
			System s = EJBLocator.getDispatcherService().findDispatcherByName(roleAccount.getSystem());
			if (s == null)
				throw new InternalErrorException("Cannot find system "+roleAccount.getSystem());
			UserAccount account = EJBLocator.getAccountService().createAccount(user, s, null);
			accountName = account.getName();
		}
		return accountName;
	}
	
}
