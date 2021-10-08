package com.soffid.iam.addons.bpm.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.jbpm.JbpmContext;
import org.jbpm.context.def.ContextDefinition;
import org.jbpm.file.def.FileDefinition;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.EndState;
import org.jbpm.graph.node.Fork;
import org.jbpm.graph.node.Join;
import org.jbpm.graph.node.MailNode;
import org.jbpm.graph.node.StartState;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.instantiation.Delegation;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.soffid.iam.addons.bpm.common.Attribute;
import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.Filter;
import com.soffid.iam.addons.bpm.common.NodeType;
import com.soffid.iam.addons.bpm.common.PageInfo;
import com.soffid.iam.addons.bpm.common.Trigger;
import com.soffid.iam.addons.bpm.common.WorkflowType;
import com.soffid.iam.addons.bpm.handler.ApplyAccountHandler;
import com.soffid.iam.addons.bpm.handler.ApplyHandler;
import com.soffid.iam.addons.bpm.handler.AssignmentHandler;
import com.soffid.iam.addons.bpm.handler.ComputeMatchesNodeHandler;
import com.soffid.iam.addons.bpm.handler.CustomActionHandler;
import com.soffid.iam.addons.bpm.handler.GrantTaskNodeHandler;
import com.soffid.iam.addons.bpm.handler.StartAccountHandler;
import com.soffid.iam.addons.bpm.handler.StartHandler;
import com.soffid.iam.addons.bpm.model.NodeEntity;
import com.soffid.iam.addons.bpm.model.NodeEntityDao;
import com.soffid.iam.addons.bpm.model.ProcessEntity;
import com.soffid.iam.addons.bpm.model.TransitionEntity;
import com.soffid.iam.api.Audit;
import com.soffid.iam.bpm.business.Messages;
import com.soffid.iam.bpm.mail.Mail;
import com.soffid.iam.bpm.model.ProcessDefinitionProperty;
import com.soffid.iam.bpm.model.ProcessDefinitionUserRole;
import com.soffid.iam.bpm.model.TenantModuleDefinition;
import com.soffid.iam.bpm.model.UserInterface;
import com.soffid.iam.bpm.model.dal.ProcessDefinitionPropertyDal;
import com.soffid.iam.bpm.service.BpmEngine;
import com.soffid.iam.common.security.SoffidPrincipal;
import com.soffid.iam.model.AuditEntity;
import com.soffid.iam.model.AuditEntityDao;
import com.soffid.iam.utils.Security;

import es.caib.bpm.vo.PredefinedProcessType;
import es.caib.seycon.ng.exception.InternalErrorException;

public class Deployer {
	private BpmEngine bpmEngine;
	private NodeEntityDao nodeDao;
	private JbpmContext ctx;
	AuditEntityDao auditEntityDao; 
	
	public void deploy ( ProcessEntity procEntity, com.soffid.iam.addons.bpm.common.Process proc ) throws InternalErrorException, IOException
	{
		ctx = bpmEngine.getContext();
		
		try {
			ProcessDefinition def0 = findExistingDefinition ( ctx, procEntity );
			
			ProcessDefinition def = new ProcessDefinition();
			def.setDescription( procEntity.getDescription() );
			def.setName( procEntity.getName() );
			def.setTerminationImplicit(true);
			def.setVersion( def0 == null ? 1 : def0.getVersion()+1);

			ContextDefinition cd = new ContextDefinition();
			def.addDefinition(cd);
			FileDefinition fd = new FileDefinition();
			def.addDefinition(fd);
			TenantModuleDefinition td = new TenantModuleDefinition();
			td.setTenantId(Security.getCurrentTenantId());
			def.addDefinition(td);
			TaskMgmtDefinition tmd = new TaskMgmtDefinition();
			def.addDefinition(tmd);

			Map<NodeEntity, Node> nodesMap = new HashMap<NodeEntity, Node>();
			saveNodes ( def, procEntity, tmd, nodesMap );
			saveTransitions (def, procEntity, nodesMap);
			if (def.getStartState() == null)
			{
				throw new InternalErrorException ( "There is no starting node. Please, create one" );
			}
			ctx.getGraphSession().saveProcessDefinition(def);
			saveTaskNodeInformation(nodesMap, proc, fd);
			generateUiXml( fd, procEntity.getVersion() );
			generateZuls (fd, procEntity, def);
			ctx.getSession().saveOrUpdate(def);
			saveWorkflowType (procEntity, def, ctx);
			saveActors (procEntity, def);
			saveVersion(procEntity, def);
			
			upgradeProcess(def);
		} finally {
			ctx.close();
		}
	}

