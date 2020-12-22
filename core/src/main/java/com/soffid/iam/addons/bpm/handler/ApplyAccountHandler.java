package com.soffid.iam.addons.bpm.handler;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.LogFactory;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.module.exe.ModuleInstance;

import com.soffid.iam.ServiceLocator;
import com.soffid.iam.addons.bpm.common.Constants;
import com.soffid.iam.addons.bpm.common.RoleRequestInfo;
import com.soffid.iam.api.Account;
import com.soffid.iam.api.BpmUserProcess;
import com.soffid.iam.api.DataType;
import com.soffid.iam.api.MetadataScope;
import com.soffid.iam.api.Role;
import com.soffid.iam.api.RoleAccount;
import com.soffid.iam.api.User;
import com.soffid.iam.service.AccountService;
import com.soffid.iam.service.ApplicationService;
import com.soffid.iam.service.UserService;
import com.soffid.iam.utils.Security;

import es.caib.bpm.toolkit.exception.UserWorkflowException;
import es.caib.seycon.ng.exception.InternalErrorException;

public class ApplyAccountHandler implements ActionHandler {
	org.apache.commons.logging.Log log = LogFactory.getLog(getClass());
	
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
		String user = Security.getCurrentUser();
		Security.nestedLogin(Security.ALL_PERMISSIONS);
		try {
			if ( "true".equals(applyUserChanges))
			{
				applyUserChanges(executionContext, user);
			}
			String userName = (String) executionContext.getVariable("requester");
			if (userName != null)
			{
				BpmUserProcess proc = new BpmUserProcess();
				proc.setProcessId(executionContext.getProcessInstance().getId());
				proc.setUserCode(userName);
				proc.setTerminated(false);
				ServiceLocator.instance().getUserService().create(proc);
			}
			executionContext.leaveNode();
		} finally {
			Security.nestedLogoff();
		}
	}

	private void applyUserChanges(ExecutionContext executionContext, String user) throws InternalErrorException, UserWorkflowException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		while (user.startsWith("*")) user = user.substring(1);
		String accountName = (String) executionContext.getVariable("account");
		String dispatcherName = (String) executionContext.getVariable("systemName");
		if (accountName == null || accountName.trim().isEmpty() ||
				dispatcherName == null || dispatcherName.trim().isEmpty()) {
			throw new UserWorkflowException("Please, select an account");
		}
		AccountService accSvc = ServiceLocator.instance().getAccountService();
		Account account = accSvc.findAccount(accountName, dispatcherName);
		Date until = (Date) executionContext.getVariable("until");
		if (until == null)
			until = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000); // One day
		accSvc.grantAcccountToUser(account, user, executionContext.getProcessInstance().getId(), until);
	}

}
