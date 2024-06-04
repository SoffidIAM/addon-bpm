package com.soffid.iam.addons.bpm.web;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.CreateException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import com.soffid.iam.EJBLocator;
import com.soffid.iam.addons.bpm.common.Attribute;
import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.Node;
import com.soffid.iam.addons.bpm.common.NodeType;
import com.soffid.iam.addons.bpm.common.Process;
import com.soffid.iam.addons.bpm.common.Transition;
import com.soffid.iam.addons.bpm.common.WorkflowType;
import com.soffid.iam.addons.bpm.core.ejb.BpmEditorService;
import com.soffid.iam.addons.bpm.core.ejb.BpmEditorServiceHome;
import com.soffid.iam.api.Application;
import com.soffid.iam.api.DataType;
import com.soffid.iam.api.DomainType;
import com.soffid.iam.api.MetadataScope;
import com.soffid.iam.api.Role;
import com.soffid.iam.utils.Security;
import com.soffid.iam.web.WebDataType;

import es.caib.seycon.ng.comu.TypeEnumeration;
import es.caib.seycon.ng.exception.InternalErrorException;
import es.caib.zkib.component.DataListbox;
import es.caib.zkib.component.DataModel;
import es.caib.zkib.datamodel.DataModelCollection;
import es.caib.zkib.datasource.XPathUtils;
import es.caib.zkib.zkiblaf.Missatgebox;

public class NewProcessWindow extends Window {
	public void createProcess () throws Exception
	{
		Textbox name = (Textbox) getFellow("name");
		if (name.getValue() == null || name.getValue().trim().isEmpty())
		{
			Missatgebox.avis(Labels.getLabel("bpm.newProcess.missing.name"));
			return;
		}
		Listbox type = (Listbox) getFellow("type");
		if (type.getSelectedItem() == null ||
				type.getSelectedItem().getValue() == null ||
				type.getSelectedItem().getValue().equals(""))
		{
			Missatgebox.avis(Labels.getLabel("bpm.newProcess.missing.wf"));
			return;
		}
		
		Process p = new Process();
		p.setName(name.getValue());
		p.setDescription(name.getValue());
		p.setInitiators("*");
		p.setManagers("SOFFID_ADMIN");
		p.setObservers("admin");
		WorkflowType t = (WorkflowType) type.getSelectedItem().getValue();
		p.setType( t );
		
		if (t == WorkflowType.WT_USER)
			createUserTemplate(p);
		else if (t == WorkflowType.WT_ACCOUNT_RESERVATION)
			createAccountTemplate(p);
		else if (t == WorkflowType.WT_DELEGATION)
			createDelegationTemplate(p);
		else
			createPermissionsTemplate(p);
		
		DataModel model = (DataModel) getParent().getFellow("model");
		String path = XPathUtils.createPath(model, "/process", p);
		setVisible(false);
		
		DataModelCollection c = (DataModelCollection) XPathUtils.eval(model, "/process");
		int size = c.getSize();
		
		ProcessListbox lb = (ProcessListbox) getParent().getFellow("listbox");
		lb.setSelectedIndex( size - 1);
		
		EditorHandler frame = (EditorHandler) getParent().getFellow("frame");
//		Form f = (Form) getParent().getFellow("editor").getFellow("form");
//		f.setDataPath("/model:"+path);
		frame.showDetails();
	}

	private void createUserTemplate(Process p) throws InternalErrorException, NamingException, CreateException {
		Node nodeStart = new Node();
		Node nodeApprove = new Node();
		Node nodeApply = new Node();
		Node nodeEnd = new Node();
		
		Attribute att = new Attribute();
		att.setLabel(Labels.getLabel("com.soffid.iam.api.User.action"));
		att.setName("action");
		att.setType(TypeEnumeration.STRING_TYPE);
		att.setValues(Arrays.asList("A:New user","M:Modify user","D:Disable user","E:Enable user"));
		att.setOrder(1L);
		p.getAttributes().add(att);

		att = new Attribute();
		att.setLabel(Labels.getLabel("com.soffid.iam.api.User.userSelector"));
		att.setName("userSelector");
		att.setType(TypeEnumeration.USER_TYPE);
		att.setOrder(2L);
		p.getAttributes().add(att);

		att = new Attribute();
		att.setLabel(Labels.getLabel("com.soffid.iam.api.User.grants"));
		att.setName("grants");
		att.setOrder(3L);
		p.getAttributes().add(att);
		
		nodeStart.setName("Start");
		nodeStart.setDescription("Request new user management process");
		nodeStart.setType(NodeType.NT_START);
		nodeStart.setUploadDocuments(true);
		addFields (nodeStart, false);
		p.getNodes().add(nodeStart);
		
		nodeApprove.setName("Approve");
		nodeApprove.setTaskName("Approve changes on #{fullName}");
		nodeApprove.setDescription("Approve ");
		nodeApprove.setType(NodeType.NT_SCREEN);
		nodeApprove.setMailActor(getGroupOwner());
		nodeApprove.setUploadDocuments(true);
		nodeApprove.setMailShortcut(true);
		addFields (nodeApprove, true);
		p.getNodes().add(nodeApprove);
		
		nodeApply.setName("Apply changes");
		nodeApply.setType(NodeType.NT_APPLY);
		nodeApply.setDescription("Apply changes");
		nodeApply.setApplyUserChanges(true);
		p.getNodes().add(nodeApply);
		
		nodeEnd.setName("End");
		nodeEnd.setDescription("End");
		nodeEnd.setType(NodeType.NT_END);
		p.getNodes().add(nodeEnd);
		
		addTransition (nodeStart, nodeApprove, "Request");
		addTransition (nodeApprove, nodeApply, "Approve");
		addTransition (nodeApprove, nodeEnd, "Reject");
		addTransition (nodeApply, nodeEnd, "");
	}

