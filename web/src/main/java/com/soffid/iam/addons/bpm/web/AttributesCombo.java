package com.soffid.iam.addons.bpm.web;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Row;

import com.soffid.iam.EJBLocator;
import com.soffid.iam.addons.bpm.common.Attribute;
import com.soffid.iam.addons.bpm.common.Process;
import com.soffid.iam.api.DataType;
import com.soffid.iam.api.MetadataScope;

import es.caib.seycon.ng.exception.InternalErrorException;
import es.caib.zkib.binder.BindContext;
import es.caib.zkib.component.DataCombobox;
import es.caib.zkib.datamodel.DataNode;
import es.caib.zkib.datasource.XPathUtils;

public class AttributesCombo extends DataCombobox {
	public static EventListener onFocus = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			AttributesCombo combo = (AttributesCombo) event.getTarget();
			BindContext bc = XPathUtils.getComponentContext(combo.getFellow("w"));
			DataNode processNode = (DataNode) XPathUtils.getValue(bc.getDataSource(), ".");
			Process p = (Process) processNode.getInstance();
			combo.fill ( p );
		}
	};
	
	public static EventListener onChange = new EventListener() {
		@Override
		public void onEvent(Event event) throws Exception {
			AttributesCombo c = (AttributesCombo) event.getTarget();
			ProcessWindow w = (ProcessWindow) c.getFellow("w");
			w.addField((Row) c.getParent());
		}
	};
	
	public AttributesCombo() {
		addEventListener("onFocus", onFocus);
		addEventListener("onChange", onChange);
	}

	protected void fill(Process p) throws InternalErrorException, NamingException, CreateException {
		getItems().clear();
		LinkedList<Attribute> attributes = new LinkedList<Attribute>( p.getAttributes() );
		Collections.sort(attributes, new Comparator<Attribute>() {
			@Override
			public int compare(Attribute o1, Attribute o2) {
				if (o1.getOrder() == null && o2.getOrder() == null)
					return 0;
				else if (o1.getOrder() == null)
					return 1;
				else if (o2.getOrder() == null)
					return -1;
				else
					return o1.getOrder().compareTo(o2.getOrder());
			}
		});
		for (Attribute attribute: attributes)
		{
			Comboitem item = new Comboitem(attribute.getName());
			getItems().add(item);
			item.setValue(item.getLabel());
		}
		
		addStandardAttributes();
	}

	
	private void addStandardAttributes() throws InternalErrorException, NamingException, CreateException {
		for (DataType s: EJBLocator.getAdditionalDataService().findDataTypes2(MetadataScope.USER))
		{
			Comboitem item = new Comboitem(s.getCode());
			item.setValue(item.getLabel());
			getItems().add(item);
		}
	}
}
