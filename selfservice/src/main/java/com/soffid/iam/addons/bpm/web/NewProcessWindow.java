package com.soffid.iam.addons.bpm.web;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import com.soffid.iam.EJBLocator;
import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.Node;
import com.soffid.iam.addons.bpm.common.NodeType;
import com.soffid.iam.addons.bpm.common.Process;
import com.soffid.iam.addons.bpm.common.Transition;
import com.soffid.iam.addons.bpm.common.WorkflowType;
import com.soffid.iam.api.DataType;
import com.soffid.iam.api.MetadataScope;

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
		Node nodeRequest = new Node();
		Node nodeApprove = new Node();
		Node nodeApply = new Node();
		Node nodeEnd = new Node();
		
		nodeStart.setName("Start");
		nodeStart.setType(NodeType.NT_START);
		p.getNodes().add(nodeStart);
		
		nodeRequest.setName("Request");
		nodeRequest.setDescription("Request new user management process");
		nodeRequest.setType(NodeType.NT_SCREEN);
		addFields (nodeRequest, false);
		p.getNodes().add(nodeRequest);
		
		nodeApprove.setName("Approve");
		nodeApprove.setDescription("Approve ");
		nodeApprove.setType(NodeType.NT_SCREEN);
		addFields (nodeApprove, true);
		p.getNodes().add(nodeApprove);
		
		nodeApply.setName("Apply changes");
		nodeApply.setType(NodeType.NT_APPLY);
		nodeApply.setDescription("Apply changes");
		p.getNodes().add(nodeApply);
		
		nodeEnd.setName("End");
		nodeEnd.setDescription("End");
		nodeEnd.setType(NodeType.NT_END);
		p.getNodes().add(nodeEnd);
		
		addTransition (nodeStart, nodeRequest, "");
		addTransition (nodeRequest, nodeApprove, "Request");
		addTransition (nodeRequest, nodeEnd, "Cancel");
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
		"userName",
		"userType",
		"firstName",
		"lastName",
		"primaryGroup",
		"shortName",
		"mailDomain",
		"comments"
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
		
		for (DataType ad: EJBLocator.getAdditionalDataService().findDataTypes(MetadataScope.USER))
		{
			Field f = new Field();
			f.setLabel( ad.getLabel() );
			f.setName( "attributes{"+ad.getCode()+"}");
			f.setOrder( new Long (order ++));
			f.setValidationScript(ad.getVisibilityExpression());
			f.setVisibilityScript(ad.getVisibilityExpression());
			node.getFields().add(f);
		}
	}

}
