package com.soffid.iam.addons.bpm.web;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Row;
import org.zkoss.zul.Window;

import es.caib.zkib.binder.BindContext;
import es.caib.zkib.component.DataListbox;
import es.caib.zkib.component.DataModel;
import es.caib.zkib.datasource.XPathUtils;
import es.caib.zkib.zkiblaf.Missatgebox;

public class ProcessListbox extends DataListbox {
	private BindContext ctxToRemove;

	public ProcessListbox () {
	}

	public void onEdit(Event event) {
		Component c = event.getTarget();
		while ( c != null)
		{
			if ( c instanceof Listitem)
			{
				setSelectedItem((Listitem) c);
				Window processEditor = (Window) getParent().getFellow("editor").getFellow("w");
				processEditor.doHighlighted();
			}
			c = c.getParent();
		}
	}
	
	public void onRemove(Event event) {
		ctxToRemove = XPathUtils.getComponentContext(event.getTarget());
		String name = (String) XPathUtils.getValue(event.getTarget(), "name");
		Missatgebox.confirmaOK_CANCEL( String.format("Please, confirm to remove business process definition: %s", name), onConfirmRemove);
	}
	
	private EventListener onConfirmRemove = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			if (event.getData().equals( new Integer ( Missatgebox.OK)))
			{
				XPathUtils.removePath(ctxToRemove.getDataSource(), ctxToRemove.getXPath());
				getDataSource().commit();
			}
		}
	};

}