	private void saveWorkflowType(ProcessEntity procEntity, ProcessDefinition def, JbpmContext ctx2) {
		if (procEntity.getType() == WorkflowType.WT_PERMISSION)
		{
            ProcessDefinitionProperty prop = new ProcessDefinitionProperty();
            prop.setProcessDefinitionId(
            		new Long(def.getId()));
            prop.setName("type"); //$NON-NLS-1$
            prop.setValue(PredefinedProcessType.ROLE_GRANT_APPROVAL.getValue());
            ctx.getSession().save(prop);
            
            prop = new ProcessDefinitionProperty();
            prop.setProcessDefinitionId(
            		new Long(def.getId()));
            prop.setName("appliesTo"); //$NON-NLS-1$
            prop.setValue("privileged-account");
            ctx.getSession().save(prop);
		}
	}

	private void saveVersion(ProcessEntity procEntity, ProcessDefinition def) {
		ProcessDefinitionProperty processProperty = new ProcessDefinitionProperty();
		processProperty.setName("tag");
		processProperty.setProcessDefinitionId(def.getId());
		processProperty.setValue(procEntity.getVersion().toString());
		ctx.getSession().save(processProperty);

		SoffidPrincipal p = Security.getSoffidPrincipal();
		if (p != null)
		{
			processProperty = new ProcessDefinitionProperty();
			processProperty.setName("author");
			processProperty.setProcessDefinitionId(def.getId());
			processProperty.setValue(p.getUserName());
			ctx.getSession().save(processProperty);
		}
		processProperty = new ProcessDefinitionProperty();
		processProperty.setName("deployed");
		processProperty.setProcessDefinitionId(def.getId());
		processProperty.setValue(Long.toString(System.currentTimeMillis()));
		ctx.getSession().save(processProperty);
	}

	private void generateZuls(FileDefinition fd, ProcessEntity procEntity, ProcessDefinition def) {
		generateDefaultZul(fd, "ui/default.zul");
		
		String grantType = null;
		for ( NodeEntity node: procEntity.getNodes())
		{
			if (node.getType().equals(NodeType.NT_START))
				grantType = node.getGrantScreenType();
		}

		if ( procEntity.getType() == WorkflowType.WT_PERMISSION  && grantType!=null &&
				grantType.equals( "request") )
			generateZul(fd, "ui/start.zul", "request.zul");
		else
			generateDefaultZul(fd, "ui/start.zul");

		generateZul(fd, "ui/match.zul", "match.zul");

		for ( NodeEntity node: procEntity.getNodes())
		{
			if (node.getType().equals(NodeType.NT_SCREEN) ||
					node.getType().equals(NodeType.NT_GRANT_SCREEN))
			{
				UserInterface ui = new UserInterface();
				ui.setFileName("ui/default.zul");
				ui.setTarea(node.getTaskName() == null || node.getTaskName().trim().isEmpty() ? node.getName() : node .getTaskName());
				ui.setProcessDefinitionId(def.getId());
				ctx.getSession().save(ui);
			}
			if (node.getType().equals(NodeType.NT_MATCH_SCREEN) )
			{
				UserInterface ui = new UserInterface();
				ui.setFileName("ui/match.zul");
				ui.setTarea(node.getTaskName() == null || node.getTaskName().trim().isEmpty() ? node.getName() : node .getTaskName());
				ui.setProcessDefinitionId(def.getId());
				ctx.getSession().save(ui);
			}
		}
	}

	private void saveActors(ProcessEntity procEntity, ProcessDefinition def) {
		saveActors (def, "initiator", procEntity.getInitiators());
		saveActors (def, "observer", procEntity.getObservers());
		saveActors (def, "supervisor", procEntity.getManagers());
	}

	private void saveActors(ProcessDefinition def, String role, String actors) {
		if (actors != null && !actors.trim().isEmpty())
		{
			for (String actor: actors.trim().split("[ ,]+"))
			{
				ProcessDefinitionUserRole userRole = new ProcessDefinitionUserRole();
				userRole.setProcessDefinitionId(new Long(def.getId()));
				userRole.setUserRole(actor);
				userRole.setAppRole(role);
				userRole.setIsUser(false);
			
				ctx.getSession().save(userRole);
			}
		}
	}

