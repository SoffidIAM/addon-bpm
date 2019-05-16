package com.soffid.iam.addons.bpm.handler;

import java.util.Map;

import org.jbpm.jpdl.el.ELException;
import org.jbpm.jpdl.el.VariableResolver;
import org.jbpm.jpdl.el.impl.JbpmVariableResolver;

import com.soffid.iam.api.Application;
import com.soffid.iam.api.Role;

public class RoleVariableResolver extends JbpmVariableResolver {
	private static final long serialVersionUID = 1L;
	VariableResolver variableResolver = null;
	private Role role;
	private Application application;

	public RoleVariableResolver(
			Role role,
			Application application,
			VariableResolver variableResolver) {
		this.variableResolver = variableResolver;
		this.role = role;
		this.application = application;
	}

	public Object resolveVariable(String pName) throws ELException {
		if (pName.equals("role"))
			return role;
		else if (pName.equals("application"))
			return application;
		else if (variableResolver == null)
			return null;
		else
			return variableResolver.resolveVariable(pName);
	}
}
