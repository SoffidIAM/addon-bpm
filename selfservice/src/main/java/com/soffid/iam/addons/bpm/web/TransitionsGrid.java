package com.soffid.iam.addons.bpm.web;

import java.util.Iterator;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Row;

import com.soffid.iam.addons.bpm.common.Node;
import com.soffid.iam.addons.bpm.common.Transition;

import es.caib.zkib.binder.BindContext;
import es.caib.zkib.component.DataGrid;
import es.caib.zkib.component.DataListcell;
import es.caib.zkib.datasource.JXPContext;
import es.caib.zkib.datasource.XPathUtils;
import es.caib.zkib.events.XPathRerunEvent;
import es.caib.zkib.jxpath.Pointer;
import es.caib.zkib.zkiblaf.ImageClic;

public class TransitionsGrid extends DataGrid {
	public TransitionsGrid () {
		addEventListener("onNewRow", onNeRow);
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


	EventListener onNeRow = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			Row r = (Row) event.getData();
			Listbox sourceLB = (Listbox) r.getChildren().get(0);
			initListbox (sourceLB, "source");
			Listbox targetLB = (Listbox) r.getChildren().get(2);
			initListbox (targetLB, "target");
			ImageClic img = (ImageClic) ((Component)r.getChildren().get(3)).getChildren().get(1);
			img.addEventListener("onClick", onEditScript);
		}
	};

	EventListener onSelect = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			Listbox listbox = (Listbox) event.getTarget();
			if (listbox.getSelectedItem() != null){
				Node selected = (Node) listbox.getSelectedItem().getValue();
				if (selected != null)
				{
					String type = (String) listbox.getAttribute("listboxType");
					Transition t = (Transition) XPathUtils.getValue( listbox.getParent(), ".");
					List<Node> nodes = (List<Node>) XPathUtils.getValue( getFellow("w"), "/nodes");
					if (type.equals("source"))
					{
						Node previous = t.getSource();
						t.setSource(selected);
						if (previous != null && previous != selected)
							previous.getOutTransitions().remove(t);
						if (previous != selected)
							selected.getOutTransitions().add(t);
					}
					else
					{
						Node previous = t.getTarget();
						t.setTarget(selected);
						if (previous != null && previous != selected)
							previous.getInTransitions().remove(t);
						if (previous != selected)
							selected.getInTransitions().add(t);
					}
					BindContext ctx = XPathUtils.getComponentContext(getParent());
					ctx.getDataSource().sendEvent( new XPathRerunEvent( ctx.getDataSource(), ctx.getXPath()));
				}
			}
		}
	};

	private void initListbox(Listbox listbox, String path) {
		BindContext ctx = XPathUtils.getComponentContext(getFellow("w"));
		
//		List<Node> nodes = (List<Node>) XPathUtils.getValue( getFellow("w"), "/nodes");
		Transition t = (Transition) XPathUtils.getValue( listbox.getParent(), ".");
		try {
			Node value = (Node) XPathUtils.getValue( listbox.getParent(), path);
			listbox.getItems().clear();
			for (Iterator<Pointer> it = ctx.getDataSource().getJXPathContext().iteratePointers("/nodes"); it.hasNext(); )
			{
				Pointer p = it.next();
				Listitem item = new Listitem();
				Object node = p.getValue();
				item.setValue(node);
				DataListcell cell = new DataListcell();
				cell.setBind( "/processListbox:"+ p.asPath()+"/@name");
				item.appendChild(cell);
				listbox.getItems().add(item);
				if (node == value)
					listbox.setSelectedItem(item);
			}
		} catch (Exception e) {
			// Ignore exception
		}
		listbox.setAttribute("listboxType", path);
		listbox.addEventListener("onSelect", onSelect);
	}
}