	private void saveTransitions(ProcessDefinition def, ProcessEntity procEntity, Map<NodeEntity, Node> nodesMap) {
		for (NodeEntity nodeEntity: nodesMap.keySet())
		{
			Node jbpmNode = nodesMap.get(nodeEntity);
			for (TransitionEntity t: nodeEntity.getOutTransitions())
			{
				Transition jbpmTransition = new Transition();
				jbpmTransition.setName(t.getName());
				Node from = nodesMap.get(t.getSource());
				jbpmTransition.setFrom( from);
				Node to = nodesMap.get(t.getTarget());
				jbpmTransition.setTo(to);
				from.addLeavingTransition(jbpmTransition);
				to.addArrivingTransition(jbpmTransition);
				jbpmTransition.setProcessDefinition(def);
				Event event = null;
				if ( t.getScript() != null && ! t.getScript().trim().isEmpty())
				{
					Delegation d = new Delegation();
					d.setClassName(CustomActionHandler.class.getName());
					d.setConfigType("bean");
					d.setConfiguration("<script>" + 
							escape (t.getScript())+
							"</script>");
					Action a = new Action();
					a.setName(t.getName());
					a.setActionDelegation( d );
					a.setPropagationAllowed(true);

					event = new Event(jbpmTransition, Event.EVENTTYPE_TRANSITION);
					event.addAction(a);
					jbpmTransition.addEvent( event);
				}
				if (t.getSource().getType().equals(NodeType.NT_START) &&
						(procEntity.getType() == WorkflowType.WT_PERMISSION ||
						 procEntity.getType() == WorkflowType.WT_ACCOUNT_RESERVATION))
				{
					if ( event == null) {
						event = new Event(jbpmTransition, Event.EVENTTYPE_TRANSITION);
						jbpmTransition.addEvent( event);
					}
					
					Delegation d = new Delegation();
					
					if (procEntity.getType() == WorkflowType.WT_ACCOUNT_RESERVATION)
						d.setClassName(StartAccountHandler.class.getName());
					else
						d.setClassName(StartHandler.class.getName());
					
					Action a = new Action();
					a.setName(t.getName()+"Start");
					a.setActionDelegation( d );
					a.setPropagationAllowed(true);
					event.addAction(a);
				}
			}
		}
	}

	private void saveTaskNodeInformation(Map<NodeEntity, Node> nodesMap, com.soffid.iam.addons.bpm.common.Process proc, FileDefinition fd) throws IOException {
		LinkedList<Field> processFields = new LinkedList<Field>();
		for (NodeEntity nodeEntity: nodesMap.keySet())
		{
			Node jbpmNode = nodesMap.get(nodeEntity);
			for ( com.soffid.iam.addons.bpm.common.Node node: proc.getNodes())
			{
				if (node.getId().equals(nodeEntity.getId()))
				{
					addFields(processFields, node.getFields());
					PageInfo pageInfo = new PageInfo();
					pageInfo.setNodeType(node.getType());
					pageInfo.setFields(node.getFields().toArray( new Field[ node.getFields().size() ] ));
					pageInfo.setAttributes(proc.getAttributes().toArray(new Attribute[proc.getAttributes().size()]));
					pageInfo.setTriggers(node.getTriggers().toArray( new Trigger[ node.getTriggers().size()] ) );
					if (node.getFilters() != null)
						pageInfo.setFilters(node.getFilters().toArray( new Filter[node.getFilters().size()] ));
					pageInfo.setMatchThreshold(node.getMatchThreshold());
					pageInfo.setWorkflowType(proc.getType());
					pageInfo.setApproveTransition(node.getApproveTransition());
					pageInfo.setDenyTransition(node.getDenyTransition());
					ByteArrayOutputStream os = new ByteArrayOutputStream();
					new ObjectOutputStream(os).writeObject( pageInfo );
					fd.addFile("task#"+jbpmNode.getId(), os.toByteArray());
				}
			}
		}
		for ( com.soffid.iam.addons.bpm.common.Node node: proc.getNodes())
		{
			if (node.getType() == NodeType.NT_START)
			{
				addFields(processFields, node.getFields());
				PageInfo pageInfo = new PageInfo();
				pageInfo.setNodeType(NodeType.NT_START);
				pageInfo.setFields(node.getFields().toArray( new Field[ node.getFields().size() ] ));
				pageInfo.setAttributes(proc.getAttributes().toArray(new Attribute[proc.getAttributes().size()]));
				pageInfo.setTriggers(node.getTriggers().toArray( new Trigger[ node.getTriggers().size()] ) );
				pageInfo.setWorkflowType(proc.getType());
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				new ObjectOutputStream(os).writeObject( pageInfo );
				fd.addFile("task#start", os.toByteArray());
			}
		}
		// Generate process information
		PageInfo pageInfo = new PageInfo();
		pageInfo.setNodeType(NodeType.NT_SCREEN);
		pageInfo.setFields(processFields.toArray( new Field[ processFields.size() ] ));
		pageInfo.setAttributes(proc.getAttributes().toArray(new Attribute[proc.getAttributes().size()]));
		pageInfo.setWorkflowType(proc.getType());
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		new ObjectOutputStream(os).writeObject( pageInfo );
		fd.addFile("task#process", os.toByteArray());
	}

