package com.soffid.iam.addons.bpm.ui;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Div;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import com.soffid.iam.addons.bpm.common.Constants;
import com.soffid.iam.addons.bpm.common.RoleRequestInfo;
import com.soffid.iam.api.Application;
import com.soffid.iam.api.Role;
import com.soffid.iam.api.User;
//import com.soffid.bpm.consum.data.RequestInfo;
import com.soffid.iam.utils.Security;

import es.caib.bpm.toolkit.WorkflowWindow;
import es.caib.bpm.toolkit.exception.UserWorkflowException;
import es.caib.bpm.toolkit.exception.WorkflowException;
import es.caib.seycon.ng.exception.InternalErrorException;

public class RequestWindow extends WorkflowWindow
{
		
	@SuppressWarnings("unchecked")
	@Override
	protected void prepareTransition(String trasition) throws WorkflowException {
		if ( ! trasition.toLowerCase().contains("cancel"))
		{
			if ( perms == null || perms.isEmpty())
				throw new UserWorkflowException("Please, add some permission");
		}
	}

	Log log = LogFactory.getLog(getClass());
	private Row addRoleRow;
	private boolean readOnly = false;
	private List<RoleRequestInfo> perms = new LinkedList<RoleRequestInfo>();

	public boolean isReadonly() {
		return readOnly;
	}

	public void setReadonly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public RequestWindow() 
	{
	}

	public void onCreate() throws InternalErrorException
	{
	}
	
	public void addRole ( Role role ) throws InternalErrorException, NamingException, CreateException
	{
		RoleRequestInfo i = new RoleRequestInfo();
		User me = com.soffid.iam.EJBLocator.getUserService().getCurrentUser();
		Security.nestedLogin(Security.ALL_PERMISSIONS);
		try 
		{	
			Application app = com.soffid.iam.EJBLocator.getApplicationService().findApplicationByApplicationName(role.getInformationSystemName());
			i.setApplicationName(role.getInformationSystemName());
			i.setApplicationDescription(app.getDescription());
			i.setApproved(false);
			i.setComments("");
			i.setDenied(false);
			i.setOwners(null);
			i.setOwnersString(null);
			i.setParentRole(null);
			i.setPreviousRoleDescription(null);
			i.setPreviousRoleId(null);
			i.setRoleId(role.getId());
			i.setRoleDescription(role.getDescription());
			i.setUserFullName(Security.getSoffidPrincipal().getFullName());
			i.setUserName(me.getUserName());
			
			perms.add(i);
			getTask().getVariables().put("grants", perms);
		}
		finally {
			Security.nestedLogoff();
		}
	}
	
	public void removeRole ( Role role ) throws InternalErrorException, NamingException, CreateException
	{
		RoleRequestInfo i = new RoleRequestInfo();
		User me = com.soffid.iam.EJBLocator.getUserService().getCurrentUser();
		Security.nestedLogin(Security.ALL_PERMISSIONS);
		try 
		{	
			for ( Iterator<RoleRequestInfo> it = perms.iterator(); it.hasNext(); )
			{
				RoleRequestInfo perm = it.next();
				if (perm.getRoleId().equals(role.getId()))
					it.remove();
			}
			getTask().getVariables().put("grants", perms);
		}
		finally {
			Security.nestedLogoff();
		}
	}
	
	private Map<String, Object> getVariables()
	{
		if (getTask() == null)
			return getProcessInstance().getVariables();
		else
			return getTask().getVariables();
	}
	
