package com.soffid.iam.addons.bpm.ui;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.au.out.AuInvoke;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import com.soffid.iam.EJBLocator;
import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.NodeType;
import com.soffid.iam.addons.bpm.common.RoleRequestInfo;
import com.soffid.iam.addons.bpm.common.WorkflowType;
import com.soffid.iam.addons.bpm.tools.TaskUtils;
import com.soffid.iam.api.Account;
import com.soffid.iam.api.Application;
import com.soffid.iam.api.Role;
import com.soffid.iam.api.RoleAccount;
import com.soffid.iam.api.User;
import com.soffid.iam.api.UserAccount;
import com.soffid.iam.service.ejb.ApplicationService;
import com.soffid.iam.utils.Security;
import com.soffid.iam.web.component.CustomField3;

import es.caib.bpm.toolkit.exception.SystemWorkflowException;
import es.caib.seycon.ng.exception.InternalErrorException;
import es.caib.zkib.component.DataTable;
import es.caib.zkib.component.DateFormats;

public class DelegateWindow extends StandardUserWindow {
	private DataTable grantsTable;
	CustomField3 account;
	CustomField3 user;
	CustomField3 role;
	CustomField3 date;
	private RoleRequestInfo currentGrant;
	List<RoleRequestInfo> displayedGrants = null;

	protected void createGrants(Field field) throws Exception {
		Div d = new Div();
		d.setSclass("databox");
		grid.appendChild(d);
		Label label = new Label(field.getLabel());
		label.setSclass("label");
		d.appendChild(label);
		Div data = new Div();
		data.setSclass("container");
		// data.setWidth("100%");
		d.appendChild(data);
		grantsTable = new DataTable();
		grantsTable.setEnablefilter(true);
		grantsTable.setColumns(
			"- name: "+Labels.getLabel("com.soffid.iam.api.Role.informationSystemName")+"\n"+
			"  value: applicationDescription\n"+
			"- name: "+Labels.getLabel("agents.zul.Rol")+"\n"+
		    "  value: roleDescription\n" +
			"- name: "+Labels.getLabel("selfService.delegateTo")+"\n"+
		    "  value: delegateToUser\n"+
			"- name: "+Labels.getLabel("pamSession.accountName")+"\n"+
		    "  value: delegateTo\n"+
			"- name: "+Labels.getLabel("selfService.delegateUntil")+"\n"+
		    "  value: delegateUntil\n"+
			"  template: #{delegateUntil_date}\n"+
		    "  className: datetimeColumn\n"
			);
		data.appendChild(grantsTable);
		JSONArray array = new JSONArray();
		if (grants == null) {
			grants = new LinkedList<>();
			getTask().getVariables().put("grants", grants);
		}
		displayedGrants = grants;
		displayedGrants.clear();
		
		String me = Security.getCurrentUser();
		final List<RoleAccount> currentDelegations = EJBLocator.getEntitlementDelegationService().findActiveDelegations();
		Security.nestedLogin(Security.ALL_PERMISSIONS);
		try {
			final ApplicationService applicationService = EJBLocator.getApplicationService();
			for (RoleAccount ra: applicationService.findUserRolesByUserName(me)) {
				RoleRequestInfo grant = TaskUtils.generateRoleRequestInfo(ra);
				if (grant != null && grant.getDelegateTo() == null ) { 
					Application app = applicationService.findApplicationByApplicationName(grant.getApplicationName());
					Role role = applicationService.findRoleById(grant.getRoleId());
					if (isAcceptable(app) && isAcceptable(role)) {
						JSONObject o = generateRequestRow(grant);
						array.put(o);
						displayedGrants.add(grant);
					}
				}
			}
			for (RoleAccount ra: currentDelegations) {
				RoleRequestInfo grant = TaskUtils.generateRoleRequestInfo(ra);
				if (grant != null) { 
					Application app = applicationService.findApplicationByApplicationName(grant.getApplicationName());
					Role role = applicationService.findRoleById(grant.getRoleId());
					if (isAcceptable(app) && isAcceptable(role)) {
						JSONObject o = generateRequestRow(grant);
						array.put(o);
						displayedGrants.add(grant);
					}
				}
			}
		} finally {
			Security.nestedLogoff();
		}
		grantsTable.addEventListener("onSelect", (event) -> {
			openDelegationWindow();
		});
		grantsTable.setData(array);
		data.appendChild(grantsTable);
		grantsTable.afterCompose();
		inputFields.put (field.getName(), d);
		
	}

	private JSONObject generateRequestRow(RoleRequestInfo grant)
			throws InternalErrorException, NamingException, CreateException {
		JSONObject o = new JSONObject();
		o.put("applicationDescription", grant.getApplicationDescription());
		o.put("roleDescription", grant.getRoleDescription());
		o.put("delegateTo", grant.getDelegateTo());
		o.put("delegateUntil", grant.getDelegateUntil() == null ? null: grant.getDelegateUntil().getTime());
		if (grant.getDelegateUntil() != null)
			o.put("delegateUntil_date", DateFormats.getDateFormat().format(grant.getDelegateUntil()));
		if (grant.getDelegateToUser() != null) {
			Security.nestedLogin(Security.ALL_PERMISSIONS);
			try {
				User u = EJBLocator.getUserService().findUserByUserName(grant.getDelegateToUser());
				if (u != null) {
					o.put("delegateToUser", u.getFullName());
				}
			} finally {
				Security.nestedLogoff();
			}
		}
		else if (grant.getDelegateTo() != null && grant.getRoleId() != null) {
			Security.nestedLogin(Security.ALL_PERMISSIONS);
			try {
				Role roleValue = EJBLocator.getApplicationService().findRoleById(currentGrant.getRoleId());
				if (roleValue != null) {
					Account account = EJBLocator.getAccountService().findAccount(grant.getDelegateTo(), roleValue.getSystem());
					if (account != null)
						o.put("delegateToUser", account.getDescription());
					else if (grant.getDelegateToUser() != null) {
						User user = EJBLocator.getUserService().findUserByUserName(grant.getDelegateToUser());
						o.put("delegateToUser", user.getFullName());
					}
				}
			} finally {
				Security.nestedLogoff();
			}

		}
		return o;
	}