	private void addFields(LinkedList<Field> processFields, List<Field> fields) {
		for (Field field: fields)
		{
			boolean found = false;
			for ( Field f: processFields)
			{
				if (f.getName() != null && f.getName().endsWith(field.getName()))
				{
					found = true;
					break;
				}
			}
			if ( ! found)
			{
				Field field2 = new Field(field);
				field2.setReadOnly(true);
				processFields.add(field2);
			}
		}
		
	}

	private void saveNodes(ProcessDefinition def, ProcessEntity proc, TaskMgmtDefinition tmd, Map<NodeEntity, Node> nodesMap) throws InternalErrorException {

		for ( NodeEntity node: proc.getNodes())
		{
			Node n = null; 
			if (node.getType().equals( NodeType.NT_START))
			{
				n = new StartState();
				if (def.getStartState() == null)
					def.setStartState(n);
				else
					throw new InternalErrorException("More than one starting node exist. Please remove "+node.getName()+" or "+def.getStartState().getName());
			}
			else if (node.getType().equals( NodeType.NT_CUSTOM))
			{
				n = new Node();
				Delegation d = new Delegation();
				d.setClassName(CustomActionHandler.class.getName());
				d.setConfigType("bean");
				d.setConfiguration("<script>" + 
						escape (node.getCustomScript())+
						"</script>");
				Action a = new Action();
				a.setName(node.getName());
				a.setActionDelegation( d );
				a.setPropagationAllowed(true);
				n.setAction(a);
			}
			else if (node.getType().equals( NodeType.NT_APPLY))
			{
				n = new Node();
				if (! Boolean.TRUE.equals( node.getApplyEntitlements()) &&
						! Boolean.TRUE.equals(node.getApplyUserChanges()))
					throw new InternalErrorException ("Node "+node.getName()+" must check apply user changes, apply entitlements or both");
				Delegation d = new Delegation();
				if (proc.getType() == WorkflowType.WT_ACCOUNT_RESERVATION)
					d.setClassName(ApplyAccountHandler.class.getName());
				else
					d.setClassName(ApplyHandler.class.getName());
				d.setConfigType("bean");
				d.setConfiguration("<applyUserChanges>"+node.getApplyUserChanges()+"</applyUserChanges>"
						+ "<applyEntitlements>"+node.getApplyEntitlements()+"</applyEntitlements>");
				Action a = new Action();
				a.setName(node.getName());
				a.setActionDelegation( d );
				a.setPropagationAllowed(true);
				n.setAction(a);
			}
			else if (node.getType().equals((NodeType.NT_END)))
			{
				n = new EndState();
			}
			else if (node.getType().equals((NodeType.NT_SCREEN)))
			{
				n = new TaskNode();
				Task t = new Task();
				if (node.getMailActor() != null && ! node.getMailActor().isEmpty())
					t.setPooledActorsExpression( node.getMailActor() );
				else if (node.getCustomScript() != null && ! node.getCustomScript().isEmpty())
				{
					Delegation d = new Delegation();
					d.setClassName(AssignmentHandler.class.getName());
					d.setConfigType("bean");
					d.setConfiguration("<script>" + 
							escape (node.getCustomScript())+
							"</script>");
					t.setAssignmentDelegation( d );
				} else {
					t.setActorIdExpression("previous");
				}
				t.setDescription(node.getDescription());
				t.setName(node.getTaskName() == null || node.getTaskName().trim().isEmpty() ? node.getName() : node .getTaskName());
				t.setProcessDefinition(def);
				t.setTaskNode((TaskNode) n);
				t.setSignalling(true);
				tmd.addTask(t);
				
				// Add mail notification
				addMailNotification(node, n, t);
				
			}
			else if (node.getType().equals((NodeType.NT_GRANT_SCREEN)))
			{
				TaskNode tn = new TaskNode();
				n = tn;
				tn.setCreateTasks(false);
				tn.setSignal(TaskNode.SIGNAL_LAST_WAIT);
				
				Task t = new Task();
				t.setName(node.getTaskName() == null || node.getTaskName().trim().isEmpty() ? node.getName() : node .getTaskName());
				t.setDescription(node.getDescription());
				t.setProcessDefinition(def);
				t.setTaskNode((TaskNode) n);
				tmd.addTask(t);
				
				Delegation d2 = new Delegation();				
				Action action = new Action();
				Event ev = new Event(tn, "node-enter");
				
				d2.setClassName(GrantTaskNodeHandler.class.getName());
				d2.setConfigType("bean");
				d2.setConfiguration("<script>" + 
						escape (node.getCustomScript())+
						"</script><actor>"+
						escape (node.getMailActor())+
						"</actor>"+
						"<shortcut>"+
						(Boolean.TRUE.equals(node.getMailShortcut()) ? "true" : "false")+
						"</shortcut>"+
						"<type>"+
						escape (node.getGrantScreenType())+
						"</type>"
						);

				action.setEvent(ev);
				action.setName("Create tasks");
				action.setActionDelegation(d2);
				
				ev.addAction(action);
				tn.addEvent(ev);
			}
			else if (node.getType().equals((NodeType.NT_MATCH_SCREEN)))
			{
				TaskNode tn = new TaskNode();
				n = tn;
				Task t = new Task();
				if (node.getMailActor() != null && ! node.getMailActor().isEmpty())
					t.setPooledActorsExpression( node.getMailActor() );
				else if (node.getCustomScript() != null && ! node.getCustomScript().isEmpty())
				{
					Delegation d = new Delegation();
					d.setClassName(AssignmentHandler.class.getName());
					d.setConfigType("bean");
					d.setConfiguration("<script>" + 
							escape (node.getCustomScript())+
							"</script>");
					t.setAssignmentDelegation( d );
				} else {
					t.setActorIdExpression("previous");
				}
				t.setDescription(node.getDescription());
				t.setName(node.getTaskName() == null || node.getTaskName().trim().isEmpty() ? node.getName() : node .getTaskName());
				t.setProcessDefinition(def);
				t.setTaskNode((TaskNode) n);
				t.setSignalling(true);
				tmd.addTask(t);

				// Match computation
				Delegation d2 = new Delegation();				
				Action action = new Action();
				Event ev = new Event(tn, "task-create");
				
				d2.setClassName(ComputeMatchesNodeHandler.class.getName());
				d2.setConfigType("bean");

				action.setEvent(ev);
				action.setName("Create tasks");
				action.setActionDelegation(d2);
				
				ev.addAction(action);
				tn.addEvent(ev);
				
				// Add mail notification
				addMailNotification(node, tn, t);
			}
			else if (node.getType().equals((NodeType.NT_MAIL)))
			{
				n = new MailNode();
				Delegation d = new Delegation();
				d.setClassName(Mail.class.getName());
				StringBuffer mailConfig = new StringBuffer();
				if ( node.getMailActor() != null && ! node.getMailActor().trim().isEmpty())
				{
					mailConfig.append("<actors>")
						.append(escape(node.getMailActor()))
						.append("</actors>");
				}
				if ( node.getMailAddress() != null && !node.getMailAddress().trim().isEmpty())
				{
					mailConfig.append("<to>")
						.append(escape(node.getMailAddress()))
						.append("</to>");
				}
				if ( node.getMailSubject() != null && !node.getMailSubject().trim().isEmpty())
				{
					mailConfig.append("<subject>")
						.append(escape(node.getMailSubject()))
						.append("</subject>");
				}
				if ( node.getMailMessage() != null && !node.getMailMessage().trim().isEmpty())
				{
					mailConfig.append("<text>")
						.append(escape(node.getMailMessage()))
						.append("</text>");
				}
				d.setConfiguration(mailConfig.toString());
				Action a = new Action();
				a.setName(node.getName());
				a.setActionDelegation( d );
				a.setPropagationAllowed(true);
				n.setAction(a);
			}
			else if (node.getType().equals((NodeType.NT_FORK)))
			{
				Fork fork = new Fork();
				n = fork;
			}
			else if (node.getType().equals((NodeType.NT_JOIN)))
			{
				Join join = new Join();
				n = join;
				join.setNOutOfM(-1);
				join.setDiscriminator(false);
			}
			else
			{
				throw new InternalErrorException("Unexpected node type "+node.getType());
			}
			n.setName(node.getName());
			n.setDescription(node.getDescription());
			n.setProcessDefinition(def);
			def.addNode(n);
			nodesMap.put ( node, n );
			Set<String> names = new HashSet<String>();
			for (TransitionEntity transition: node.getOutTransitions()) {
				if (names.contains(transition.getName())) {
					throw new InternalErrorException("The node "+node.getName()+" has two transitions with the same name ["+transition.getName()+"]");
				}
				names.add(transition.getName());
			}
		}
		
	}