	EventListener onSelectApp = new EventListener() {
		private Application currentApplication;

		public void onEvent(Event event) throws Exception {
			Div div = (Div) event.getTarget();
			if (div.getSclass().equals("appSelected"))
			{
				Div p = (Div) getFellow("apps");
				for ( Object o: p.getChildren())
				{
					Div d = (Div) o;
					d.setVisible( true);
					d.setSclass("app");
				}
				((Textbox) getFellow("appSelector")).setText("");
				((Textbox) getFellow("appSelector")).focus();
				p = (Div) getFellow("roles");
				p.getChildren().clear();
				((Div)getFellow("roleSelectorDiv")).setVisible(false);
			} else {
				Application app = (Application) event.getTarget().getAttribute("app");
				currentApplication = app;
				Div p = (Div) getFellow("apps");
				for ( Object o: p.getChildren())
				{
					Div d = (Div) o;
					d.setVisible( d == event.getTarget());
					d.setSclass("appSelected");
				}
				
				Security.nestedLogin(Security.ALL_PERMISSIONS);
				try {
					Div r = (Div) getFellow("roles");
					for ( Role role: 
						com.soffid.iam.EJBLocator.getApplicationService()
							.findRolesByApplicationName(app.getName()))
					{
						if (role.getBpmEnforced() != null && role.getBpmEnforced().booleanValue())
						{
							Div d = new Div();
							d.setAttribute("role", role);
							d.setSclass("role");
							Image i = new Image("~./img/remove.png");
							i.setSclass("cancel");
							d.appendChild(i);
							Label l = new Label(role.getName());
							l.setSclass("roleName");
							d.appendChild(l);
							l = new Label (role.getDescription());
							l.setSclass("roleDescription");
							d.appendChild(l);
							r.appendChild(d);	
							d.addEventListener("onClick", onSelectRole);
						}
						
					}
				} finally {
					Security.nestedLogoff();
				}
				((Div)getFellow("roleSelectorDiv")).setVisible(true);
				((Textbox)getFellow("roleSelector")).setText("");
				((Textbox)getFellow("roleSelector")).focus();
			}
		}
	};
	
	EventListener onSelectRole = new EventListener() {
		public void onEvent(Event event) throws Exception {
			Div div = (Div) event.getTarget();
			Role role = (Role) event.getTarget().getAttribute("role");
			if (div.getSclass().equals("selected"))
			{
				removeRole(role);
				div.detach();
			} 
			else
			{
				addRole(role);
				Div p = (Div) getFellow("roles");
				for ( Object o: p.getChildren())
				{
					Div d = (Div) o;
					if (d == event.getTarget())
					{
						d.setParent(getFellow("selected"));
						d.setSclass("selected");
						return;
					}
				}
				
				
			}
		}
	};

	public void onLoad () throws InternalErrorException, NamingException, CreateException
	{
		readOnly = readOnly || getTask() == null;
		if (getTask() != null && ! getTask().getVariables().containsKey(Constants.REQUESTER_VAR)) {
			getTask().getVariables().put(Constants.REQUESTER_VAR, Security.getCurrentUser());
			getTask().getVariables().put(Constants.REQUESTER_NAME_VAR, Security.getSoffidPrincipal().getFullName());
		}

		Security.nestedLogin(Security.ALL_PERMISSIONS);
		try {
			Div p = (Div) getFellow("apps");
			for ( Application app: 
				com.soffid.iam.EJBLocator.getApplicationService()
					.findApplicationByJsonQuery("bpmEnforced eq \"true\"))"))
			{
				Div d = new Div();
				d.setAttribute("app", app);
				d.setSclass("app");
				Image i = new Image("~./img/remove.png");
				i.setSclass("cancel");
				d.appendChild(i);
				Label l = new Label(app.getName());
				l.setSclass("appName");
				d.appendChild(l);
				l = new Label (app.getDescription());
				l.setSclass("appDescription");
				d.appendChild(l);
				p.appendChild(d);
				d.addEventListener("onClick", onSelectApp);
				
			}
		} finally {
			Security.nestedLogoff();
		}
		((Textbox)getFellow("appSelector")).focus();
	}


	public void onChangingAppSelector(InputEvent event)
	{
		String data = event.getValue();
		filterApps(data);
	}
	
	private void filterApps(String data) {
		Div p = (Div) getFellow("apps");
		String[] split = data.trim().toLowerCase().split(" +");
		for (  Div d: (Collection<Div>) p.getChildren())
		{
			Application app = (Application) d.getAttribute("app");
			String t = app.getName().toLowerCase()+" "+app.getDescription().toLowerCase();
			boolean match = true;
			for (String s: split)
				if (! t.contains(s)) match = false;
			d.setVisible(match);
		}
	}

	public void onChangingRoleSelector(InputEvent event)
	{
		String data = event.getValue();
		filterRoles(data);
	}
	
	private void filterRoles(String data) {
		Div p = (Div) getFellow("roles");
		String[] split = data.trim().toLowerCase().split(" +");
		for (  Div d: (Collection<Div>) p.getChildren())
		{
			Role role = (Role) d.getAttribute("role");
			String t = role.getName().toLowerCase()+" "+role.getDescription().toLowerCase();
			boolean match = true;
			for (String s: split)
				if (! t.contains(s)) match = false;
			d.setVisible(match);
		}
	}

}
