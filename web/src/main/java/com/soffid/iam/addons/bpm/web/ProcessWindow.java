package com.soffid.iam.addons.bpm.web;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.naming.InitialContext;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.Node;
import com.soffid.iam.addons.bpm.common.NodeType;
import com.soffid.iam.addons.bpm.common.Process;
import com.soffid.iam.addons.bpm.common.Transition;
import com.soffid.iam.addons.bpm.common.Trigger;
import com.soffid.iam.addons.bpm.common.WorkflowType;
import com.soffid.iam.addons.bpm.core.ejb.BpmEditorService;
import com.soffid.iam.addons.bpm.core.ejb.BpmEditorServiceHome;
import com.sun.mail.imap.protocol.ListInfo;

import es.caib.bpm.vo.PredefinedProcessType;
import es.caib.zkib.binder.BindContext;
import es.caib.zkib.component.DataGrid;
import es.caib.zkib.component.DataListbox;
import es.caib.zkib.component.DataModel;
import es.caib.zkib.datamodel.DataNode;
import es.caib.zkib.datamodel.DataNodeCollection;
import es.caib.zkib.datasource.XPathUtils;
import es.caib.zkib.events.XPathRerunEvent;
import es.caib.zkib.zkiblaf.Missatgebox;

public class ProcessWindow extends Window {
	private EventListener onSave = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			DataNode node =  (DataNode) XPathUtils.getValue( ProcessWindow.this, "/");
			node.update();
			getDataModel().commit();
			setVisible(false);
		}
	};

	private EventListener onPublish = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			DataNode dataNode =  (DataNode) XPathUtils.getValue( ProcessWindow.this, "/");
			dataNode.update();
			getDataModel().commit();
			com.soffid.iam.addons.bpm.common.Process process = (Process) dataNode.getInstance();
			BpmEditorService svc = (BpmEditorService) new InitialContext().lookup(BpmEditorServiceHome.JNDI_NAME);
			svc.publish(process);
			setVisible(false);
		}
	};

	private EventListener onCancel = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			Missatgebox.confirmaOK_CANCEL("If you confirm to exit, any change will be dascarded", onConfirmCancel);
		}
	};

	private EventListener onConfirmCancel = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			if (event.getData().equals( new Integer ( Missatgebox.OK)))
			{
				getDataModel().refresh();
				setVisible(false);
			}
		}
	};

	private EventListener onNewNode = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			DataListbox lb = (DataListbox) getFellow("nodes");
			BindContext ctx = XPathUtils.getComponentContext(event.getTarget());
			XPathUtils.createPath(ctx.getDataSource(), "/nodes", new Node());
			lb.setSelectedIndex( lb.getItemCount() - 1);
		}
	};

	private EventListener onSelectType = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			enableStepDetails();
		}
	};

	private EventListener onChangeXPath = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			enableStepDetails();
		}
	};

	private EventListener onNewField = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			Grid fields = (Grid) getFellow(getFieldsName());
			BindContext ctx = XPathUtils.getComponentContext(event.getTarget());
			
			Collection<Field> fieldList = (Collection<Field>) XPathUtils.getValue(ctx.getDataSource(), "/fields");
			int order = 1;
			for (Field field: fieldList)
			{
				if (field.getOrder() != null && field.getOrder().intValue() >= order)
					order = field.getOrder().intValue() + 1 ;
			}
			Field field = new Field();
			field.setOrder(new Long(order));
			XPathUtils.createPath(ctx.getDataSource(), "/fields", field);
			Textbox tb = (Textbox) fields.getRows().getLastChild().getFirstChild().getNextSibling();
			tb.focus();
		}

	};

	public void onChangeTriggerType(Event ev) throws Exception {
		Component target ;
		if (ev.getName().equals("onSelect"))
		{
			target = ev.getTarget().getParent();
		}
		else
		{
			target = (Component) ev.getData();
		}
		String type = (String) XPathUtils.getValue(target, "name");
		((Component) target.getChildren().get(1)).setVisible( "onChange".equals(type));
	};
	
	private EventListener onNewTrigger = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			Grid triggers = (Grid) getFellow("triggers");
			BindContext ctx = XPathUtils.getComponentContext(event.getTarget());
			
			Trigger trigger = new Trigger();
			trigger.setName("onPrepareTransition");
			XPathUtils.createPath(ctx.getDataSource(), "/triggers", trigger);
			Textbox tb = (Textbox) triggers.getRows().getLastChild().getFirstChild().getNextSibling();
			tb.focus();
		}

	};

	private String getFieldsName() {
//		NodeType type = (NodeType) typeListbox.getSelectedItem().getValue();
		return "fields";
	}

	private EventListener onNewTransition = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			BindContext ctx = XPathUtils.getComponentContext(event.getTarget());
			Node node = (Node) XPathUtils.getValue(event.getTarget(), "/");
			Transition t = new Transition ();
			t.setSource(node);
			t.setTarget(node);
			XPathUtils.createPath(ctx.getDataSource(), "/inTransitions", t);
			XPathUtils.createPath(ctx.getDataSource(), "/outTransitions", t);
		}
	};

	public void onRemoveField(Event event) {
		BindContext ctx = XPathUtils.getComponentContext(event.getTarget());
		XPathUtils.removePath(ctx.getDataSource(), ctx.getXPath());
	}

	public void onRemoveTrigger(Event event) {
		BindContext ctx = XPathUtils.getComponentContext(event.getTarget());
		XPathUtils.removePath(ctx.getDataSource(), ctx.getXPath());
	}

	private EventListener onEditScript = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
		    Events.sendEvent(new Event ("onEdit", 
		    		getDesktop().getPage("editor").getFellow("top"),
		    		new Object[] {
						    event.getTarget().getPreviousSibling(),
							"{\"executionContext\":\"org.jbpm.graph.exe.ExecutionContext\","
							  + "\"serviceLocator\":\"com.soffid.iam.ServiceLocator\"}"
					}
		    ));
		}
	};

	
	public void editValidationScript (Event event) {
	    Events.sendEvent(new Event ("onEdit", 
	    		getDesktop().getPage("editor").getFellow("top"),
	    		new Object[] {
					    event.getTarget().getPreviousSibling(),
						"{\"serviceLocator\":\"com.soffid.iam.ServiceLocator\"}"
				}
	    ));

	}

	public void editTriggerScript (Event event) {
	    Events.sendEvent(new Event ("onEdit", 
	    		getDesktop().getPage("editor").getFellow("top"),
	    		new Object[] {
					    event.getTarget().getPreviousSibling(),
						"{\"serviceLocator\":\"com.soffid.iam.EJBLocator\", "
						+ "\"worflowWindow\":\"es.caib.bpm.toolkit.WorkflowWindow\", "
						+ "\"task\":\"com.soffid.iam.bpm.api.TaskInstance\"}"
				}
	    ));

	}

	public void editVisibilityScript (Event event) {
	    Events.sendEvent(new Event ("onEdit", 
	    		getDesktop().getPage("editor").getFellow("top"),
	    		new Object[] {
					    event.getTarget().getPreviousSibling(),
						"{\"serviceLocator\":\"com.soffid.iam.ServiceLocator\"}"
				}
	    ));

	}

	private Listbox typeListbox;
	private Listbox nodesListbox;
	private Listbox grantTypeListbox;
	
	public void onCreate() {
		getFellow("save").addEventListener("onClick", onSave);
		getFellow("saveAndPublish").addEventListener("onClick", onPublish);
		getFellow("cancel").addEventListener("onClick", onCancel);
		getFellow("newNodeButton").addEventListener("onClick", onNewNode);
		typeListbox = (Listbox) getFellow("type");
		typeListbox.addEventListener("onSelect", onSelectType);
		grantTypeListbox = (Listbox) getFellow("grantScreenListbox");
		grantTypeListbox.addEventListener("onSelect", onSelectType);
		getParent().getFellow("form").addEventListener("onChangeXPath", onChangeXPath);
		getFellow("container").addEventListener("onChangeXPath", onChangeXPath);
		nodesListbox = ((Listbox) getFellow("nodes"));
		getFellow("newFieldButton").addEventListener("onClick", onNewField);
//		getFellow("newFieldButton2").addEventListener("onClick", onNewField);
		getFellow("newTriggerButton").addEventListener("onClick", onNewTrigger);
		getFellow("newOutTransitionButton").addEventListener("onClick", onNewTransition);
		getFellow("newInTransitionButton").addEventListener("onClick", onNewTransition);
		getFellow("editScriptButton").addEventListener("onClick", onEditScript);
	}

	private DataModel getDataModel() {
		return (DataModel) getPage().getFellow("model");
	}

	@Override
	public void doHighlighted() {
		super.doHighlighted();
		nodesListbox.setSelectedIndex(0);
	}
	
	public void onRemoveNode ( Event event )
	{
		BindContext ctx = XPathUtils.getComponentContext(event.getTarget());
		XPathUtils.removePath(ctx.getDataSource(), ctx.getXPath());
	}
	
	private void enableStepDetails()
	{
		if ( typeListbox.getSelectedItem() != null)
		{
			String grantType = 
				grantTypeListbox.getSelectedItem() == null ? null :
				(String) grantTypeListbox.getSelectedItem().getValue();
			Node node = (Node) nodesListbox.getSelectedItem().getValue();
			WorkflowType processType = (WorkflowType) XPathUtils.getValue(this, "type");
			NodeType type = (NodeType) typeListbox.getSelectedItem().getValue();
			getFellow("screenType").setVisible(type == NodeType.NT_SCREEN || ( type == NodeType.NT_START && processType.equals(WorkflowType.WT_USER)));
			getFellow("grantScreenType").setVisible(type == NodeType.NT_GRANT_SCREEN || (type == NodeType.NT_START && processType.equals(WorkflowType.WT_PERMISSION)));
			getFellow("fieldsTab").setVisible(
					type == NodeType.NT_SCREEN || 
					type == NodeType.NT_GRANT_SCREEN || 
					(type == NodeType.NT_START  && ! (processType == WorkflowType.WT_PERMISSION && "request".equals(grantType))));
			getFellow("triggersTab").setVisible(type == NodeType.NT_SCREEN || type == NodeType.NT_START );
			getFellow("actorRow").setVisible(type == NodeType.NT_SCREEN);
			getFellow("actorRow2").setVisible(type == NodeType.NT_GRANT_SCREEN);
			getFellow("customType").setVisible(type == NodeType.NT_CUSTOM);
			getFellow("grantTypeDiv").setVisible(type == NodeType.NT_START  && processType == WorkflowType.WT_PERMISSION ||
					type == NodeType.NT_GRANT_SCREEN) ;
			grantTypeListbox.getItemAtIndex(0).setDisabled(false);
			grantTypeListbox.getItemAtIndex(1).setDisabled(type != NodeType.NT_START);
			grantTypeListbox.getItemAtIndex(2).setDisabled(type == NodeType.NT_START);
			grantTypeListbox.getItemAtIndex(3).setDisabled(type == NodeType.NT_START);
			grantTypeListbox.getItemAtIndex(4).setDisabled(type == NodeType.NT_START);
			grantTypeListbox.getItemAtIndex(5).setDisabled(type == NodeType.NT_START);
			getFellow("mailType").setVisible(type == NodeType.NT_MAIL);
			getFellow("applyType").setVisible(type == NodeType.NT_APPLY);
			if (node != null && node.getFields() != null)
			{
				Collections.sort( node.getFields(), new Comparator<Field>() {
	
					@Override
					public int compare(Field o1, Field o2) {
						if (o2.getOrder() == null && o1.getOrder() == null)
							return 0;
						if (o2.getOrder() == null)
							return -1;
						if (o2.getOrder() == null)
							return +1;
						return o1.getOrder().compareTo(o2.getOrder());
					}
					
				});
			}
		}
	}
	
	public void renumAttributes (DropEvent event)
	{
		DataGrid grid = (DataGrid) getFellow ( getFieldsName());
		Row srcRow = (Row) event.getDragged().getParent().getParent();
		Row targetRow = (Row) event.getTarget();
		if (srcRow != targetRow)
		{
			Field srcField = (Field) XPathUtils.getValue(srcRow, ".");
			Field targetField = (Field) XPathUtils.getValue(targetRow, ".");
			
			ContainerDiv container =  (ContainerDiv) getFellow("container");
			Node node =  (Node) XPathUtils.getValue((Component) container, "/");
			node.getFields().remove(srcField);
			int pos = node.getFields().indexOf(targetField);
			node.getFields().add(pos, srcField);
			renameAttributes (node);

			String db = grid.getDataPath();
			grid.setDataPath(null);
			grid.setDataPath(db);
			XPathRerunEvent event2 = new XPathRerunEvent(grid.getDataSource(), grid.getDataPath());
			grid.onUpdate(event2);
		}
	}

	private void renameAttributes(Node node) {
		int pos = 1;
		for ( Field f: node.getFields())
		{
			f.setOrder(new Long(pos++));
		}
	}
}