	public void addMailNotification(NodeEntity node, Node n, Task t) {
		Event ev = new Event(n, "task-create");
		Action action = new Action();
		Delegation d2 = new Delegation();				

		t.addEvent(ev);
		ev.addAction(action);
		action.setEvent(ev);
		action.setName(t.getName());
		action.setPropagationAllowed(true);
		action.setAsync(false);
		action.setActionDelegation(d2);
		
		if (Boolean.TRUE.equals(node.getMailShortcut())) {
			d2.setClassName("com.soffid.iam.addons.bpm.handler.MailShortcut");
		} else {
			d2.setClassName("com.soffid.iam.bpm.mail.Mail");
		}
		d2.setConfiguration("<template>task-assign</template>");
	}

	private String escape(String customScript) {
		if (customScript == null)
			return "";
		else
			return customScript.replace("&", "&amp;").replace("<", "&lt;").replaceAll(">", "&gt;");
	}

	private ProcessDefinition findExistingDefinition(JbpmContext ctx2, ProcessEntity proc) {
		return ctx.getGraphSession().findLatestProcessDefinition(proc.getName());
	}

	public BpmEngine getBpmEngine() {
		return bpmEngine;
	}

	public void setBpmEngine(BpmEngine bpmEngine) {
		this.bpmEngine = bpmEngine;
	}

