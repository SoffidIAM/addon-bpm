package com.soffid.iam.addons.bpm.web;

import javax.servlet.http.HttpServletRequest;

import org.zkoss.zk.ui.Executions;

import com.soffid.iam.web.component.FrameHandler;

import es.caib.seycon.ng.exception.InternalErrorException;

public class AnonymousAction extends FrameHandler {

	public AnonymousAction() throws InternalErrorException {
		super();
	}

	@Override
	public void afterCompose() {
		HttpServletRequest req = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
		String key = req.getParameter("key");

	}
	
}