	private void openDelegationWindow() throws InternalErrorException, NamingException, CreateException, IOException {
		int i = grantsTable.getSelectedIndex();
		if (i >= 0) {
			currentGrant = displayedGrants.get(i);
			Window w = (Window) getFellow("selectUser");
			user = (CustomField3) w.getFellow("user");
			account = (CustomField3) w.getFellow("account");
			role = (CustomField3) w.getFellow("role");
			date = (CustomField3) w.getFellow("date");
			Security.nestedLogin(Security.ALL_PERMISSIONS);
			try {
				Role roleValue = EJBLocator.getApplicationService().findRoleById(currentGrant.getRoleId());
				if (roleValue != null)
					role.setValue(roleValue.getName()+"@"+roleValue.getSystem());
			} finally {
				Security.nestedLogoff();
			}
			if (currentGrant.getDelegateTo() == null) {
				user.setValue(null);
				account.setVisible(false);
				Calendar c = Calendar.getInstance();
				c.set(Calendar.HOUR_OF_DAY, 0);
				c.set(Calendar.MINUTE, 0);
				c.set(Calendar.SECOND, 0);
				c.set(Calendar.MILLISECOND, 0);
				c.add(Calendar.DAY_OF_MONTH, 7);
				date.setValue(c.getTime());
			} else {
				user.setValue(currentGrant.getDelegateToUser());
				account.setVisible(false);
				date.setValue(currentGrant.getDelegateUntil());
				refreshAccountList();
			}
			w.doHighlighted();
		}
	}

	private void refreshAccountList() throws InternalErrorException, NamingException, CreateException, IOException {
		Security.nestedLogin(Security.ALL_PERMISSIONS);
		try {
			String userName = (String) user.getValue();
			if (userName != null && ! userName.trim().isEmpty()) {
				Role roleValue = EJBLocator.getApplicationService().findRoleById(currentGrant.getRoleId());
				if (roleValue != null) {
					List<UserAccount> accounts = EJBLocator.getAccountService().findUsersAccounts(userName, roleValue.getSystem());
					if (accounts.size() > 1) {
						List<String> values = new LinkedList<>();
						boolean found = false;
						for (UserAccount account: accounts) {
							if (account.getName().equals(currentGrant.getDelegateTo()))
								found = true;
							values.add(URLEncoder.encode(account.getName(), "UTF-8"));
						}
						account.setListOfValues(values.toArray(new String[values.size()]));
						if (found) account.setValue(currentGrant.getDelegateTo());
						else account.setValue(accounts.get(0).getName());
						account.setVisible(true);
						account.updateMetadata();
						account.invalidate();
					} else if (accounts.size() == 1){
						account.setVisible(false);
						account.setValue(accounts.get(0).getName());
						currentGrant.setDelegateTo(accounts.get(0).getName());
					} else {
						account.setVisible(false);
						currentGrant.setDelegateTo(null);
					}
				}
			} else {
				account.setVisible(false);
			}
		} finally {
			Security.nestedLogoff();
		}
	}

	public void onChangeDelegateUser(Event ev) throws InternalErrorException, NamingException, CreateException, IOException {
		refreshAccountList();
	}
	
	
	@Override
	protected void load() {
		if (!getTask().getVariables().containsKey("userSelector")) {
			getTask().getVariables().put("userSelector", Security.getCurrentUser());
			try {
				fetchUserAttributes();
			} catch (Exception e) {
				throw new UiException("Error fetching user data", e);
			}
		}
		super.load();
	}

	public void closeDelegateWindow(Event ev) throws InternalErrorException, NamingException, CreateException {
		currentGrant.setDelegateTo(null);
		currentGrant.setDelegateToUser(null);
		currentGrant.setPreviousDelegateUntil(null);
		currentGrant.setDelegateUntil(null);
		getFellow("selectUser").setVisible(false);
		JSONObject o = generateRequestRow(currentGrant);
		int i = grantsTable.getSelectedIndex();
        response("update_"+i, new AuInvoke(grantsTable, "updateRow", Integer.toString(i), o.toString()));
        grantsTable.setSelectedIndex(-1);
	}

	public void addDelegate(Event ev) throws InternalErrorException, NamingException, CreateException {
		if ((!account.isVisible() || account.validate()) && user.validate() && date.validate()) {
			final Date dateValue = (Date) date.getValue();
			if (dateValue.before(new Date())) {
				date.setWarning(0, Labels.getLabel("bpm.delegate.dateRestriction"));
			} else {
				currentGrant.setDelegateUntil(dateValue);
				currentGrant.setDelegateToUser((String) user.getValue());
				currentGrant.setDelegateTo((String) account.getValue());
				getFellow("selectUser").setVisible(false);
				JSONObject o = generateRequestRow(currentGrant);
				int i = grantsTable.getSelectedIndex();
	            response("update_"+i, new AuInvoke(grantsTable, "updateRow", Integer.toString(i), o.toString()));
	            grantsTable.setSelectedIndex(-1);
			}
		}
	}
}