	private void createDelegationTemplate(Process p) throws InternalErrorException, NamingException, CreateException {
		Node nodeStart = new Node();
		Node nodeApply = new Node();
		Node nodeEnd = new Node();
		
		Attribute att = new Attribute();
		att.setLabel(Labels.getLabel("com.soffid.iam.api.User.userSelector"));
		att.setName("userSelector");
		att.setOrder(1L);
		p.getAttributes().add(att);

		att = new Attribute();
		att.setLabel(Labels.getLabel("com.soffid.iam.api.User.grants"));
		att.setName("grants");
		att.setOrder(0L);
		p.getAttributes().add(att);
		
		nodeStart.setName("Start");
		nodeStart.setDescription("Add or revoke delegations");
		nodeStart.setType(NodeType.NT_START);
		// Add grants item
		Field f = new Field();
		f.setLabel("Permissions");
		f.setName("grants");
		f.setOrder(1L);
		nodeStart.getFields().add(f);
		p.getNodes().add(nodeStart);
		
		nodeApply.setName("Apply changes");
		nodeApply.setType(NodeType.NT_APPLY);
		nodeApply.setDescription("Apply changes");
		nodeApply.setApplyEntitlements(true);
		p.getNodes().add(nodeApply);
		
		nodeEnd.setName("End");
		nodeEnd.setDescription("End");
		nodeEnd.setType(NodeType.NT_END);
		p.getNodes().add(nodeEnd);
		
		addTransition (nodeStart, nodeApply, "Apply");
		addTransition (nodeApply, nodeEnd, "");
	}

	private void createPermissionsTemplate(Process p) throws InternalErrorException, NamingException, CreateException {
		Node nodeStart = new Node();
		Node nodeApprove = new Node();
		Node nodeApply = new Node();
		Node nodeEnd = new Node();
		
		nodeStart.setName("Start");
		nodeStart.setDescription( p.getType() == WorkflowType.WT_PERMISSION?
				"Request permisson changes": "Request permissions");
		nodeStart.setType(NodeType.NT_START);
		addPermFields (nodeStart, false);
		p.getNodes().add(nodeStart);
		
		nodeApprove.setName("Approve");
		nodeApprove.setDescription("Approve ");
		nodeApprove.setType(NodeType.NT_GRANT_SCREEN);
		nodeApprove.setMailActor(getApplicationOwner());
		nodeApprove.setTaskName("Approve permissions for #{fullName}");
		nodeApprove.setGrantScreenType("displayPending");
		nodeApprove.setMailShortcut(true);
		nodeApprove.setApproveTransition("Approve");
		nodeApprove.setDenyTransition("Reject");
		addPermFields (nodeApprove, false);
		p.getNodes().add(nodeApprove);
		
		nodeApply.setName("Apply changes");
		nodeApply.setType(NodeType.NT_APPLY);
		nodeApply.setDescription("Apply changes");
		nodeApply.setApplyEntitlements(true);
		p.getNodes().add(nodeApply);
		
		nodeEnd.setName("End");
		nodeEnd.setDescription("End");
		nodeEnd.setType(NodeType.NT_END);
		p.getNodes().add(nodeEnd);
		
		addTransition (nodeStart, nodeApprove, "Request");
		addTransition (nodeStart, nodeEnd, "Cancel");
		addTransition (nodeApprove, nodeApply, "Approve");
		addTransition (nodeApprove, nodeEnd, "Reject");
		addTransition (nodeApply, nodeEnd, "");
	}

