package com.soffid.iam.addons.bpm.ui;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.au.out.AuInvoke;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Label;

import com.soffid.iam.addons.bpm.common.EJBLocator;
import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.PageInfo;
import com.soffid.iam.api.DataType;
import com.soffid.iam.api.MetadataScope;
import com.soffid.iam.api.User;
import com.soffid.iam.bpm.api.TaskInstance;
import com.soffid.iam.service.ejb.AdditionalDataService;
import com.soffid.iam.service.ejb.UserService;
import com.soffid.iam.utils.Security;
import com.soffid.iam.web.WebDataType;
import com.soffid.iam.web.datarender.DataTypeRenderer;
import com.soffid.iam.web.datarender.DefaultRenderer;

import es.caib.bpm.toolkit.exception.UserWorkflowException;
import es.caib.bpm.toolkit.exception.WorkflowException;
import es.caib.seycon.ng.exception.InternalErrorException;
import es.caib.zkib.component.DataTable;

public class MatchWindow extends StandardUserWindow {
	private JSONArray data;
	private DataTable usersTable;
	private String selectedUser;
	String originalUser;
	private Boolean merge;

	@Override
	protected void load() {
		super.load();
		List<String >matches = (List<String>) getVariables().get("$matches$");
		usersTable = (DataTable) getFellow("matches");
		Security.nestedLogin(Security.ALL_PERMISSIONS);
		try {
			PageInfo pi = EJBLocator.getBpmUserService().getPageInfo(TaskInstance.toTaskInstance(getTask()));
			if (pi.getFields() == null || pi.getFields().length == 0)
				createDefaultFields(usersTable);
			else
				createScreenFields(usersTable, pi);
			
			Label l = (Label) getFellow("conflict1");
			l.setValue(String.format(Labels.getLabel("bpm.conflict1"), matches.size()));
			
			UserService userSvc = com.soffid.iam.EJBLocator.getUserService();
			data = new JSONArray();
			for ( String userName: matches) {
				User user = userSvc.findUserByUserName(userName);
				JSONObject wrap = usersTable.wrap(user);
				if (!user.getActive().booleanValue())
					wrap.put("$class", "dashed");
				data.put(wrap);
			}
			data.put(createNewUserObject());
			usersTable.setData(data);
			originalUser = (String) getVariables().get("userName");
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			Security.nestedLogoff();
		}
		
	}

	private JSONObject createNewUserObject() throws JSONException, InternalErrorException, NamingException, CreateException {
		JSONObject o = new JSONObject();
		JSONObject attributes = new JSONObject();
		o.put("attributes", attributes);
		for (DataType dt: com.soffid.iam.EJBLocator.getAdditionalDataService().findDataTypes2(MetadataScope.USER)) {
			Object value = getVariables().get(dt.getName());
			if (value != null) {
				if (Boolean.TRUE.equals(dt.getBuiltin()))
					o.put(dt.getName(), value);
				else
					attributes.put(dt.getName(), value);
			}
		}
		o.put("$class", "bold");
		o.put("id",  JSONObject.NULL);
		return o;
	}

	private void createScreenFields(DataTable dt, PageInfo pi) throws InternalErrorException, NamingException, CreateException {
		AdditionalDataService svc = com.soffid.iam.EJBLocator.getAdditionalDataService();

		Arrays.sort(pi.getFields(), new Comparator<Field>() {
			public int compare(Field o1, Field o2) {
				return o1.getOrder().compareTo(o2.getOrder());
			}
			
		});
		JSONArray a = new JSONArray();
		JSONObject first = new JSONObject();
		first.put("name", Labels.getLabel("com.soffid.iam.api.User.action"));
		first.put("filter", false);
		first.put("sort", false);
		first.put("className", "statusColumn");
		first.put("value", "$action");
		a.put(first);

		for (Field field: pi.getFields()) {
			Collection<DataType> data = svc.findDataTypesByObjectTypeAndName2(User.class.getName(), field.getName());
			if (data != null && !data.isEmpty()) {
				DataType dataType = data.iterator().next();
				DataTypeRenderer renderer = DefaultRenderer.getRenderer(dataType);
				a.put ( renderer.renderColumn(dataType));
			}
		}
		
		if (a.length() == 1)
			createDefaultFields(dt);
		else {
			JSONObject last = new JSONObject();
			last.put("name", "");
			last.put("filter", false);
			last.put("sort", false);
			last.put("className", "statusColumn");
			last.put("template", getImageTemplate());
			a.put(last);
			dt.setColumns(a.toString());
		}
	}
	
