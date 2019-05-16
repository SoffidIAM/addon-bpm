package com.soffid.iam.addons.bpm.core;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;

import com.soffid.iam.addons.bpm.common.Attribute;
import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.Node;
import com.soffid.iam.addons.bpm.common.NodeType;
import com.soffid.iam.addons.bpm.common.PageInfo;
import com.soffid.iam.bpm.api.ProcessInstance;
import com.soffid.iam.bpm.api.TaskInstance;

public class BpmUserServiceImpl extends BpmUserServiceBase {

	@Override
	protected PageInfo handleGetPageInfo(ProcessInstance proc) throws Exception {
		JbpmContext ctx = getBpmEngine().getContext();
		try {
			org.jbpm.graph.exe.ProcessInstance process = ctx.loadProcessInstance(proc.getId());
			ProcessDefinition def = process.getProcessDefinition();
			InputStream in = def.getFileDefinition().getInputStream("task#start");
			if (in == null)
			{
				PageInfo pi = new PageInfo();
				pi.setFields(new Field[0]);
				pi.setAttributes(new Attribute[0]);
				return pi;
			}
			ObjectInputStream o = new ObjectInputStream(in);
			PageInfo pi = (PageInfo) o.readObject();
			
			return pi;
		} finally {
			ctx.close();
		}
	}

	@Override
	protected PageInfo handleGetPageInfo(TaskInstance task) throws Exception {
		JbpmContext ctx = getBpmEngine().getContext();
		try {
			InputStream in;
			if (task.isDummyTask())
			{
				ProcessDefinition def = ctx.getGraphSession().getProcessDefinition( task.getProcessDefinition() ) ;
				in = def.getFileDefinition().getInputStream("task#start");
			}
			else
			{
				org.jbpm.taskmgmt.exe.TaskInstance ti = ctx.getTaskInstance(task.getId());
				long nodeId = ti.getToken().getNode().getId();
				org.jbpm.graph.exe.ProcessInstance process = ti.getToken().getProcessInstance();
				ProcessDefinition def = process.getProcessDefinition();
				in = def.getFileDefinition().getInputStream("task#"+nodeId);
			}
			if (in == null)
			{
				PageInfo pi = new PageInfo();
				pi.setFields(new Field[0]);
				pi.setAttributes(new Attribute[0]);
				return pi;
			}
			ObjectInputStream o = new ObjectInputStream(in);
			PageInfo pi = (PageInfo) o.readObject();
			
			return pi;
		} finally {
			ctx.close();
		}
	}

}