	private String getApplicationOwner() throws InternalErrorException, NamingException, CreateException {
		for (Role role: EJBLocator.getApplicationService().findRolesByApplicationName("SOFFID")) {
			if (DomainType.APPLICATIONS.equals(role.getDomain()))
				return role.getName()+"/#{applicationName}";
		}
		
		Application app = EJBLocator.getApplicationService().findApplicationByApplicationName("SOFFID");
		if (app != null) {
			Role role = new Role();
			role.setInformationSystemName(app.getName());
			role.setDescription("Application supervisor");
			role.setBpmEnabled(false);
			role.setName("SOFFID_APP_OWNER");
			role.setSystem(EJBLocator.getDispatcherService().findSoffidDispatcher().getName());
			role.setDomain(DomainType.APPLICATIONS);
			role = EJBLocator.getApplicationService().create(role);
			return role.getName()+"/#{applicationName}";
		} else {
			return Security.getCurrentUser();
		}
	}

	private String getGroupOwner() throws InternalErrorException, NamingException, CreateException {
		for (Role role: EJBLocator.getApplicationService().findRolesByApplicationName("SOFFID")) {
			if (DomainType.GROUPS.equals(role.getDomain()))
				return role.getName()+"/#{primaryGroup}";
		}
		
		Application app = EJBLocator.getApplicationService().findApplicationByApplicationName("SOFFID");
		if (app != null) {
			Role role = new Role();
			role.setInformationSystemName(app.getName());
			role.setDescription("Business unit supervisor");
			role.setBpmEnabled(false);
			role.setName("SOFFID_GROUP_MGR");
			role.setSystem(EJBLocator.getDispatcherService().findSoffidDispatcher().getName());
			role.setDomain(DomainType.GROUPS);
			role = EJBLocator.getApplicationService().create(role);
			return role.getName()+"/#{primaryGroup}";
		} else {
			return Security.getCurrentUser();
		}
	}

	private void createAccountTemplate(Process p) throws InternalErrorException, NamingException, CreateException {
		p.setInitiators("-nobody-");
		
		long order = 1;
		
		List<Attribute> attributes = new LinkedList<Attribute>();
		Attribute a = new Attribute();
		a.setName("account");
		a.setLabel("Account name");
		a.setMultiValued(false);
		a.setOrder(order++);
		a.setType(TypeEnumeration.STRING_TYPE);
		attributes.add(a);
		
		a = new Attribute();
		a.setName("systemName");
		a.setLabel("System");
		a.setMultiValued(false);
		a.setOrder(order++);
		a.setType(TypeEnumeration.STRING_TYPE);
		attributes.add(a);
		
		a = new Attribute();
		a.setName("loginName");
		a.setLabel("Login name");
		a.setMultiValued(false);
		a.setOrder(order++);
		a.setType(TypeEnumeration.STRING_TYPE);
		attributes.add(a);
		
		a = new Attribute();
		a.setName("server");
		a.setLabel("Server");
		a.setMultiValued(false);
		a.setOrder(order++);
		a.setType(TypeEnumeration.STRING_TYPE);
		attributes.add(a);
		
		a = new Attribute();
		a.setName("owners");
		a.setLabel("Owners");
		a.setMultiValued(true);
		a.setOrder(order++);
		a.setType(TypeEnumeration.STRING_TYPE);
		attributes.add(a);
		
		a = new Attribute();
		a.setName("until");
		a.setLabel("Until");
		a.setMultiValued(false);
		a.setOrder(order++);
		a.setType(TypeEnumeration.DATE_TIME_TYPE);
		attributes.add(a);

		p.setAttributes(attributes );
		
		Node nodeStart = new Node();
		Node nodeApprove = new Node();
		Node nodeApply = new Node();
		Node nodeEnd = new Node();
		
		nodeStart.setName("Start");
		nodeStart.setDescription("Request access to privileged account");
		nodeStart.setType(NodeType.NT_START);
		addAccountFields (nodeStart, false);
		p.getNodes().add(nodeStart);
		
		nodeApprove.setName("Approve");
		nodeApprove.setDescription("Approve ");
		nodeApprove.setType(NodeType.NT_SCREEN);
		nodeApprove.setMailActor("${owners}");
		addAccountFields (nodeApprove, true);
		p.getNodes().add(nodeApprove);
		
		nodeApply.setName("Apply changes");
		nodeApply.setType(NodeType.NT_APPLY);
		nodeApply.setDescription("Apply changes");
		nodeApply.setApplyEntitlements(true);
		p.getNodes().add(nodeApply);
		
		nodeEnd.setName("End");
		nodeEnd.setDescription("End");
		nodeEnd.setType(NodeType.NT_END);
		p.getNodes().add(nodeEnd);
		
		addTransition (nodeStart, nodeApprove, "Request");
		addTransition (nodeStart, nodeEnd, "Cancel");
		addTransition (nodeApprove, nodeApply, "Approve");
		addTransition (nodeApprove, nodeEnd, "Reject");
		addTransition (nodeApply, nodeEnd, "");
	}