	public void selectUser(Event event) {
		for (int i = 0; i < data.length(); i++) {
			JSONObject user = data.getJSONObject(i);
			if (i == usersTable.getSelectedIndex()) {
				String userName = user.optString("userName", null);
				if (user.has("id") && ! user.optString("id").trim().isEmpty() ) {
					user.put("$action", Labels.getLabel("bpm.action.Merge"));
					selectedUser = userName;
					merge = true;
				} else {
					merge = false;
					getVariables().put("userName", userName);
					user.put("$action", Labels.getLabel("bpm.action.New"));
					selectedUser = (String) getVariables().get("userName");
				}
	            usersTable.response("update_"+i, new AuInvoke(usersTable, "updateRow", Integer.toString(i), user.toString()));
				usersTable.response("setSelected", new AuInvoke(this, "setSelected", Integer.toString(i)));
			}
			else if (user.has("$action"))
			{
				user.remove("$action");
	            usersTable.response("update_"+i, new AuInvoke(usersTable, "updateRow", Integer.toString(i), user.toString()));
			}
		}
	}

	public String getImageTemplate() {
		return "<img style='display: #{id == null ? 'none': 'inline-block'}' src=\"" + getDesktop().getExecution().getContextPath()+"/img/link.svg\" "
				+ "class=\"imageclic\" "
				+ "onClick=\"zkDatatable.sendClientAction(this,'onOpenUser')\"/>"
				+ "<span style='display: #{id != null ? 'none': 'inline-block'}'>"+Labels.getLabel("bpm.createNewUser")+"</span>";
	}

	private void createDefaultFields(DataTable dt) {
		dt.setColumns(
				"- name: ${c:l('com.soffid.iam.api.User.action')}\n" + 
				"  value: $action\n" + 
				"- name: ${com.soffid.iam.api.User.userName }\n" + 
				"  value: userName\n" + 
				"- name: ${com.soffid.iam.api.User.fullName }\n" + 
				"  value: fullName\n" + 
				"- name: ${com.soffid.iam.api.User.userType }\n" + 
				"  value: userType\n" + 
				"- name: ${com.soffid.iam.api.User.primaryGroup }\n" + 
				"  value: primaryGroup\n" +
				"- name: \"\"\n" + 
				"  filter: false\n" + 
				"  sort: false\n" + 
				"  className: statusColumn\n" + 
				"  template: "+getImageTemplate()+"\n");
	}

	public void openUser(Event event) {
		int selected = usersTable.getSelectedIndex();
		if (selected >= 0 && selected < data.length()) {
			JSONObject user = data.getJSONObject(selected);
			String userName = user.getString("userName");
			if (userName != null)
				Executions.getCurrent().sendRedirect("/resource/user/user.zul?userName="+userName, "_blank");
		}
	}

	@Override
	protected void prepareTransition(String trasition) throws WorkflowException {
		super.prepareTransition(trasition);
		if ( merge == null) 
			throw new UserWorkflowException("Please, select an option to merge or register a new user");
		String current = (String) getVariables().get("userName");
		if (merge.booleanValue()) {
			getVariables().put("userName", selectedUser);
			getVariables().put("userSelector", selectedUser);
			getVariables().put("action", "M");
		}
	}
		
}
