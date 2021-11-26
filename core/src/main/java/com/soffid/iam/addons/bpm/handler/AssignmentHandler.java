package com.soffid.iam.addons.bpm.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.taskmgmt.exe.Assignable;

import com.soffid.iam.addons.bpm.common.Constants;
import com.soffid.iam.service.impl.bshjail.SecureInterpreter;

import bsh.EvalError;
import bsh.ParseException;
import bsh.TargetError;
import es.caib.bpm.toolkit.exception.SystemWorkflowException;
import es.caib.seycon.ng.exception.InternalErrorException;

public class AssignmentHandler implements org.jbpm.taskmgmt.def.AssignmentHandler {
	String script;
	
	@Override
	public void assign(Assignable assignable, ExecutionContext executionContext) throws Exception {
		loadFile(executionContext);
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
			if ( o == null)
				throw new JbpmException("Script for task assignment returned null: "+script);
			if (o instanceof String)
			{
				assignable.setPooledActors(new String[] {o.toString()});
			}
			else if (o instanceof String[]) 
			{
				assignable.setPooledActors((String[]) o);
			}
			else if (o instanceof List)
			{
				assignable.setPooledActors(  (String[]) ((List) o).toArray(new String[0]));
			}
			else
			{
				throw new JbpmException("Script for task assignment wrong object "+o+": "+script);
			}
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
