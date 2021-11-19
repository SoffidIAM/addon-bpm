package com.soffid.iam.addons.bpm.core;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jbpm.JbpmContext;
import org.jbpm.file.def.FileDefinition;

import com.soffid.iam.addons.bpm.common.Attribute;
import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.Node;
import com.soffid.iam.addons.bpm.common.NodeType;
import com.soffid.iam.addons.bpm.common.Process;
import com.soffid.iam.addons.bpm.common.Transition;
import com.soffid.iam.addons.bpm.common.Trigger;
import com.soffid.iam.addons.bpm.model.AttributeEntity;
import com.soffid.iam.addons.bpm.model.FieldEntity;
import com.soffid.iam.addons.bpm.model.NodeEntity;
import com.soffid.iam.addons.bpm.model.ProcessEntity;
import com.soffid.iam.addons.bpm.model.TransitionEntity;
import com.soffid.iam.addons.bpm.model.TriggerEntity;
import com.soffid.iam.bpm.api.TaskInstance;

import es.caib.seycon.ng.comu.TypeEnumeration;
import es.caib.seycon.ng.exception.InternalErrorException;

public class BpmEditorServiceImpl extends BpmEditorServiceBase {

	@Override
	protected Process handleCreate(Process process) throws Exception {
		ProcessEntity pe =  getProcessEntityDao().processToEntity(process);
		getProcessEntityDao().create(pe);
		
		updateProcess (pe, process);
		
		getProcessEntityDao().toProcess(pe, process);
		return  process;
	}

	private void updateProcess(ProcessEntity pe, Process process) throws InternalErrorException {
		for ( NodeEntity nodeEntity: pe.getNodes())
		{
			getTransitionEntityDao().remove(nodeEntity.getOutTransitions());
			getTransitionEntityDao().remove(nodeEntity.getInTransitions());
			nodeEntity.getOutTransitions().clear();
			nodeEntity.getInTransitions().clear();
			getFieldEntityDao().remove(nodeEntity.getFields());
			getTriggerEntityDao().remove(nodeEntity.getTriggers());
		}

		getNodeEntityDao().remove(pe.getNodes());
		pe.getNodes().clear();
		
		HashMap<Long, NodeEntity> nodes = new HashMap<Long, NodeEntity>();
		for ( Node node: process.getNodes())
		{
			NodeEntity nodeEntity = getNodeEntityDao().nodeToEntity(node);
			nodeEntity.setProcess(pe);
			getNodeEntityDao().create(nodeEntity);
			
			pe.getNodes().add(nodeEntity);
			
			nodes.put(nodeEntity.getId(), nodeEntity);
			
			node.setId(nodeEntity.getId());
			
			for (Field f: node.getFields())
			{
				FieldEntity fieldEntity = getFieldEntityDao().fieldToEntity(f);
				fieldEntity.setNode(nodeEntity);
				getFieldEntityDao().create(fieldEntity);
				f.setId(fieldEntity.getId());
				nodeEntity.getFields().add(fieldEntity);
			}
			
			for (Trigger t: node.getTriggers())
			{
				TriggerEntity triggerEntity = getTriggerEntityDao().triggerToEntity(t);
				triggerEntity.setNode(nodeEntity);
				getTriggerEntityDao().create(triggerEntity);
				t.setId(triggerEntity.getId());
				nodeEntity.getTriggers().add(triggerEntity);
			}
			
		}
		
		for ( Node node: process.getNodes())
		{
			for (Transition transition: node.getOutTransitions())
			{
				TransitionEntity transitionEntity = getTransitionEntityDao().transitionToEntity(transition);
				transitionEntity.setSource( nodes.get(node.getId()) );
				transitionEntity.setTarget( nodes.get( transition.getTarget().getId() ) );
				
				if (transitionEntity.getTarget() == null)
					throw new InternalErrorException(String.format("Cannot store process definition. Transition %s from step %s is missing target step",
							transition.getName(), node.getName()));
				getTransitionEntityDao().create(transitionEntity);
				transitionEntity.getSource().getOutTransitions().add(transitionEntity);
				transitionEntity.getTarget().getInTransitions().add(transitionEntity);
			}
		}
		
		getAttributeEntityDao().remove( pe.getAttributes() );
		pe.getAttributes().clear();
		for ( Attribute a: process.getAttributes())
		{
			AttributeEntity attEntity = getAttributeEntityDao().attributeToEntity(a);
			attEntity.setProcess(pe);
			getAttributeEntityDao().create(attEntity);
			pe.getAttributes().add(attEntity);
		}

	}