	public JbpmContext getCtx() {
		return ctx;
	}

	public void setCtx(JbpmContext ctx) {
		this.ctx = ctx;
	}

	public NodeEntityDao getNodeDao() {
		return nodeDao;
	}

	public void setNodeDao(NodeEntityDao nodeDao) {
		this.nodeDao = nodeDao;
	}

	private void generateDefaultZul (FileDefinition def, String fileName)
	{
		generateZul (def, fileName, "user.zul");
	}

	private void generateZul (FileDefinition def, String fileName, String resource)
	{
		InputStream in = getClass().getResourceAsStream(resource);
		def.addFile(fileName, in);
	}
	
	private void generateUiXml (FileDefinition def, Long version)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream p = new PrintStream(out);
		p.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
				"\n" + 
				"<process>\n" + 
				"	<tag>"+version.toString()+"</tag>\n" + 
				"</process>");
		
		def.addFile("ui.xml", out.toByteArray());

	}


	private void upgradeProcess(ProcessDefinition processDefinition) {
        List definitions = ctx.getGraphSession()
                .findAllProcessDefinitionVersions(processDefinition.getName());
        ProcessDefinitionPropertyDal propertyDal = new ProcessDefinitionPropertyDal();
        propertyDal.setContext(ctx);
        
        for (Iterator it = definitions.iterator(); it.hasNext();) {
            ProcessDefinition def = (ProcessDefinition) it.next();
            if (def.getId() != processDefinition.getId()) {
            	doDefinitionUpgrade(def, processDefinition);
            }
            ctx.getSession().flush();
            ctx.getSession().clear();
        }
    }

    private void doDefinitionUpgrade(ProcessDefinition source,
            ProcessDefinition target)
    {
        Criteria busqueda = ctx.getSession().createCriteria(
                ProcessInstance.class);

        busqueda.add(Restrictions.eq("processDefinition", source));  //$NON-NLS-1$
        busqueda.add(Restrictions.isNull("end"));  //$NON-NLS-1$

        List resultado = busqueda.list();
        for (Iterator it = resultado.iterator(); it.hasNext();) {
            ProcessInstance instance = (ProcessInstance) it.next();
            doProcessUpgrade(instance, target);
        }
    }

	private boolean doProcessUpgrade(ProcessInstance instance, 
			ProcessDefinition target) {
		boolean ok = true;

        Collection taskInstances = instance.getTaskMgmtInstance().getTaskInstances();

		Hashtable tasks = new Hashtable();
		Hashtable tokens = new Hashtable();
		// Actualizar tareas
		for (Iterator taskIterator = taskInstances.iterator(); taskIterator
		        .hasNext();) {
		    TaskInstance ti = (TaskInstance) taskIterator.next();
		    String sourceTask = ti.getTask().getName();
		    String newTask = ti.getTask().getName();
		    Task targetTask = target.getTaskMgmtDefinition().getTask(
		            newTask);
		    if (targetTask == null) {
		        String message = String.format(Messages.getString("UserInterfaceBusiness.NotUpgradeTask"), sourceTask, //$NON-NLS-1$
		        		newTask, instance.getId());  
//		        messages.add (message);
		        ok = false;
		    } else
		        tasks.put(ti, targetTask);
		}
		// Actualizar tokens
		if (ok) {
		    Token token = instance.getRootToken();

		    ok = upgradeToken(token, target, tokens);
		}
		if (ok) {
		    for (Iterator taskIterator = tasks.keySet().iterator(); taskIterator
		            .hasNext();) {

		        TaskInstance ti = (TaskInstance) taskIterator.next();
		        String taskName = ti.getName();
		        boolean exactName = taskName == null || taskName.equals(ti.getTask().getName());
		        Task targetTask = (Task) tasks.get(ti);
		        ti.setTask(targetTask);
		        if (!exactName) {
		            ti.setName(taskName);
		        }
		    }
		    for (Iterator tokenIterator = tokens.keySet().iterator(); tokenIterator
		            .hasNext();) {
		        Token token = (Token) tokenIterator.next();
		        org.jbpm.graph.def.Node targetNode = (org.jbpm.graph.def.Node) tokens
		                .get(token);
		        token.setNode(targetNode);
		    }
		    instance.setProcessDefinition(target);
//		    messages.add (String.format(Messages.getString("UserInterfaceBusiness.UpgradedProcess"), instance.getId()));  //$NON-NLS-1$
		}
		return ok;
	}

    private boolean upgradeToken(Token token, ProcessDefinition target, Hashtable translations)
    {
        // Buscar el nuevo estado
        if (token.getNode() == null)
            return false;
        String newNodeName = null;
        
        String nodeName = token.getNode().getName();
        newNodeName = nodeName;

        org.jbpm.graph.def.Node newNode = target.getNode(nodeName);
        if (newNode == null) {
//            messages.add (String.format(Messages.getString("UserInterfaceBusiness.NotUpgradeNode"), nodeName, newNodeName, //$NON-NLS-1$
//            		token.getProcessInstance().getId())); 
            return false;
        }
        translations.put(token, newNode);
        boolean ok = true;
        for (Iterator it = token.getChildren().values().iterator(); ok
                && it.hasNext();) {
            Token child = (Token) it.next();
            ok = upgradeToken(child, target, translations);
        }
        return ok;
    }

	public void audit(org.jbpm.graph.def.ProcessDefinition definition, String action) {
		Audit auditoria = new Audit();
		auditoria.setAction(action); //$NON-NLS-1$
		auditoria.setAuthor(Security.getCurrentAccount());
		auditoria.setConfigurationParameter( definition.getName());
		auditoria.setObject("JBPM_PROCESSDEFINITON"); //$NON-NLS-1$
		AuditEntity auditoriaEntity = getAuditEntityDao().auditToEntity(
				auditoria);
		getAuditEntityDao().create(auditoriaEntity);
	}

	public AuditEntityDao getAuditEntityDao() {
		return auditEntityDao;
	}

	public void setAuditEntityDao(AuditEntityDao auditEntityDao) {
		this.auditEntityDao = auditEntityDao;
	}

}
