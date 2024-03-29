package com.soffid.iam.addons.bpm.handler;

import java.util.LinkedList;
import java.util.List;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

import com.soffid.iam.ServiceLocator;
import com.soffid.iam.api.Account;
import com.soffid.iam.service.AccountService;
import com.soffid.iam.service.ApplicationService;
import com.soffid.iam.service.UserService;
import com.soffid.iam.utils.Security;

import es.caib.bpm.toolkit.exception.UserWorkflowException;

public class StartAccountHandler implements ActionHandler {
	UserService userService = ServiceLocator.instance().getUserService();
	ApplicationService appService = ServiceLocator.instance().getApplicationService();
	

	@Override
	public void execute(ExecutionContext executionContext) throws Exception {
		String user = Security.getCurrentUser();
		Security.nestedLogin(Security.ALL_PERMISSIONS);
		try {
			String accountName = (String) executionContext.getVariable("account");
			String dispatcherName = (String) executionContext.getVariable("systemName");
			if (accountName == null || accountName.trim().isEmpty() ||
					dispatcherName == null || dispatcherName.trim().isEmpty()) {
				throw new UserWorkflowException("Please, select an account");
			}
			AccountService accSvc = ServiceLocator.instance().getAccountService();
			Account account = accSvc.findAccount(accountName, dispatcherName);
			if (account == null)
				throw new UserWorkflowException("Cannot find account "+accountName+" at "+dispatcherName);
			List<String> owners = new LinkedList<String>();
			if (account.getOwnerUsers() != null) owners.addAll(account.getOwnerUsers());
			if (account.getOwnerRoles() != null) owners.addAll(account.getOwnerRoles());
			if (account.getOwnerGroups() != null) owners.addAll(account.getOwnerGroups());
			executionContext.setVariable("owners", owners);
			accSvc.registerAccountReservationProcess(account, user, executionContext.getProcessInstance().getId());
		} finally {
			Security.nestedLogoff();
		}
	}

	
}
