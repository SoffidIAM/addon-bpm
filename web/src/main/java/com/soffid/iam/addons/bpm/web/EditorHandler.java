package com.soffid.iam.addons.bpm.web;

import javax.servlet.http.HttpServletRequest;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.UiException;

import com.soffid.iam.addons.bpm.common.Process;
import com.soffid.iam.addons.bpm.common.WorkflowType;
import com.soffid.iam.web.component.FrameHandler;

import es.caib.seycon.ng.exception.InternalErrorException;
import es.caib.zkib.datamodel.DataNodeCollection;

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

	@Override
	protected Component getListbox() {
		return getFellow("processListbox");
	}


}
