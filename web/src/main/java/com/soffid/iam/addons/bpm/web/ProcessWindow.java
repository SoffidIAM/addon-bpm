package com.soffid.iam.addons.bpm.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ejb.CreateException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.AbstractComponent;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.ComponentNotFoundException;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Row;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;
import org.zkoss.zul.impl.InputElement;

import com.soffid.addons.bpm.web.mxgraph.MxGraph;
import com.soffid.iam.EJBLocator;
import com.soffid.iam.addons.bpm.common.Attribute;
import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.Filter;
import com.soffid.iam.addons.bpm.common.InvocationField;
import com.soffid.iam.addons.bpm.common.Node;
import com.soffid.iam.addons.bpm.common.NodeType;
import com.soffid.iam.addons.bpm.common.Process;
import com.soffid.iam.addons.bpm.common.Transition;
import com.soffid.iam.addons.bpm.common.Trigger;
import com.soffid.iam.addons.bpm.common.WorkflowType;
import com.soffid.iam.addons.bpm.core.ejb.BpmEditorService;
import com.soffid.iam.addons.bpm.core.ejb.BpmEditorServiceHome;
import com.soffid.iam.api.DataType;
import com.soffid.iam.api.User;
import com.soffid.iam.web.component.CustomField3;
import com.sun.mail.imap.protocol.ListInfo;

import es.caib.bpm.vo.PredefinedProcessType;
import es.caib.seycon.ng.comu.TypeEnumeration;
import es.caib.seycon.ng.exception.InternalErrorException;
import es.caib.zkib.binder.BindContext;
import es.caib.zkib.component.DataGrid;
import es.caib.zkib.component.DataListbox;
import es.caib.zkib.component.DataListcell;
import es.caib.zkib.component.DataModel;
import es.caib.zkib.component.Form2;
import es.caib.zkib.datamodel.DataNode;
import es.caib.zkib.datamodel.DataNodeCollection;
import es.caib.zkib.datasource.XPathUtils;
import es.caib.zkib.events.XPathRerunEvent;
import es.caib.zkib.jxpath.JXPathException;
import es.caib.zkib.jxpath.Pointer;
import es.caib.zkib.zkiblaf.Missatgebox;

public class ProcessWindow extends Form2 {
	String version = "2";
	
