package com.soffid.iam.addons.bpm.ui;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.metainfo.EventHandler;
import org.zkoss.zk.ui.metainfo.ZScript;
import org.zkoss.zul.Div;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import com.soffid.iam.EJBLocator;
import com.soffid.iam.addons.bpm.common.Constants;
import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.RoleRequestInfo;
import com.soffid.iam.api.Application;
import com.soffid.iam.api.Role;
import com.soffid.iam.api.RoleGrant;
import com.soffid.iam.api.User;
//import com.soffid.bpm.consum.data.RequestInfo;
import com.soffid.iam.utils.Security;

import es.caib.bpm.toolkit.exception.SystemWorkflowException;
import es.caib.bpm.toolkit.exception.UserWorkflowException;
import es.caib.bpm.toolkit.exception.WorkflowException;
import es.caib.seycon.ng.exception.InternalErrorException;

public class RequestWindow extends StandardUserWindow
{
	ApplicationTree applicationTree;
	LinkedList<ApplicationTree> treeNavigation = new LinkedList<ApplicationTree>();
	ApplicationTree currentApplication;
	
	@SuppressWarnings("unchecked")
	@Override
	protected void prepareTransition(String trasition) throws WorkflowException {
		if ( ! trasition.toLowerCase().contains("cancel"))
		{
			if ( perms == null || perms.isEmpty())
				throw new UserWorkflowException("Please, add some permission");
			
			super.prepareTransition(trasition);
			String currentUser = Security.getCurrentUser();
			Security.nestedLogin(Security.ALL_PERMISSIONS);
			User user;
			try {
				String userName = getVariables().get("userName") == null ?
						currentUser:
						(String) getVariables().get("userName");
				user = EJBLocator.getUserService().findUserByUserName(userName);
				for ( RoleRequestInfo perm: perms) {
					perm.setUserName(user.getUserName());
					perm.setUserFullName(user.getFullName());
				}
			} catch (Exception e) {
				throw new SystemWorkflowException(e);
			} finally {
				Security.nestedLogoff();
			}
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

	public void onCreate() 
	{
		super.onCreate();
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
			getFellow("selected").setVisible(perms.size() > 0);
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
			getFellow("selected").setVisible(perms.size() > 0);
			fillRoles();
		}
		finally {
			Security.nestedLogoff();
		}
	}
	
	EventListener onSelectApp = new EventListener() {
		public void onEvent(Event event) throws Exception {
			selectApp(event);
		}

	};

	private void selectApp(Event event) throws InternalErrorException, NamingException, CreateException {
		Div div = (Div) event.getTarget();
		ApplicationTree tree = (ApplicationTree) event.getTarget().getAttribute("app");
		Integer levels = (Integer) div.getAttribute("levels");
		currentApplication = tree;
		addParents(tree, levels);
		Div p = (Div) getFellow("apps");
		
		Security.nestedLogin(Security.ALL_PERMISSIONS);
		try {
			fillApplicationButtons();
			fillNavigationBar();
			fillRoles();
		} finally {
			Security.nestedLogoff();
		}
		((Div)getFellow("roleSelectorDiv")).setVisible(true);
		((Textbox)getFellow("roleSelector")).setText("");
		((Textbox)getFellow("roleSelector")).focus();
	}
	
	private void addParents(ApplicationTree tree, int levels) {
		if (levels > 0)
			addParents(tree.parent, levels - 1);
		treeNavigation.add(tree);
	}
	
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
	private Collection<RoleGrant> currentGrants;

	
	
	@Override
	protected void load()
	{
		readOnly = readOnly || getTask() == null;
		if (getTask() != null && ! getTask().getVariables().containsKey(Constants.REQUESTER_VAR)) {
			getTask().getVariables().put(Constants.REQUESTER_VAR, Security.getCurrentUser());
			getTask().getVariables().put(Constants.REQUESTER_NAME_VAR, Security.getSoffidPrincipal().getFullName());
		}
		
		try {
			if (getTask() != null && ! getTask().getVariables().containsKey("userSelector")) {
				getTask().getVariables().put("userSelector", Security.getCurrentUser());
				fetchUserAttributes();
			} else {
				loadCurrentGrants();
			}
		} catch (Exception e) {
			throw new UiException(e);
		}

		super.load();
		Security.nestedLogin(Security.ALL_PERMISSIONS);
		try {
			try {
				loadApplications();
				treeNavigation.clear();
				currentApplication = applicationTree;
				fillNavigationBar();
				fillApplicationButtons();
			} catch (Exception e) {
				throw new UiException(e);
			}
		} finally {
			Security.nestedLogoff();
		}
		((Textbox)getFellow("appSelector")).focus();
	}

	private void fillApplicationButtons() {
		Textbox appSelector = (Textbox) getFellow("appSelector");
		appSelector.setValue("");
		getFellow("appSelectorDiv").setVisible(!currentApplication.children.isEmpty());
		Div p = (Div) getFellow("apps");
		p.getChildren().clear();
		for ( ApplicationTree tree: currentApplication.children) { 
			fillApplicationButton(p, tree, 0);
		}
	}

	public void fillApplicationButton(Div p, ApplicationTree tree, int levels) {
		Div d = new Div();
		d.setAttribute("app", tree);
		d.setAttribute("levels", levels);
		d.setSclass("app");
		Image i = new Image("~./img/remove.png");
		i.setSclass("cancel");
		d.appendChild(i);
		Label l = new Label(tree.app.getRelativeName());
		l.setSclass("appName");
		d.appendChild(l);
		l = new Label (tree.app.getDescription());
		l.setSclass("appDescription");
		d.appendChild(l);
		p.appendChild(d);
		d.addEventListener("onClick", onSelectApp);

		ApplicationTree parent = tree.parent;
		for ( int j = 0; j < levels; j++) {
			Label l2 = new Label(parent.app.getRelativeName());
			d.insertBefore(l2, i.getNextSibling());
			parent = parent.parent;
			if (j + 1 < levels) d.insertBefore(new Label(" / "), l2);
			
		}
	}

	private void fillNavigationBar() {
		Div navbar = (Div) getFellow("navbar");
		navbar.getChildren().clear();
		Label label = new Label();
		label.setValue(Labels.getLabel("seu.aplciacions"));
		label.addEventHandler("onClick",  new EventHandler(ZScript.parseContent("ref:window.menu"), null));
		label.setAttribute("pos", 0);
		label.setAttribute("target", applicationTree);
		label.setSclass("link");
		navbar.appendChild(label);
		
		int pos = 1;
		for (ApplicationTree t: treeNavigation) {
			navbar.appendChild(new Label(" > "));
			label = new Label();
			label.setValue(t.app.getRelativeName());
			label.setAttribute("target", treeNavigation);
			label.setAttribute("pos", pos);
			if (pos < treeNavigation.size()) {
				label.addEventHandler("onClick",  new EventHandler(ZScript.parseContent("ref:window.menu"), null));
				label.setSclass("link");
				pos ++;
			}
			navbar.appendChild(label);
		}
	}
	
	public void menu(Event event) throws InternalErrorException, NamingException, CreateException {
		Integer pos = (Integer) event.getTarget().getAttribute("pos");
		while (treeNavigation.size() > pos.intValue() ) 
			treeNavigation.remove (pos.intValue());
		if (treeNavigation.size() == 0)
			currentApplication = applicationTree;
		else
			currentApplication = treeNavigation.getLast();
		Security.nestedLogin(Security.ALL_PERMISSIONS);
		try {
			fillApplicationButtons();
			fillNavigationBar();
			fillRoles();
		} finally {
			Security.nestedLogoff();
		}
	}

	private void fillRoles() throws InternalErrorException, NamingException, CreateException {
		Div r = (Div) getFellow("roles");
		r.getChildren().clear();
		if (currentApplication.app != null) {
			Collection<Role> roles = com.soffid.iam.EJBLocator.getApplicationService()
				.findRolesByApplicationName(currentApplication.app.getName());
			boolean any = false;
			for ( Role role: roles)
			{
				if (role.getBpmEnforced() != null && role.getBpmEnforced().booleanValue() && !inCart(role))
				{
					Div d = new Div();
					d.setAttribute("role", role);
					if (isAlreadyGranted(role)) 
						d.setSclass("role disabled");
					else {
						d.setSclass("role");
						d.addEventListener("onClick", onSelectRole);
					}
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
					any = true;
				}
				
			}
			getFellow("roleSelectorDiv").setVisible(any);
		} else {
			getFellow("appSelectorDiv").setVisible(false);

		}
	}

	private boolean inCart(Role role) {
		for (RoleRequestInfo grant: perms) {
			if (grant.getRoleId().equals(role.getId()))
				return true;
		}
		return false;
	}

	private boolean isAlreadyGranted(Role role) {
		if (currentGrants == null)
			return false;
		for (RoleGrant grant: currentGrants) {
			if (grant.getRoleId().equals(role.getId())) return true;
		}
		return false;
	}

	private void loadApplications() throws InternalErrorException, NamingException, CreateException {
		applicationTree = new ApplicationTree();
		applicationTree.children = new LinkedList<ApplicationTree>();
		for (Application app: EJBLocator.getApplicationService().findApplicationByJsonQuery("bpmEnabled eq true")) {
			ApplicationTree parentTree = findApplicationTree(app.getParent());
			addApplicationToTree(app, parentTree);
		}
	}

	private ApplicationTree findApplicationTree(String parent) throws InternalErrorException, NamingException, CreateException {
		if (parent == null) return applicationTree;
		else {
			Application app = EJBLocator.getApplicationService().findApplicationByApplicationName(parent);
			if (app == null) return applicationTree;
			else {
				ApplicationTree parentTree = findApplicationTree(app.getParent());
				for (ApplicationTree appTree: parentTree.children)
					if (appTree.app.getName().equals(parent))
						return appTree;
				return addApplicationToTree(app, parentTree);
				
			}
		}
	}

	public ApplicationTree addApplicationToTree(Application app, ApplicationTree parentTree) {
		ApplicationTree thisTree = new ApplicationTree();
		thisTree.children = new LinkedList<ApplicationTree>();
		thisTree.app = app;
		parentTree.children.add(thisTree);
		thisTree.parent = parentTree;
		return thisTree;
	}

	public void onChangingAppSelector(InputEvent event)
	{
		String data = event.getValue();
		filterApps(data);
	}
	
	private void filterApps(String data) {
		Security.nestedLogin(Security.ALL_PERMISSIONS);
		String[] split = data.trim().toLowerCase().split(" +");
		try {
			Div p = (Div) getFellow("apps");
			if (data == null || data.trim().isEmpty()) {
				fillApplicationButtons();
			} else {
				p.getChildren().clear();
				recursiveFilter (p, currentApplication, split, 0);
			}
			for (  Div d: (Collection<Div>) p.getChildren())
			{
				ApplicationTree app = (ApplicationTree) d.getAttribute("app");
			}
			
		} finally {
			Security.nestedLogoff();
		}
	}

	private void recursiveFilter(Div p, ApplicationTree parent, String[] split, int levels) {
		for (ApplicationTree tree: parent.children) {
			String t = tree.app.getName().toLowerCase()+" "+tree.app.getDescription().toLowerCase();
			boolean match = true;
			for (String s: split)
				if (! t.contains(s)) match = false;
			if (match)
				fillApplicationButton(p, tree, levels);
			else {
				recursiveFilter(p, tree, split, levels + 1);
			}
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

	// Not required
	protected void createGrants(Field field) throws Exception {
		return;
	}

	@Override
	public void fetchUserAttributes() throws Exception {
		super.fetchUserAttributes();
		loadCurrentGrants();
	}

	private void loadCurrentGrants() throws InternalErrorException, NamingException, CreateException {
		Security.nestedLogin(Security.ALL_PERMISSIONS);
		try {
			currentGrants = new LinkedList<RoleGrant>();
			String userName = (String) getTask().getVariables().get("userSelector");
			if (userName != null && ! userName.trim().isEmpty()) {
				User user = EJBLocator.getUserService().findUserByUserName(userName);
				if (user != null) {
					currentGrants = EJBLocator.getApplicationService().findEffectiveRoleGrantByUser(user.getId());
				}
			}
		} finally {
			Security.nestedLogoff();
		}
	}
}

class ApplicationTree {
	Application app;
	ApplicationTree parent;
	List<ApplicationTree> children;
}
