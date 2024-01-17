package com.soffid.iam.addons.bpm.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

import com.soffid.iam.addons.bpm.common.Constants;
import com.soffid.iam.interp.Evaluator;
import com.soffid.iam.service.impl.bshjail.SecureInterpreter;

import bsh.EvalError;
import bsh.ParseException;
import bsh.TargetError;
import es.caib.bpm.toolkit.exception.SystemWorkflowException;
import es.caib.seycon.ng.exception.InternalErrorException;

public class CustomActionHandler implements ActionHandler {
	String script;
	@Override
	public void execute(ExecutionContext executionContext) throws Exception {
		loadFile(executionContext);
		Map<String, Object> map = new HashMap<>();

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
		try {
			Object o = Evaluator.instance().evaluate(script, map, label);
			if (executionContext.getNode() != null)
			{
				if (o != null && o instanceof String &&
						executionContext.getNode().getLeavingTransitionsMap().get(o.toString()) != null )
				{
					executionContext.leaveNode(o.toString());
				}
				else
					executionContext.leaveNode();
			}
		} catch (ParseException e) {
			throw new SystemWorkflowException("Error parsing custom script "+label+": "+e.getMessage());
		} catch (TargetError e) {
			throw new InternalErrorException ("Error executing custom script "+label+" at "+e.getScriptStackTrace(),
					e.getTarget());
		} catch (EvalError e) {
			String msg;
			try {
				msg = e.getMessage() + "[ "+ e.getErrorText()+"] ";
			} catch (Exception e2) {
				msg = e.getMessage();
			}
			throw new InternalErrorException ("Error parsing custom script "+label+": "+msg);
		}
	}

	public String getScript() {
		return script;
	}
	
	public void setScript(String script) {
		this.script = script;
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
			script = out.toString("UTF-8");
		}
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}
	

}
