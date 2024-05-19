package com.soffid.iam.addons.bpm.web;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.http.HttpServletRequest;

import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Window;

import com.soffid.addons.bpm.web.mxgraph.MxGraph;
import com.soffid.iam.addons.bpm.common.Node;
import com.soffid.iam.addons.bpm.common.Process;
import com.soffid.iam.addons.bpm.common.WorkflowType;
import com.soffid.iam.web.component.FrameHandler;
import com.soffid.iam.web.popup.FileUpload2;

import es.caib.seycon.ng.exception.InternalErrorException;
import es.caib.zkib.component.DataTable;
import es.caib.zkib.datamodel.DataModelCollection;
import es.caib.zkib.datamodel.DataNode;
import es.caib.zkib.datamodel.DataNodeCollection;
import es.caib.zkib.datasource.DataSource;
import es.caib.zkib.datasource.XPathUtils;
import es.caib.zkib.jxpath.JXPathException;
import es.caib.zkib.zkiblaf.Missatgebox;

public class EditorHandler extends FrameHandler {

	public EditorHandler() throws InternalErrorException {
		super();
	}

	@Override
	public void afterCompose() {
		super.afterCompose();
		HttpServletRequest req = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
		String wizard = req.getParameter("wizard");
		if ("new".equals(wizard)) {
			try {
				NewProcessWindow w = (NewProcessWindow) getFellow("newProcessWindow");
				w.createSampleProcesses();			
				DataNodeCollection coll = (DataNodeCollection) getModel().getValue("/process");
				coll.refresh();
			} catch (Exception e) {
				throw new UiException(e);
			}
		}
	}

	public void importProcess() throws Exception {
		FileUpload2.get(
			event -> {
				Media media = ( (UploadEvent) event).getMedia();
				JsonReader reader = media.isBinary() && media.inMemory() ? Json.createReader( new ByteArrayInputStream( media.getByteData()) ):
					media.isBinary() ? Json.createReader( media.getStreamData() ):
					media.inMemory() ? Json.createReader( new StringReader( media.getStringData()) ):
							Json.createReader( media.getReaderData() );
				
				JsonObject object = reader.readObject();
				reader.close();
				final Process p = ProcessSerializer.processFromJson(object);
				final String name = p.getName();
				final List<Process> old = com.soffid.iam.addons.bpm.common.EJBLocator.getBpmEditorService().findByName(name);
				if ( old == null || old.isEmpty() ) {
					openProcessWindow(p);			
				} else {
					Missatgebox.confirmaYES_NO(String.format("The process %s already exists. Do you want to overwrite it?", name), 
							new EventListener() {
						public void onEvent(Event ev) throws Exception {
							if (ev.getData().equals( Missatgebox.YES ))
							{
								DataModelCollection c = (DataModelCollection) getModel().getValue("/process");
								for ( int i = 0; i < c.getSize(); i++)
								{
									DataNode node = (DataNode) c.getDataModel(i);
									if (node != null && !node.isDeleted()) {
										String cellName = (String) node.get("name");
										if (cellName != null && cellName.equals(name)) {
											node.setTransient(true);
											node.delete();
										}
									}
								}
								p.setId(old.iterator().next().getId());
								openProcessWindow(p);
							}
						}
					});
				}
			}
		);
	}

	public void openProcessWindow(Process p) throws Exception {
		String path = XPathUtils.createPath(getDataSource(), "/process", p);
		DataTable dt = (DataTable) getListbox();
		ListModel model = dt.getModel();
		dt.setSelectedIndex(model.getSize() - 1);
		showDetails();
	}

	private DataSource getDataSource() {
		return getModel();
	}

}