	private void addTransition(Node nodeStart, Node nodeEnd, String name) {
		Transition t = new Transition ();
		t.setName(name);
		t.setSource(nodeStart);
		t.setTarget(nodeEnd);
		nodeStart.getOutTransitions().add(t);
		nodeEnd.getInTransitions().add(t);
	}

	static String defaultFields[] = {
		"action",
		"userSelector",
	};
	
	private void addFields(Node node, boolean readOnly) throws InternalErrorException, NamingException, CreateException {
		int order = 1;
		for (String field: defaultFields)
		{
			Field f = new Field();
			f.setLabel( Labels.getLabel("com.soffid.iam.api.User."+field) );
			f.setName(field);
			f.setOrder( new Long (order ++));
			f.setReadOnly(readOnly);
			node.getFields().add(f);
		}
		
		for (DataType ad: EJBLocator.getAdditionalDataService().findDataTypes2(MetadataScope.USER))
		{
			if (! ad.isReadOnly()) {
				Field f = new Field();
				WebDataType wdt = new WebDataType(ad);
				f.setLabel( wdt.getLabel() );
				f.setName( wdt.getName());
				f.setOrder( new Long (order ++));
				f.setValidationScript(wdt.getVisibilityExpression());
				f.setVisibilityScript(wdt.getVisibilityExpression());
				f.setReadOnly(ad.isReadOnly());
				f.setRequired(ad.isRequired());
				node.getFields().add(f);
			}
		}
	}

	private void addPermFields(Node node, boolean readOnly) throws InternalErrorException, NamingException, CreateException {
		int order = 1;
		Field f = new Field();
		f.setLabel( "Permissions" );
		f.setName( "grants");
		f.setOrder( new Long (order ++));
		f.setReadOnly(readOnly);
		node.getFields().add(f);
		
	}

	private void addAccountFields(Node node, boolean readOnly) throws InternalErrorException, NamingException, CreateException {
		int order = 1;
			
		Field f = new Field();
		f.setLabel( "Account name" );
		f.setName( "account");
		f.setOrder( new Long (order ++));
		f.setReadOnly(readOnly);
		node.getFields().add(f);

		f = new Field();
		f.setLabel( "System" );
		f.setName( "systemName");
		f.setOrder( new Long (order ++));
		f.setReadOnly(readOnly);
		node.getFields().add(f);

		f = new Field();
		f.setLabel( "Login name" );
		f.setName( "loginName");
		f.setOrder( new Long (order ++));
		f.setReadOnly(readOnly);
		node.getFields().add(f);

		f = new Field();
		f.setLabel( "Server" );
		f.setName( "server");
		f.setOrder( new Long (order ++));
		f.setReadOnly(readOnly);
		node.getFields().add(f);

		f = new Field();
		f.setLabel( "Until date" );
		f.setName( "until");
		f.setOrder( new Long (order ++));
		f.setReadOnly(true);
		node.getFields().add(f);
	}
	
	public void raise(Event event) {
		Textbox name = (Textbox) getFellow("name");
		name.setValue ("");
		Listbox type = (Listbox) getFellow("type");
		type.setSelectedIndex(0);
		doHighlighted();
		name.focus();
	}
	
	public void hide(Event event) {
		setVisible(false);
	}

	public void createSampleProcesses() throws InternalErrorException, NamingException, CreateException {
		WorkflowType type;
		Iterator names = WorkflowType.names().iterator();
		Iterator literals = WorkflowType.literals().iterator();
		while (names.hasNext())
		{
			String name = (String) names.next();
			String literal = (String) literals.next();
			type = WorkflowType.fromString(literal.toString());
			createProcess (name, type); 
		}
	}

	private void createProcess(String name, WorkflowType t) throws InternalErrorException, NamingException, CreateException {
		if (t == WorkflowType.WT_REQUEST)
			return;
		Process p = new Process();
		p.setName(Labels.getLabel("com.soffid.iam.addons.bpm.common.WorkflowType."+name));
		p.setDescription(Labels.getLabel("com.soffid.iam.addons.bpm.common.WorkflowType."+name));
		p.setInitiators("*");
		p.setManagers("SOFFID_ADMIN");
		p.setObservers("admin");
		p.setType(t);
		BpmEditorService svc = (BpmEditorService) new InitialContext().lookup(BpmEditorServiceHome.JNDI_NAME);
		
		if (! svc.findByName(p.getName()).isEmpty())
			return;
		
		if (t == WorkflowType.WT_USER)
			createUserTemplate(p);
		else if (t == WorkflowType.WT_ACCOUNT_RESERVATION)
			createAccountTemplate(p);
		else if (t == WorkflowType.WT_DELEGATION)
			createDelegationTemplate(p);
		else
			createPermissionsTemplate(p);
		p = svc.create(p);
	}

}