	@Override
	protected Process handleUpdate(Process process) throws Exception {
		ProcessEntity pe =  getProcessEntityDao().processToEntity(process);
		
		updateProcess (pe, process);
		
		getProcessEntityDao().update(pe);

//		getProcessEntityDao().toProcess(pe, process);
		return  process;
	}

	@Override
	protected List<Process> handleFindAll() throws Exception {
		List<Process> list = new LinkedList<Process>();
		
		for (ProcessEntity pe: getProcessEntityDao().loadAll())
		{
			list.add(toProcess (pe));
		}
		return list ;
	}

	private Process toProcess(ProcessEntity pe) {
		Process proc = getProcessEntityDao().toProcess(pe);
		Map<Long,Node> nodes = new HashMap<Long, Node>();
		Collection<Node> nodesList = new LinkedList<Node>();
		Node current = null;
		nodesList.addAll(proc.getNodes());
		proc.getNodes().clear();
		while ( ! nodesList.isEmpty())
		{
			Set<Node> next = new HashSet<Node>();
			if (current != null)
			{
				for (Transition t: current.getOutTransitions() )
				{
					if ( t.getTarget() != null && t.getTarget() != current && nodesList.contains(t.getTarget()))
					{
						next.add(t.getTarget());
					}
				}
			}
			
			if ( next.isEmpty())
			{
				current = findStart (nodesList);
			} 
			else if (next.size() == 1)
			{
				current = next.iterator().next();
			}
			else
			{
				current = LongestPathFinder.find (current, next, nodesList);
			}
			nodesList.remove(current);
			proc.getNodes().add(current);
		}
		
		boolean containsAction = false;
		boolean containsOldUser = false;
		boolean containsGrants = false;
		proc.setAttributes( new LinkedList<Attribute>());
		for (AttributeEntity attEntity: pe.getAttributes())
		{
			Attribute att = getAttributeEntityDao().toAttribute(attEntity);
			proc.getAttributes().add(att);
			if (att.getName().equals("action"))
				containsAction = true;
			if (att.getName().equals("userSelector"))
				containsOldUser = true;
			if (att.getName().equals("grants"))
				containsGrants = true;
		}
		if ( ! containsAction)
		{
			AttributeEntity attEntity = getAttributeEntityDao().newAttributeEntity();
			attEntity.setProcess(pe);
			pe.getAttributes().add(attEntity);
			attEntity.setLabel("Action");
			attEntity.setName("action");
			attEntity.setType(TypeEnumeration.STRING_TYPE);
			attEntity.setOrder(0L);
			attEntity.setValues("A:+Add+user E:+Enable+user M:+Modify+user D:+Disable+user");
			getAttributeEntityDao().create(attEntity);
			Attribute att = getAttributeEntityDao().toAttribute(attEntity);
			proc.getAttributes().add(0, att);
		}
		if ( ! containsOldUser)
		{
			AttributeEntity attEntity = getAttributeEntityDao().newAttributeEntity();
			attEntity.setProcess(pe);
			pe.getAttributes().add(attEntity);
			attEntity.setLabel("Select user");
			attEntity.setName("userSelector");
			attEntity.setType(TypeEnumeration.USER_TYPE);
			attEntity.setOrder(0L);
			getAttributeEntityDao().create(attEntity);
			Attribute att = getAttributeEntityDao().toAttribute(attEntity);
			proc.getAttributes().add(1, att);
		}
		if ( ! containsGrants)
		{
			AttributeEntity attEntity = getAttributeEntityDao().newAttributeEntity();
			attEntity.setProcess(pe);
			pe.getAttributes().add(attEntity);
			attEntity.setLabel("Permissions");
			attEntity.setName("grants");
			attEntity.setType(TypeEnumeration.STRING_TYPE);
			attEntity.setOrder(2L);
			getAttributeEntityDao().create(attEntity);
			Attribute att = getAttributeEntityDao().toAttribute(attEntity);
			proc.getAttributes().add(2, att);
		}
		Collections.sort(proc.getAttributes(), new Comparator<Attribute>() {
			@Override
			public int compare(Attribute o1, Attribute o2) {
				return o1.getName().compareTo(o2.getName());
			}
			
		});
		return proc;
	}

