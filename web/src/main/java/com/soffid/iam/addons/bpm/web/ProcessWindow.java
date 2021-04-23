package com.soffid.iam.addons.bpm.web;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.naming.InitialContext;

import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.ComponentNotFoundException;
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
import org.zkoss.zul.impl.InputElement;

import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.Filter;
import com.soffid.iam.addons.bpm.common.Node;
import com.soffid.iam.addons.bpm.common.NodeType;
import com.soffid.iam.addons.bpm.common.Process;
import com.soffid.iam.addons.bpm.common.Transition;
import com.soffid.iam.addons.bpm.common.Trigger;
import com.soffid.iam.addons.bpm.common.WorkflowType;
import com.soffid.iam.addons.bpm.core.ejb.BpmEditorService;
import com.soffid.iam.addons.bpm.core.ejb.BpmEditorServiceHome;
import com.soffid.iam.web.component.CustomField3;
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
	String version = "2";
	
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
			Node node = new Node();
			node.setType(NodeType.NT_SCREEN);
			XPathUtils.createPath(ctx.getDataSource(), "/nodes", node);
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

	private EventListener onNewFilter = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			Grid fields = (Grid) getFellow(getFieldsName());
			BindContext ctx = XPathUtils.getComponentContext(event.getTarget());
			
			Filter field = new Filter();
			field.setWeight(new Long(1));
			XPathUtils.createPath(ctx.getDataSource(), "/filters", field);
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

	public void onRemoveFilter(Event event) {
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
			edit(event.getTarget().getPreviousSibling(),
					"{\"executionContext\":\"org.jbpm.graph.exe.ExecutionContext\","
							  + "\"serviceLocator\":\"com.soffid.iam.ServiceLocator\"}");
		}
	};

	
	public void editValidationScript (Event event) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		edit (event.getTarget().getPreviousSibling(),
						"{\"serviceLocator\":\"com.soffid.iam.ServiceLocator\"}");

	}

	public void editTriggerScript (Event event) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		edit (event.getTarget().getPreviousSibling(),
						"{\"serviceLocator\":\"com.soffid.iam.EJBLocator\", "
						+ "\"worflowWindow\":\"es.caib.bpm.toolkit.WorkflowWindow\", "
						+ "\"task\":\"com.soffid.iam.bpm.api.TaskInstance\"}");

	}

	public void editVisibilityScript (Event event) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		edit ( event.getTarget().getPreviousSibling(),
						"{\"serviceLocator\":\"com.soffid.iam.ServiceLocator\"}");

	}

	private CustomField3 typeListbox;
	private Listbox nodesListbox;
	private CustomField3 grantTypeListbox;
	
	public void onCreate() {
		getFellow("save").addEventListener("onClick", onSave);
		getFellow("saveAndPublish").addEventListener("onClick", onPublish);
		getFellow("cancel").addEventListener("onClick", onCancel);
		getFellow("newNodeButton").addEventListener("onClick", onNewNode);
		typeListbox = (CustomField3) getFellow("type");
		typeListbox.addEventListener("onChange", onSelectType);
		grantTypeListbox = (CustomField3) getFellow("grantScreenListbox");
		grantTypeListbox.addEventListener("onSelect", onSelectType);
		getParent().getFellow("form").addEventListener("onChangeXPath", onChangeXPath);
		getFellow("container").addEventListener("onChangeXPath", onChangeXPath);
		nodesListbox = ((Listbox) getFellow("nodes"));
		getFellow("newFieldButton").addEventListener("onClick", onNewField);
		try {
			getFellow("newFilterButton").addEventListener("onClick", onNewFilter);
		} catch (ComponentNotFoundException e) {
			
		}
//		getFellow("newFieldButton2").addEventListener("onClick", onNewField);
		getFellow("newTriggerButton").addEventListener("onClick", onNewTrigger);
		getFellow("newOutTransitionButton").addEventListener("onClick", onNewTransition);
		getFellow("newInTransitionButton").addEventListener("onClick", onNewTransition);
//		getFellow("editScriptButton").addEventListener("onClick", onEditScript);
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
		if ( typeListbox.getValue() != null && nodesListbox.getSelectedItem() != null)
		{
			String grantType = (String) grantTypeListbox.getValue();
			Node node = (Node) nodesListbox.getSelectedItem().getValue();
			WorkflowType processType = (WorkflowType) XPathUtils.getValue(this, "type");
			NodeType type = (NodeType) typeListbox.getValue();
			getFellow("screenType").setVisible(type == NodeType.NT_SCREEN || ( type == NodeType.NT_START && processType.equals(WorkflowType.WT_USER)));
			getFellow("grantScreenType").setVisible(type == NodeType.NT_GRANT_SCREEN || (type == NodeType.NT_START && processType.equals(WorkflowType.WT_PERMISSION)));
			getFellow("fieldsTab").setVisible(
					type == NodeType.NT_SCREEN || 
					type == NodeType.NT_MATCH_SCREEN || 
					type == NodeType.NT_GRANT_SCREEN || 
					(type == NodeType.NT_START  ));
			getFellow("triggersTab").setVisible(type == NodeType.NT_SCREEN || type == NodeType.NT_START );
			try {
				getFellow("filtersTab").setVisible(	type == NodeType.NT_MATCH_SCREEN );
				getFellowIfAny("screenMatchType").setVisible(type == NodeType.NT_MATCH_SCREEN);
			} catch (ComponentNotFoundException e) { }
			getFellow("actorRow").setVisible(type == NodeType.NT_SCREEN);
			getFellow("actorRow2").setVisible(type == NodeType.NT_GRANT_SCREEN);
			getFellow("customType").setVisible(type == NodeType.NT_CUSTOM);
			if (type == NodeType.NT_START)
				grantTypeListbox.setListOfValues(new String[] {
						"enter: " + Labels.getLabel("bpm.grantTypeList"),
						"request: " + Labels.getLabel("bpm.grantTypeRequest"),
//						"displayPending: " + Labels.getLabel("bpm.grantTypeDisplayPending"),
//						"displayAll: " + Labels.getLabel("bpm.grantTypeDisplayAll"),
//						"displayApproved: " + Labels.getLabel("bpm.grantTypeDisplayApproved"),
//						"displayRejected:" + Labels.getLabel("bpm.grantTypeDisplayRejected")
				});
			else
				grantTypeListbox.setListOfValues(new String[] {
						"enter: " + Labels.getLabel("bpm.grantTypeList"),
//						"request: " + Labels.getLabel("bpm.grantTypeRequest"),
						"displayPending: " + Labels.getLabel("bpm.grantTypeDisplayPending"),
						"displayAll: " + Labels.getLabel("bpm.grantTypeDisplayAll"),
						"displayApproved: " + Labels.getLabel("bpm.grantTypeDisplayApproved"),
						"displayRejected:" + Labels.getLabel("bpm.grantTypeDisplayRejected")
				});
			
			grantTypeListbox.updateMetadata();

			getFellow("mailType").setVisible(type == NodeType.NT_MAIL);
			getFellow("applyType").setVisible(type == NodeType.NT_APPLY && processType != WorkflowType.WT_ACCOUNT_RESERVATION);
			getFellow("applyAccountType").setVisible(type == NodeType.NT_APPLY && processType == WorkflowType.WT_ACCOUNT_RESERVATION);
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
		updateMailShortcut(null);
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

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void edit(Component component, String vars ) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		if ("3".equals(version)) {
			Class.forName("com.soffid.iam.web.popup.Editor")
			.getMethod("edit", InputElement.class, String.class)
			.invoke(null, component, vars);

		} else {
			Events.sendEvent(new Event ("onEdit", 
					getDesktop().getPage("editor").getFellow("top"),
					new Object[] { component, vars }
					));
		}
	}
	
	public void updateMailShortcut (Event ev) {
		Boolean b = false;
		try {
			b = (Boolean) XPathUtils.eval(getFellow("container"), "mailShortcut");
		} catch (Exception e) {}
		getFellow("approveTransition").setVisible(Boolean.TRUE.equals(b));
		getFellow("denyTransition").setVisible(Boolean.TRUE.equals(b));
	}
}
