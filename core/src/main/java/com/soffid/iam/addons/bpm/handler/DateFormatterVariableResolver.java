package com.soffid.iam.addons.bpm.handler;

import java.util.Calendar;
import java.util.Date;

import org.jbpm.jpdl.el.ELException;
import org.jbpm.jpdl.el.VariableResolver;
import org.jbpm.jpdl.el.impl.JbpmVariableResolver;

import com.ibm.icu.text.SimpleDateFormat;

public class DateFormatterVariableResolver extends JbpmVariableResolver implements VariableResolver {

	@Override
	public Object resolveVariable(String name) throws ELException {
		Object o = super.resolveVariable(name);
		if (o == null)
			return null;
		if (o instanceof Date) {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH.mm.ss"); //$NON-NLS-1$
			return df.format((Date)o);
		}
		if (o instanceof Calendar) {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH.mm.ss"); //$NON-NLS-1$
			return df.format(((Calendar)o).getTime());
		}
		return o;
	}

}