	private Node findStart(Collection<Node> nodesList) {
		for (Node n: nodesList)
		{
			if (n.getType().equals( NodeType.NT_START ))
				return n;
		}
		for (Node n: nodesList)
		{
			if (! n.getType().equals( NodeType.NT_END ))
				return n;
		}
		return nodesList.iterator().next();
	}

	@Override
	protected List<Process> handleFindByName(String name) throws Exception {
		LinkedList<Process> r = new LinkedList<Process>();
		for (ProcessEntity procEntity: getProcessEntityDao().findByName(name))
		{
			r.add( toProcess(procEntity));
		}
		return r;
	}

	@Override
	protected void handlePublish(Process process) throws Exception {
		Deployer d = new Deployer();
		d.setAuditEntityDao(getAuditEntityDao());
		d.setNodeDao(getNodeEntityDao());
		d.setBpmEngine( getBpmEngine() );

		ProcessEntity pe =  getProcessEntityDao().processToEntity(process);
		updateProcess (pe, process);
		if (pe.getVersion() == null)
			pe.setVersion(1L);
		else
			pe.setVersion( pe.getVersion().longValue() + 1);
		getProcessEntityDao().update(pe);
		
		d.deploy(pe, process);
		
	}

	@Override
	protected void handleRemove(Process process) throws Exception {
		ProcessEntity procEntity = getProcessEntityDao().load(process.getId());
		for ( NodeEntity ne: procEntity.getNodes())
		{
			getTransitionEntityDao().remove(ne.getOutTransitions());
			getFieldEntityDao().remove(ne.getFields());
			getTriggerEntityDao().remove(ne.getTriggers());
		}
		getAttributeEntityDao().remove(procEntity.getAttributes());
		getNodeEntityDao().remove(procEntity.getNodes());
		getProcessEntityDao().remove(procEntity);
	}

	@Override
	protected Process handleFindById(Long id) throws Exception {
		ProcessEntity procEntity = getProcessEntityDao().load(id);
		if (procEntity == null)
			return null;
		else
			return toProcess(procEntity);
	}

	@Override
	public Node handleGetTaskNode(TaskInstance task) throws Exception {
		JbpmContext ctx = getBpmEngine().getContext();
		org.jbpm.taskmgmt.exe.TaskInstance taskInstance = ctx.getTaskInstance(task.getId());
		org.jbpm.graph.def.Node jbpmNode = taskInstance.getToken().getNode();

		org.jbpm.graph.def.ProcessDefinition pd = taskInstance.getProcessInstance().getProcessDefinition();

		FileDefinition fd = pd.getFileDefinition();
		
		InputStream in = fd.getInputStream("task#"+jbpmNode.getId());
		if (in == null)
			return null;
		com.soffid.iam.addons.bpm.common.Node tn2 = (Node) new ObjectInputStream(in).readObject();
		return tn2;
	}

}
