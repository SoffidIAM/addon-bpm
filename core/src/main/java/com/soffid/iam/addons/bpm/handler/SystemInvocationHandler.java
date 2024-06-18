package com.soffid.iam.addons.bpm.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.jpdl.el.VariableResolver;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.json.JSONArray;
import org.json.JSONObject;

import com.soffid.iam.ServiceLocator;
import com.soffid.iam.addons.bpm.common.Constants;
import com.soffid.iam.interp.Evaluator;
import com.soffid.iam.service.impl.bshjail.SecureInterpreter;
import com.soffid.iam.utils.ConfigurationCache;

import bsh.EvalError;
import bsh.ParseException;
import bsh.TargetError;
import es.caib.bpm.toolkit.exception.SystemWorkflowException;
import es.caib.seycon.ng.exception.InternalErrorException;

public class SystemInvocationHandler implements ActionHandler {
	String system;
	String verb;
	String path;
	String maps;
	String returnVar;
	@Override
	public void execute(ExecutionContext executionContext) throws Exception {
		loadFile(executionContext);
		Map<String, Object> map = new HashMap<>();

		if (executionContext.getContextInstance().getVariables() != null) {
			for (Object var: executionContext.getContextInstance().getVariables().keySet()) {
				map.put((String) var, executionContext.getVariable((String) var));
			}
		}
		map.put("executionContext", executionContext); //$NON-NLS-1$
		map.put("serviceLocator", com.soffid.iam.ServiceLocator.instance()); //$NON-NLS-1$
		map.put(Constants.ATTRIBUTES_VAR, executionContext.getVariable(Constants.ATTRIBUTES_VAR)); //$NON-NLS-1$
		map.put(Constants.REQUESTER_NAME_VAR, executionContext.getVariable(Constants.REQUESTER_NAME_VAR)); //$NON-NLS-1$
		map.put(Constants.REQUESTER_VAR, executionContext.getVariable(Constants.REQUESTER_VAR)); //$NON-NLS-1$
		map.put(Constants.USER_VAR, executionContext.getVariable(Constants.USER_VAR)); //$NON-NLS-1$
		map.put(Constants.ROLES_VAR, executionContext.getVariable(Constants.ROLES_VAR)); //$NON-NLS-1$
				
		String label = executionContext.getNode() != null ? executionContext.getNode().getName():
					executionContext.getTransition() != null ? executionContext.getTransition().getName():
				"";
		HashMap m  = new HashMap<>();
		JSONObject j = new JSONObject(maps);
		for (String key: j.keySet()) {
			String value = j.optString(key, null);
			if (value != null) {
				m.put(key, Evaluator.instance().evaluate(value, map, "Attribute "+key));
			}
		}
		VariableResolver variableResolver = JbpmExpressionEvaluator
				.getUsedVariableResolver();
		Object path2 = JbpmExpressionEvaluator.evaluate(path,
				executionContext, variableResolver,
				JbpmExpressionEvaluator.getUsedFunctionMapper());

		Collection r = ServiceLocator.instance().getDispatcherService()
			.invoke(system, verb, path2 == null ? null: path2.toString(), m);
		if (returnVar != null)
			executionContext.setVariable(returnVar, r);
		executionContext.leaveNode();
	}

	String file; 
	private void loadFile(ExecutionContext executionContext) throws InternalErrorException, IOException {
		if (file != null) {
			InputStream in = executionContext.getProcessDefinition().getFileDefinition().getInputStream(file);
			if (in == null)
				throw new InternalErrorException("Cannot find resource "+file);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			for (int read = in.read(); read >= 0; read = in.read())
				out.write(read);
			in.close();
			out.close();
			maps = out.toString("UTF-8");
		}
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String getSystem() {
		return system;
	}

	public void setSystem(String system) {
		this.system = system;
	}

	public String getVerb() {
		return verb;
	}

	public void setVerb(String verb) {
		this.verb = verb;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getMaps() {
		return maps;
	}

	public void setMaps(String maps) {
		this.maps = maps;
	}

	public String getReturnVar() {
		return returnVar;
	}

	public void setReturnVar(String returnVar) {
		this.returnVar = returnVar;
	}
	

}