	private EventListener onSave = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			DataNode node =  (DataNode) XPathUtils.eval( ProcessWindow.this, "/");
			node.update();
			getDataModel().commit();
			((EditorHandler)getPage().getFellow("frame")).hideDetails();
		}
	};

	private EventListener onPublish = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			MxGraph graph = (MxGraph) getFellow("graph");
			graph.getImage(	onImage );
		}
	};
	
	private EventListener onImage = new EventListener() {
		@Override
		public void onEvent(Event ev) throws Exception {
			byte[] image = (byte[]) ev.getData();
			DataNode dataNode =  (DataNode) XPathUtils.eval( ProcessWindow.this, "/");
			dataNode.update();
			getDataModel().commit();
			com.soffid.iam.addons.bpm.common.Process process = (Process) dataNode.getInstance();
			BpmEditorService svc = (BpmEditorService) new InitialContext().lookup(BpmEditorServiceHome.JNDI_NAME);
			svc.publish(process, image);
			((EditorHandler)getPage().getFellow("frame")).hideDetails();
		}
	};

	private EventListener onCancel = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			Missatgebox.confirmaOK_CANCEL(Labels.getLabel("bpm.discardChanges"), onConfirmCancel);
		}
	};

	private EventListener onConfirmCancel = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			if (event.getData().equals( new Integer ( Missatgebox.OK)))
			{
				getDataModel().refresh();
				((EditorHandler)getPage().getFellow("frame")).hideDetails();
			}
		}
	};

	private EventListener onSelectType = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			enableStepDetails();
			updateCurrentNodeModel();
		}

	};

	Object[] typeDrawing = new Object[] {
		NodeType.NT_ACTION, "Node", "node",
		NodeType.NT_APPLY, "Node", "apply",
		NodeType.NT_CUSTOM, "Symbol", "decision",
		NodeType.NT_END, "Symbol", "end", 
		NodeType.NT_FORK, "Symbol", "fork",
		NodeType.NT_GRANT_SCREEN, "Task", "taskgrant",
		NodeType.NT_JOIN, "Symbol", "join",
		NodeType.NT_MAIL, "Symbol", "mail",
		NodeType.NT_MATCH_SCREEN, "Task", "taskmatch",
		NodeType.NT_SCREEN, "Task", "task",
		NodeType.NT_START, "Symbol", "start",
		NodeType.NT_SYSTEM_INVOCATION, "Symbol", "system",
		NodeType.NT_TIMER, "Symbol", "timer"
	};
	
	private void updateCurrentNodeModel() throws SAXException, IOException, ParserConfigurationException, FactoryConfigurationError, Exception {
		if (currentNode != null && currentNode.getDiagramId() != null) {
			String xml = (String) XPathUtils.eval(this, "diagram");
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
					new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
			NodeList roots = doc.getElementsByTagName("root");
			for (int i = 0; i < roots.getLength(); i++) {
				for (org.w3c.dom.Node n = roots.item(i).getFirstChild(); n != null; n = n.getNextSibling()) {
					if (n instanceof Element) {
						Element e = (Element) n;
						if (currentNode.getDiagramId().equals(e.getAttribute("id")) ) {
							String type = e.getTagName().equals("Symbol") ?
									e.getAttribute("type") :
									e.getTagName();
							if (! isCompatible(currentNode.getType(), type)) {
								for (int j = 0; j < typeDrawing.length; j += 3) {
									if (typeDrawing[j] == currentNode.getType()) {
										Element e2 = doc.createElement((String) typeDrawing[j+1]);
										e2.setAttribute("id", e.getAttribute("id"));
										e2.setAttribute("label", e.getAttribute("label"));
										e2.setAttribute("type", (String) typeDrawing[j+2]);
										roots.item(i).insertBefore(e2, e);
										org.w3c.dom.Node nn;
										while ((nn = e.getFirstChild()) != null) {
											if (nn instanceof Element) {
												((Element) nn).setAttribute("style", (String) typeDrawing[j+2]);
											}
											e.removeChild(nn);
											e2.appendChild(nn);
										}
										roots.item(i).removeChild(e);
										break;
									}
								}
							}
						}
					}
				}
			}
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			StringWriter stringWriter = new StringWriter();
			transformer.transform(new DOMSource(doc), new StreamResult(stringWriter));
			String result = stringWriter.toString();
			
			MxGraph graph = (MxGraph) getFellow("graph");
			graph.setModel(result);
		}
	}

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

	private EventListener onNewInvocationField = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			Grid fields = (Grid) getFellow("invocationFields");
			BindContext ctx = XPathUtils.getComponentContext(event.getTarget());
			
			XPathUtils.createPath(ctx.getDataSource(), "/invocationFields", new InvocationField());
			CustomField3 tb = (CustomField3) fields.getRows().getLastChild().getFirstChild();
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

	public void onRemoveTransition(Event event) {
		BindContext ctx = XPathUtils.getComponentContext(event.getTarget());

		Transition t = (Transition) XPathUtils.getValue( ctx, ".");
		
		XPathUtils.removePath(ctx.getDataSource(), ctx.getXPath());

		t.getSource().getOutTransitions().remove(t);
		t.getTarget().getInTransitions().remove(t);
	}

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
							  + "\"serviceLocator\":\"com.soffid.iam.ServiceLocator\"}",
					"<b>executionContext</b>: JBPM Context<br>"
					+ "<b>serviceLocator</b>: <a target='_blank' href='http://www.soffid.org/doc/console/latest/iam-core/apidocs/index.html'>Service locator</a><br>");
		}
	};

	private Textbox currentFilterBox;

	
	public void editValidationScript (Event event) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		edit (event.getTarget().getPreviousSibling(),
						"{\"serviceLocator\":\"com.soffid.iam.ServiceLocator\"}",
						"<b>serviceLocator</b>: <a target='_blank' href='http://www.soffid.org/doc/console/latest/iam-core/apidocs/index.html'>Service locator</a><br>"
						+ "<b>inputFields</b>: Map of <a target='_blank' href='http://www.soffid.org/doc/console/latest/iam-web/apidocs/com/soffid/iam/web/component/InputField3.html'>input fields</a> by name<br>"
						+ "<b>value</b>: New field value<br>"
						+ "<b>attributes</b>: Map of current values. Contains the current field former value<br>"
						+ "<b>inputField</b>: Current <a target='_blank' href='http://www.soffid.org/doc/console/latest/iam-web/apidocs/com/soffid/iam/web/component/InputField3.html' target='_black'>input field</a><br>"
						+ "<b>ownerContext</b>: The workflow name<br><br>"
						+ "Expected return value: null or true if the value is valid. false if not. Any other value will be displayed as a warning");

	}

	public void editTriggerScript (Event event) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		edit (event.getTarget().getPreviousSibling(),
						"{\"serviceLocator\":\"com.soffid.iam.EJBLocator\", "
						+ "\"worflowWindow\":\"es.caib.bpm.toolkit.WorkflowWindow\", "
						+ "\"task\":\"com.soffid.iam.bpm.api.TaskInstance\"}",

						"<b>serviceLocator</b>: <a target='_blank' href='http://www.soffid.org/doc/console/latest/iam-core/apidocs/index.html'>Service locator</a><br>"
						+ "<b>inputFields</b>: Map of <a target='_blank' href='http://www.soffid.org/doc/console/latest/iam-web/apidocs/com/soffid/iam/web/component/InputField3.html'>input fields</a> by name<br>"
						+ "<b>value</b>: New field value<br>"
						+ "<b>attributes</b>: Map of current values. Contains the current field former value<br>"
						+ "<b>inputField</b>: Current <a target='_blank' href='http://www.soffid.org/doc/console/latest/iam-web/apidocs/com/soffid/iam/web/component/InputField3.html'>input field</a><br>"
						+ "<b>ownerContext</b>: The workflow name<br>"
						+ "<b>task</b>: <a target='_blank' href='http://www.soffid.org/doc/console/latest/iam-common/apidocs/com/soffid/iam/bpm/api/TaskInstance.html'>input fields</a> by name<br>");

	}

	public void editVisibilityScript (Event event) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		edit ( event.getTarget().getPreviousSibling(),
						"{\"serviceLocator\":\"com.soffid.iam.ServiceLocator\"}",
						"<b>serviceLocator</b>: <a target='_blank' href='http://www.soffid.org/doc/console/latest/iam-core/apidocs/index.html'>Service locator</a><br>"
								+ "<b>value</b>: New field value<br>"
								+ "<b>attributes</b>: Map of current values. Contains the current field former value<br>"
								+ "<b>inputField</b>: Current <a target='_blank' href='http://www.soffid.org/doc/console/latest/iam-web/apidocs/com/soffid/iam/web/component/InputField3.html'>input field</a><br>"
								+ "<b>inputFields</b>: Map of <a target='_blank' href='http://www.soffid.org/doc/console/latest/iam-web/apidocs/com/soffid/iam/web/component/InputField3.html'>input fields</a> by name<br>"
								+ "<b>ownerContext</b>: The workflow name<br><br>"
								+ "Expected return value: true or false");

	}

	public void editFilterExpression (Event event) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		Window tw = (Window) getFellow("textWindow");
		Textbox tb = (Textbox) tw.getFellow("editor");
		currentFilterBox = (Textbox) event.getTarget().getPreviousSibling();
		tb.setValue(currentFilterBox.getValue());
		tw.doHighlighted();
	}
	
	public void applyFilter(Event event) {
		Window tw = (Window) getFellow("textWindow");
		Textbox tb = (Textbox) tw.getFellow("editor");
		currentFilterBox.setValue(tb.getValue());
		tw.setVisible(false);
	}

	public void cleanFilter(Event event) {
		Window tw = (Window) getFellow("textWindow");
		tw.setVisible(false);
	}

	public void addField(Event event) throws InternalErrorException, NamingException, CreateException {
		Row r = (Row) event.getData();
		addField(r);
	}
	
	public void addField(Row r) throws InternalErrorException, NamingException, CreateException {
		String field = (String) XPathUtils.eval(r, "@name");
		final HtmlBasedComponent cell = (HtmlBasedComponent) r.getChildren().get(7);
		TypeEnumeration dataType = null;
		if (field != null) {
			final Collection<DataType> list = EJBLocator.getAdditionalDataService().findDataTypesByObjectTypeAndName2(User.class.getName(), field);
			if (! list.isEmpty())
				dataType = list.iterator().next().getType();
			
			if (dataType == null) {
				for (Attribute att: (List<Attribute>) XPathUtils.eval(this, "/attributes")) {
					if (field.equals(att.getName()))
					{
						dataType = att.getType();
						break;
					}
				}
			}
		}
		if (dataType != TypeEnumeration.ACCOUNT_TYPE &&
				dataType != TypeEnumeration.APPLICATION_TYPE &&
				dataType != TypeEnumeration.CUSTOM_OBJECT_TYPE &&
				dataType != TypeEnumeration.GROUP_TYPE &&
				dataType != TypeEnumeration.GROUP_TYPE_TYPE &&
				dataType != TypeEnumeration.HOST_TYPE &&
				dataType != TypeEnumeration.MAIL_DOMAIN_TYPE &&
				dataType != TypeEnumeration.MAIL_LIST_TYPE &&
				dataType != TypeEnumeration.NETWORK_TYPE &&
				dataType != TypeEnumeration.ROLE_TYPE &&
				dataType != TypeEnumeration.USER_TYPE) {
			cell.setVisible(false);
		} else {
			cell.setVisible(true);
		}
	}
	
	private CustomField3 typeListbox;
	private CustomField3 grantTypeListbox;

	private CustomField3 grantStartTypeListbox;

	private Node currentNode;
	
	public void onCreate() {
		getFellow("save").addEventListener("onClick", onSave);
		getFellow("saveAndPublish").addEventListener("onClick", onPublish);
		getFellow("cancel").addEventListener("onClick", onCancel);
		typeListbox = (CustomField3) getFellow("type");
		typeListbox.addEventListener("onChange", onSelectType);
		grantTypeListbox = (CustomField3) getFellow("grantScreenListbox");
		grantTypeListbox.addEventListener("onSelect", onSelectType);

		grantStartTypeListbox = (CustomField3) getFellow("startGrantType");
		grantStartTypeListbox.addEventListener("onSelect", onSelectType);

		getFellow("container").addEventListener("onChangeXPath", onChangeXPath);
		getFellow("newFieldButton").addEventListener("onClick", onNewField);
		getFellow("newInvocationFieldButton").addEventListener("onClick", onNewInvocationField);
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

	public void onRemoveNode ( Event event )
	{
		BindContext ctx = XPathUtils.getComponentContext(event.getTarget());
		XPathUtils.removePath(ctx.getDataSource(), ctx.getXPath());
	}
	
	private void enableStepDetails()
	{
		if ( typeListbox.getValue() != null &&
				!"".equals(typeListbox.getValue()) &&
				currentNode != null)
		{
			Node node = currentNode;
			WorkflowType processType = (WorkflowType) XPathUtils.eval(this, "type");
			NodeType type = (NodeType) typeListbox.getValue();
			getFellow("startType").setVisible(type == NodeType.NT_START );
			getFellow("startGrantType").setVisible(type == NodeType.NT_START && processType.equals(WorkflowType.WT_PERMISSION));
			getFellow("screenType").setVisible(type == NodeType.NT_SCREEN || ( type == NodeType.NT_START && processType.equals(WorkflowType.WT_USER)));
			getFellow("grantScreenType").setVisible(type == NodeType.NT_GRANT_SCREEN);
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
			getFellow("roleSelection").setVisible(type == NodeType.NT_SCREEN || 
					type == NodeType.NT_START && processType.equals(WorkflowType.WT_USER) ||
					type == NodeType.NT_START && processType.equals(WorkflowType.WT_PERMISSION));
			getFellow("actorRow2").setVisible(type == NodeType.NT_GRANT_SCREEN);
			getFellow("customType").setVisible(type == NodeType.NT_CUSTOM || type == NodeType.NT_ACTION || type == NodeType.NT_TIMER);
			getFellow("timerType").setVisible(type == NodeType.NT_TIMER);
			getFellow("asyncType").setVisible(type == NodeType.NT_ACTION || type == NodeType.NT_SYSTEM_INVOCATION);
			getFellow("systemType").setVisible(type == NodeType.NT_SYSTEM_INVOCATION);
			if (type == NodeType.NT_START)
				grantStartTypeListbox.setListOfValues(new String[] {
						"enter: " + Labels.getLabel("bpm.grantTypeList"),
						"request: " + Labels.getLabel("bpm.grantTypeRequest"),
				});
			else
				grantTypeListbox.setListOfValues(new String[] {
						"enter: " + Labels.getLabel("bpm.grantTypeList"),
						"displayPending: " + Labels.getLabel("bpm.grantTypeDisplayPending"),
						"displayAll: " + Labels.getLabel("bpm.grantTypeDisplayAll"),
						"displayApproved: " + Labels.getLabel("bpm.grantTypeDisplayApproved"),
						"displayRejected:" + Labels.getLabel("bpm.grantTypeDisplayRejected")
				});
			
			grantStartTypeListbox.updateMetadata();
			grantTypeListbox.updateMetadata();

			getFellow("mailType").setVisible(type == NodeType.NT_MAIL);
			getFellow("applyType").setVisible(type == NodeType.NT_APPLY && processType != WorkflowType.WT_ACCOUNT_RESERVATION);
			getFellow("applyUserChanges").setVisible(processType != WorkflowType.WT_DELEGATION);
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

	public void edit(Component component, String vars, String env ) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		if ("3".equals(version)) {
			Class.forName("com.soffid.iam.web.popup.Editor")
			.getMethod("edit", InputElement.class, String.class, String.class)
			.invoke(null, component, vars, env);

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
	

	public void onChangeForm(Event ev) throws Exception {
		try {
			Process process = (Process) XPathUtils.eval(this, "instance");
			String diagram = process.getDiagram();
			
			
			if (diagram == null || diagram.isEmpty()) {
				diagram = new DiagramDrawer().draw(process);
			}
			MxGraph graph = (MxGraph) getFellow("graph");
			graph.setModel(diagram);
			showPane();
		} catch (JXPathException e) {}
	}

	public void showPane() {
		MxGraph graph = (MxGraph) getFellow("graph");
		String selected = graph.getSelected();
		boolean found = false;
		Process process = (Process) XPathUtils.eval(this, "instance");
		ContainerDiv d = (ContainerDiv) getFellow("container");
		currentNode = null;
		Tabbox tb = (Tabbox) getFellow("nodeTabbox");
		if (selected != null) {
			int i = 0;
			Listbox lb = (Listbox) getFellow("nodes");
			node: for (Node node: process.getNodes()) {
				if (selected.equals(node.getDiagramId())) {
					currentNode = node;
					lb.setSelectedIndex(i);
					found = true;
					enableStepDetails();
					if (tb.getSelectedIndex() == 5)
						tb.setSelectedIndex(0);
					break;
				}
				for (Transition tran: node.getOutTransitions()) {
					if (selected.equals(tran.getDiagramId())) {
						currentNode = node;
						lb.setSelectedIndex(i);
						found = true;
						enableStepDetails();
						tb.setSelectedIndex(5);
						break node;						
					}
				}
				i ++;
			}
		}
		getFellow("processDiv").setVisible(!found);
		getFellow("container").setVisible(found);
	}

	
	public void updateGraph(Event e) throws Exception {
		Process process;
		try {
			process = (Process) XPathUtils.eval(this, "instance");
		} catch (JXPathException ex) {
			return; // No process selected
		}
		
		for (Node node: process.getNodes())
			node.setToRemove(true);
		
		String xml = (String) e.getData();
		XPathUtils.setValue(this, "diagram", xml);

		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		NodeList roots = doc.getElementsByTagName("root");
		Set<String> ids = new HashSet<>();
		for (int i = 0; i < roots.getLength(); i++) {
			Element root = (Element) roots.item(i);
			for (org.w3c.dom.Node element = root.getFirstChild(); 
					element != null;
					element = element.getNextSibling()) {
				if (element instanceof Element) {
					final Element data = (Element) element;
					String tag = data.getTagName();
					String id = data.getAttribute("id");
					String parentId = data.getFirstChild() == null ? null :
						((Element)data.getFirstChild()).getAttribute("parent");
					String type = data.getAttribute("type");
					String label = data.getAttribute("label");
					if (id != null && !id.trim().isEmpty()) {
						if (tag.equals("Edge"))
							updateEdge(process, id, label, (Element) element.getFirstChild());
						if (type != null && !type.isEmpty())
							updateNode(process, id, type, label, parentId);
						ids.add(id);
					}
				}
			}
		}
		
		for (Node node: process.getNodes()) {
			for (Iterator<Transition> it = node.getOutTransitions().iterator(); it.hasNext();) {
				Transition tran = it.next();
				if (!ids.contains(tran.getDiagramId())) {
					it.remove();
				}
			}
		}
		
		getDataSource().sendEvent(new XPathRerunEvent(getDataSource(), getDataPath()));
		DataListbox lb = (DataListbox) getFellow("nodes");
		lb.sendEvent(new XPathRerunEvent(lb, "/"));
		showPane();
	}

	private void updateEdge(Process process, String id, String label, Element firstChild) {
		String source = firstChild.getAttribute("source");
		String target = firstChild.getAttribute("target");
		Node sourceNode = null;
		Node targetNode = null;
		for (Node node: process.getNodes()) {
			if (node.getDiagramId().equals(source))
				sourceNode = node;
			if (node.getDiagramId().equals(target))
				targetNode = node;
		}
		
		boolean anyChange = false;
		boolean found = false;
		nodes: for (Node node: process.getNodes()) {
			for (Transition tran: node.getOutTransitions()) {
				if (tran.getDiagramId().equals(id)) {
					found = true;
					if (sourceNode == null || targetNode == null) {
						tran.getSource().getOutTransitions().remove(tran);
						tran.getTarget().getInTransitions().remove(tran);
						anyChange = true;
					}
					else 
					{
						if (tran.getName() == null ? label != null : 
							! tran.getName().equals(label))
						{
							tran.setName(label);
							anyChange = true;
						}
						if (tran.getSource() != sourceNode) {
							if (tran.getSource() != null)
								tran.getSource().getOutTransitions().remove(tran);
							tran.setSource(sourceNode);
							sourceNode.getOutTransitions().add(tran);
							anyChange = true;
						}
						if (tran.getTarget() != targetNode) {
							if (tran.getTarget() != null)
								tran.getTarget().getInTransitions().remove(tran);
							tran.setTarget(targetNode);
							targetNode.getInTransitions().add(tran);
							anyChange  =true;
						}
					}
					break nodes;
				}
			}
		}
		if (!found && sourceNode != null && targetNode != null) {
			Transition t = new Transition();
			t.setDiagramId(id);
			t.setName(label);
			t.setSource(sourceNode);
			t.setTarget(targetNode);
			sourceNode.getOutTransitions().add(t);
			targetNode.getInTransitions().add(t);
			anyChange = true;
		}
		if (anyChange) {
			int i = 1;
			for (Node node: process.getNodes()) {
				XPathUtils.setValue(this, "/nodes["+i+"]/@outTransitions",
						new LinkedList<>(node.getOutTransitions()));
				XPathUtils.setValue(this, "/nodes["+i+"]/@inTransitions",
						new LinkedList<>(node.getInTransitions()));
				i++;
			}
		}
	}

	private void updateNode(Process process, String id, String type, String label, String parentId) throws Exception {
		int i = 1;
		for (Node node: process.getNodes()) {
			if (node.isToRemove() && node.getDiagramId() != null &&
					node.getDiagramId().equals(id)) {
				node.setToRemove(false);
				XPathUtils.setValue(this, "/nodes["+i+"]/@name", label);
				XPathUtils.setValue(this, "/nodes["+i+"]/@diagramParentId", parentId);
				if (! isCompatible (node.getType(), type)) {
					XPathUtils.setValue(this, "/nodes["+i+"]/@type", findCompatible(type));
//					node.setType(findCompatible(type));
				}
				return;
			}
			i++;
		}
		
		Node node = new Node();
		node.setType(findCompatible(type));
		node.setName(label);
		node.setToRemove(false);
		node.setDiagramId(id);
		
		XPathUtils.createPath(getDataSource(), "/nodes", node);
	}

	private NodeType findCompatible(String type) {
		for (int i = 0; i < allowedSettings.length; i+=2)
			if (type.equals(allowedSettings[i+1]))
				return (NodeType) allowedSettings[i];
		return NodeType.NT_END;
	}

	private boolean isCompatible(NodeType type, String type2) {
		for (int i = 0; i < allowedSettings.length; i+=2)
			if (type == allowedSettings[i] && type2.equals(allowedSettings[i+1]))
				return true;
		return false;
	}

	static Object[] allowedSettings = new Object[] {
			NodeType.NT_ACTION, "node",
			NodeType.NT_APPLY, "apply",
			NodeType.NT_CUSTOM, "decision",
			NodeType.NT_END, "end",
			NodeType.NT_FORK, "fork",
			NodeType.NT_GRANT_SCREEN, "taskgrant",
			NodeType.NT_JOIN, "join",
			NodeType.NT_MAIL, "mail",
			NodeType.NT_MATCH_SCREEN, "taskmatch",
			NodeType.NT_SCREEN, "task",
			NodeType.NT_START, "start",
			NodeType.NT_TIMER, "timer",
			NodeType.NT_SYSTEM_INVOCATION, "system"
	};

	public void changeStepName(Event e) {
		String label = (String) XPathUtils.eval(getFellow("nodes"), "@name");
		String diagramId = (String) XPathUtils.eval(getFellow("nodes"), "@diagramId");
		MxGraph graph = (MxGraph) getFellow("graph");
		graph.changeLabel(diagramId, label);
	}

	public void updateTransition(Transition t) throws Exception {
		String xml = (String) XPathUtils.eval(this, "diagram");
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		NodeList roots = doc.getElementsByTagName("root");
		for (int i = 0; i < roots.getLength(); i++) {
			for (org.w3c.dom.Node n = roots.item(i).getFirstChild(); n != null; n = n.getNextSibling()) {
				if (n instanceof Element) {
					Element e = (Element) n;
					if (t.getDiagramId().equals(e.getAttribute("id")) ) {
						e.setAttribute("label", t.getName());
						Element edge = (Element) e.getFirstChild();
						edge.setAttribute("source", t.getSource().getDiagramId());
						edge.setAttribute("target", t.getTarget().getDiagramId());
					}
				}
			}
		}
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		StringWriter stringWriter = new StringWriter();
		transformer.transform(new DOMSource(doc), new StreamResult(stringWriter));
		String result = stringWriter.toString();
		
		MxGraph graph = (MxGraph) getFellow("graph");
		graph.setModel(result);
	}
	
	public void export(Event event) throws IOException {
		DataNode dn = (DataNode) XPathUtils.eval(this, ".");
		Process p = (Process) dn.getInstance();
		JsonObject json = ProcessSerializer.toJson(p);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Json.createWriter(out).writeObject(json);
		Filedownload.save(out.toByteArray(), "application/octet-stream", p.getName()+".pardef");
	}
	

}
