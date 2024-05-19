package com.soffid.addons.bpm.web.mxgraph;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EventListener;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.zkoss.xml.HTMLs;
import org.zkoss.zk.au.AuRequest;
import org.zkoss.zk.au.Command;
import org.zkoss.zk.au.out.AuInvoke;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.impl.XulElement;

import es.caib.zkib.component.Div;

public class MxGraph extends XulElement {
	String config;
	String model;
	Document doc = null;
	String selected;
	byte[] image;
	EventListener imageListener;
	
	public MxGraph() {
		super();
		setSclass("mxgraph");
	}

	static protected Command onChangeCommand = new Command("onChange2", 0) {
		protected void process(AuRequest r) {
			MxGraph g = (MxGraph) r.getComponent();
			g.model = r.getData()[0];
			try {
				g.doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
						new ByteArrayInputStream(g.model.getBytes(StandardCharsets.UTF_8)));
			} catch (Exception e) {
				throw new UiException("Error parsing model", e);
			}
			Events.postEvent(Events.ON_CHANGE, g, g.model);
		}
	};

	static protected Command onSelectCommand = new Command("onSelect2", 0) {
		protected void process(AuRequest r) {
			MxGraph g = (MxGraph) r.getComponent();
			if (r.getData() == null || r.getData().length == 0)
				g.selected = null;
			else
				g.selected = r.getData()[0];
			Events.postEvent(Events.ON_SELECT, g, g.selected);
		}
	};

	static protected Command onImageCommand = new Command("onImage", 0) {
		protected void process(AuRequest r) {
			MxGraph g = (MxGraph) r.getComponent();
			String image = r.getData()[0];
			image = image.substring(image.indexOf(",")+1);
			g.image = Base64.getDecoder().decode(image.getBytes(StandardCharsets.UTF_8));
			Events.postEvent("onImage", g, g.image);
		}
	};


	@Override
	public String getInnerAttrs() {
		StringBuffer sb = new StringBuffer();
		HTMLs.appendAttribute(sb, "z.config", Executions.getCurrent().encodeURL(config));
		HTMLs.appendAttribute(sb, "z.model", model);
		sb.append(super.getInnerAttrs());
		return sb.toString();
	}

	public String getConfig() {
		return config;
	}

	public void setConfig(String config) {
		if (config != null && !config.equals(this.config)) {
			this.config = config;
			invalidate();
		}
	}

	@Override
	public Command getCommand(String cmdId) {
		if (Events.ON_CHANGE.equals(cmdId))
			return onChangeCommand;
		if (Events.ON_SELECT.equals(cmdId))
			return onSelectCommand;
		if (onImageCommand.getId().equals(cmdId))
			return onImageCommand;
		return super.getCommand(cmdId);
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
					new ByteArrayInputStream(model.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			throw new UiException("Error parsing model", e);
		}
		smartUpdate("z.model", model);
		selected = null;
	}

	public Document getDoc() {
		return doc;
	}

	public String getSelected() {
		return selected;
	}

	public byte[] getImage() {
		return image;
	}
	
	public void getImage(EventListener l) {
		this.imageListener = l;
		response("updateImg",new AuInvoke(this, "updateImg"));
	}
	
	public void changeLabel(String id, String label) {
		response(null, new AuInvoke(this, "updateLabel", id, label));
	}
}
