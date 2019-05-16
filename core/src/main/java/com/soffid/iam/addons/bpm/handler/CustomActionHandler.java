package com.soffid.iam.addons.bpm.handler;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

import com.soffid.iam.addons.bpm.common.Constants;
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
		SecureInterpreter interpreter = new SecureInterpreter();

		interpreter.set("executionContext", executionContext); //$NON-NLS-1$
		interpreter.set("serviceLocator", com.soffid.iam.ServiceLocator.instance()); //$NON-NLS-1$
		interpreter.set(Constants.ATTRIBUTES_VAR, executionContext.getVariable(Constants.ATTRIBUTES_VAR)); //$NON-NLS-1$
		interpreter.set(Constants.REQUESTER_NAME_VAR, executionContext.getVariable(Constants.REQUESTER_NAME_VAR)); //$NON-NLS-1$
		interpreter.set(Constants.REQUESTER_VAR, executionContext.getVariable(Constants.REQUESTER_VAR)); //$NON-NLS-1$
		interpreter.set(Constants.USER_VAR, executionContext.getVariable(Constants.USER_VAR)); //$NON-NLS-1$
		interpreter.set(Constants.ROLES_VAR, executionContext.getVariable(Constants.ROLES_VAR)); //$NON-NLS-1$
				
		try {
			Object o = interpreter.eval(script);
			if (o != null && o instanceof String &&
					executionContext.getNode().getLeavingTransitionsMap().get(o.toString()) != null )
				{
					executionContext.leaveNode(o.toString());
				}
				else
					executionContext.leaveNode();
		} catch (ParseException e) {
			throw new SystemWorkflowException("Error parsing custom script "+executionContext.getNode().getName()+": "+e.getMessage());
		} catch (TargetError e) {
			throw new InternalErrorException ("Error executing custom script "+executionContext.getNode().getName()+" at "+e.getScriptStackTrace(),
					e.getTarget());
		} catch (EvalError e) {
			String msg;
			try {
				msg = e.getMessage() + "[ "+ e.getErrorText()+"] ";
			} catch (Exception e2) {
				msg = e.getMessage();
			}
			throw new InternalErrorException ("Error parsing custom script "+executionContext.getNode().getName()+": "+msg);
		}
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

}
