package com.soffid.iam.addons.bpm.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Window;

import com.soffid.iam.addons.bpm.common.Process;

import es.caib.zkib.binder.BindContext;
import es.caib.zkib.component.DataListbox;
import es.caib.zkib.datamodel.DataNode;
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
	
	public void export(Event event) throws IOException {
		ctxToRemove = XPathUtils.getComponentContext(event.getTarget());
		DataNode dn = (DataNode) XPathUtils.getValue(event.getTarget(), ".");
		Process p = (Process) dn.getInstance();
		JsonObject json = ProcessSerializer.toJson(p);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Json.createWriter(out).writeObject(json);
		Filedownload.save(out.toByteArray(), "application/octet-stream", p.getName()+".pardef");
	}
	
	public void importProcess() throws Exception {
		Media media = Fileupload.get();
		JsonReader reader = media.getByteData() != null ? Json.createReader( new ByteArrayInputStream( media.getByteData()) ):
			 media.getStreamData() != null ? Json.createReader( media.getStreamData() ):
			 media.getReaderData() != null ? Json.createReader( media.getReaderData() ):
				Json.createReader( new StringReader( media.getStringData()) );
			 
		JsonObject object = reader.readObject();
		reader.close();
		Process p = ProcessSerializer.processFromJson(object);
		String path = XPathUtils.createPath(getDataSource(), "/process", p);
		setSelectedItem( getItemAtIndex(getItemCount()-1) );
		Window processEditor = (Window) getParent().getFellow("editor").getFellow("w");
		processEditor.doHighlighted();
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
