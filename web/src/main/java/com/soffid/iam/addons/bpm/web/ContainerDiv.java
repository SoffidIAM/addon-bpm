package com.soffid.iam.addons.bpm.web;

import org.zkoss.zul.Div;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Page;
import org.zkoss.zul.Vbox;

import es.caib.zkib.binder.BindContext;
import es.caib.zkib.binder.SingletonBinder;
import es.caib.zkib.datasource.DataSource;
import es.caib.zkib.events.XPathRerunEvent;
import es.caib.zkib.events.XPathSubscriber;
import es.caib.zkib.jxpath.JXPathContext;
import es.caib.zkib.jxpath.Pointer;

public class ContainerDiv extends Div implements BindContext {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5963706775544268532L;
	SingletonBinder binder = new SingletonBinder(this);

	public ContainerDiv() {
		super();
	}

	public String getDataPath() {
		return binder.getDataPath();
	}

	public DataSource getDataSource() {
		return binder.getDataSource();
	}

	public JXPathContext getJXPathContext() {
		JXPathContext jXPathContext = binder.getJXPathContext();
		Pointer pointer = jXPathContext.getNamespaceContextPointer();
		return binder.getJXPathContext();
	}

	public String getXPath() {
		return binder.getXPath();
	}

	public void setDataPath(String bind) {
		DataSource ds = binder.getDataSource();
		String path = binder.getXPath();

		binder.setDataPath(bind);

		if (ds != null) {
			binder.getDataSource().sendEvent(new XPathRerunEvent(ds, path));
		}

		if (binder.getDataSource() != null) {
			binder.getDataSource().sendEvent(new XPathRerunEvent(getDataSource(), getXPath()));
		}
	}

	public Object getValue() {
		return binder.getValue();
	}

	public void setPage(Page page) {
		super.setPage(page);
		binder.setPage(page);
	}

	public void setParent(Component parent) {
		super.setParent(parent);
		binder.setParent(parent);
	}

	public Object clone() {
		ContainerDiv clone = (ContainerDiv) super.clone();
		clone.binder = new SingletonBinder(clone);
		clone.binder.setDataPath(binder.getDataPath());
		return clone;
	}

}
