package com.soffid.iam.addons.bpm.web;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import com.google.common.collect.Lists;
import com.soffid.iam.EJBLocator;
import com.soffid.iam.addons.bpm.common.Attribute;
import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.Node;
import com.soffid.iam.addons.bpm.common.NodeType;
import com.soffid.iam.addons.bpm.common.Process;
import com.soffid.iam.addons.bpm.common.Transition;
import com.soffid.iam.addons.bpm.common.WorkflowType;
import com.soffid.iam.api.DataType;
import com.soffid.iam.api.MetadataScope;

import es.caib.seycon.ng.comu.TypeEnumeration;
import es.caib.seycon.ng.exception.InternalErrorException;
import es.caib.zkib.component.DataListbox;
import es.caib.zkib.component.DataModel;
import es.caib.zkib.component.Form;
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
		else
			createPermissionsTemplate(p);
		
		DataModel model = (DataModel) getParent().getFellow("model");
		String path = XPathUtils.createPath(model, "/process", p);
		setVisible(false);
		
		DataListbox lb = (DataListbox) getParent().getFellow("processListbox");
		lb.setSelectedIndex( lb.getItemCount() - 1);
		
		Window processEditor = (Window) getParent().getFellow("editor").getFellow("w");
//		Form f = (Form) getParent().getFellow("editor").getFellow("form");
//		f.setDataPath("/model:"+path);
		processEditor.doHighlighted();
	}

	private void createUserTemplate(Process p) throws InternalErrorException, NamingException, CreateException {
		Node nodeStart = new Node();
		Node nodeApprove = new Node();
		Node nodeApply = new Node();
		Node nodeEnd = new Node();
		
		Attribute att = new Attribute();
		att.setLabel(Labels.getLabel("com.soffid.iam.api.User.action"));
		att.setName("action");
		att.setValues(Arrays.asList("A:New%20user","M:Modify%20user","D:Disable%20user","E:Enable%20user"));
		att.setOrder(1L);
		p.getAttributes().add(att);

		att = new Attribute();
		att.setLabel(Labels.getLabel("com.soffid.iam.api.User.userSelector"));
		att.setName("userSelector");
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
		addFields (nodeStart, false);
		p.getNodes().add(nodeStart);
		
		nodeApprove.setName("Approve");
		nodeApprove.setDescription("Approve ");
		nodeApprove.setType(NodeType.NT_SCREEN);
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
		nodeApprove.setMailActor("admin");
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
			Field f = new Field();
			f.setLabel( ad.getLabel() );
			f.setName( ad.getCode());
			f.setOrder( new Long (order ++));
			f.setValidationScript(ad.getVisibilityExpression());
			f.setVisibilityScript(ad.getVisibilityExpression());
			node.getFields().add(f);
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

}
