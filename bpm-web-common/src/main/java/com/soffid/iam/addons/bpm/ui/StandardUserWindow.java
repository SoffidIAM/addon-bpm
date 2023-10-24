package com.soffid.iam.addons.bpm.ui;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ejb.CreateException;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;

import org.apache.commons.beanutils.PropertyUtils;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import com.soffid.iam.ServiceLocator;
import com.soffid.iam.addons.bpm.common.Attribute;
import com.soffid.iam.addons.bpm.common.Constants;
import com.soffid.iam.addons.bpm.common.EJBLocator;
import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.NodeType;
import com.soffid.iam.addons.bpm.common.PageInfo;
import com.soffid.iam.addons.bpm.common.RoleRequestInfo;
import com.soffid.iam.addons.bpm.common.Trigger;
import com.soffid.iam.addons.bpm.common.WorkflowType;
import com.soffid.iam.addons.bpm.tools.TaskUtils;
import com.soffid.iam.api.Account;
import com.soffid.iam.api.Application;
import com.soffid.iam.api.DataType;
import com.soffid.iam.api.MetadataScope;
import com.soffid.iam.api.Role;
import com.soffid.iam.api.RoleAccount;
import com.soffid.iam.api.RoleGrant;
import com.soffid.iam.api.SoDRule;
import com.soffid.iam.api.User;
import com.soffid.iam.bpm.api.ProcessInstance;
import com.soffid.iam.bpm.api.TaskInstance;
import com.soffid.iam.interp.Evaluator;
import com.soffid.iam.service.ApplicationService;
import com.soffid.iam.service.impl.bshjail.SecureInterpreter;
import com.soffid.iam.web.WebDataType;
import com.soffid.iam.web.component.CustomField3;
import com.soffid.iam.web.component.InputField3;
import com.soffid.iam.web.component.InputFieldContainer;
import com.soffid.iam.web.popup.FinderHandler;

import bsh.EvalError;
import bsh.TargetError;
import es.caib.bpm.toolkit.WorkflowWindow;
import es.caib.bpm.toolkit.exception.SystemWorkflowException;
import es.caib.bpm.toolkit.exception.UserWorkflowException;
import es.caib.bpm.toolkit.exception.WorkflowException;
import es.caib.seycon.ng.comu.TypeEnumeration;
import es.caib.seycon.ng.exception.InternalErrorException;
import es.caib.seycon.ng.utils.Security;
import es.caib.zkib.binder.BindContext;
import es.caib.zkib.component.DataListbox;
import es.caib.zkib.component.DataTextbox;
import es.caib.zkib.datasource.XPathUtils;
import es.caib.zkib.zkiblaf.ImageClic;
import es.caib.zkib.zkiblaf.Missatgebox;

public class StandardUserWindow extends WorkflowWindow implements InputFieldContainer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected Div grid;
	protected List<RoleRequestInfo> grants;
	protected Div grantsGrid;
	private Listbox approveGrantsGrid;
	protected PageInfo pageInfo;
	private boolean grantsReadOnly;
	private boolean readonly;
	private boolean ignoreEmptyFields;
	protected Map<String, Component > inputFields = new HashMap<String, Component>();
	
	public void onCreate () {
	}

	@Override
	protected void load() {
		readonly = getTask() == null || getTask().getStart() == null ;

		if (getTask() != null && ! getTask().getVariables().containsKey(Constants.REQUESTER_VAR)) {
			getTask().getVariables().put(Constants.REQUESTER_VAR, Security.getCurrentUser());
			getTask().getVariables().put(Constants.REQUESTER_NAME_VAR, Security.getSoffidPrincipal().getFullName());
		}

		grid = new Div();
		grid.setStyle("display: table; border-collapse: collapse; width: 100%; table-layout: fixed");
		Component a = getFellowIfAny("attributes");
		if (a != null)
			a.appendChild(grid);
		else
			appendChild(grid);
		
		try {
			String myName = Security.getCurrentUser();
			if (myName.startsWith("*"))
				myName = myName.substring(1);
			Security.nestedLogin(Security.ALL_PERMISSIONS);
			try {
				User my = com.soffid.iam.EJBLocator.getUserService().findUserByUserName(myName);
				myGrants = com.soffid.iam.EJBLocator.getApplicationService().findEffectiveRoleGrantByUser(my.getId());
			} finally {
				Security.nestedLogoff();
			}
			if (getTask() != null)
			{
				TaskInstance task = TaskInstance.toTaskInstance( getTask() );
				pageInfo = EJBLocator.getBpmUserService().getPageInfo( task );
				grants = (List<RoleRequestInfo>) getVariables().get("grants");
				if (grants == null)
				{
					grants = new LinkedList<RoleRequestInfo>();
					getVariables().put("grants", grants);
				}
				
				ignoreEmptyFields = false;
			}
			else
			{
				ProcessInstance proc = ProcessInstance.toProcessInstance( getProcessInstance() );
				pageInfo = EJBLocator.getBpmUserService().getPageInfo( proc );
				ignoreEmptyFields = true;
			}
			
			if (pageInfo != null && pageInfo.getUploadDocuments() != null) {
				setCanAddAttachments(pageInfo.getUploadDocuments().booleanValue());
			}
			
			if (pageInfo.getTriggers() != null) {
				for ( Trigger trigger: pageInfo.getTriggers())
				{
					if ("onLoad".equals(trigger.getName()))
						runTrigger(trigger, null);
				}
			}
			
			if (pageInfo.getWorkflowType() == WorkflowType.WT_ACCOUNT_RESERVATION) {
				Date until = (Date) getVariables().get("until");
				if (until == null) {
					until = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
					getVariables().put("until", until);
				}
			}
			generateFields();
			
			adjustVisibility();
			
		} catch (Exception e) {
			throw new RuntimeException ("Error getting task information", e);
		}
	}

	
	private void runTrigger(Trigger trigger, Component inputField) throws InternalErrorException, WorkflowException {
		SecureInterpreter interpreter = new SecureInterpreter();

		try {
			if (trigger.getAction() != null &&
					!trigger.getAction().trim().isEmpty())
			{
				interpreter.set("serviceLocator", new com.soffid.iam.EJBLocator()); //$NON-NLS-1$
				interpreter.set("task", getTask()); //$NON-NLS-1$
				if (inputField != null && inputField instanceof InputField3)
					interpreter.set("value", ((InputField3)inputField).getValue()); //$NON-NLS-1$
				interpreter.set("attributes", getTask().getVariables()); //$NON-NLS-1$
				interpreter.set("inputField", inputField); //$NON-NLS-1$
				interpreter.set("inputFields", inputFields); //$NON-NLS-1$
				interpreter.set("workflowWindow", this); //$NON-NLS-1$
				
				interpreter.eval(trigger.getAction());
				
			}
		} catch (TargetError e) {
			if (e.getTarget() instanceof InternalErrorException)
				throw (InternalErrorException) e.getTarget();
			else if (e.getTarget() instanceof WorkflowException)
				throw (WorkflowException) e.getTarget();
			else
				throw new InternalErrorException ("Error evaluating trigger "+trigger.getName()+": "+
					e.toString(),
					e.getTarget());
		} catch (EvalError e) {
			throw new InternalErrorException ("Error evaluating trigger "+trigger.getName()+": "+e.toString(), 
					e);
		} catch (Exception e) {
			throw new InternalErrorException ("Error evaluating trigger "+trigger.getName(), 
					e);
		}
	}

	protected void generateFields() throws Exception {
		grid.getChildren().clear();
		Arrays.sort(pageInfo.getFields(), new Comparator<Field>() {
			public int compare(Field o1, Field o2) {
				return o1.getOrder().compareTo(o2.getOrder());
			}
			
		});
		Map vars = getTask() == null ? getProcessInstance().getVariables() : getTask().getVariables();
		inputFields = new HashMap<String, Component>();
		for ( Field field: pageInfo.getFields())
		{
			if ( ! ignoreEmptyFields || vars.get(field.getName()) != null)
				createField (field);
		}
		
		for ( Component input: inputFields.values())
			if (input instanceof InputField3)
				((InputField3) input).runOnLoadTrigger();

	}

	private void createField(Field field) throws Exception {
		if (field.getName().equals("grants"))
		{
			if ( pageInfo.getNodeType() == NodeType.NT_GRANT_SCREEN || readonly || Boolean.TRUE.equals(field.getReadOnly()))
				createApproveGrants (field);
			else
				createGrants (field);
		}
		else
		{
			createStandardField(field);
			return;
			
		}
	}

	protected void createGrants(Field field) throws Exception {
		Div d = new Div();
		d.setSclass("databox");
		grid.appendChild(d);
		Label label = new Label(field.getLabel());
		label.setSclass("label");
		d.appendChild(label);
		Div data = new Div();
		data.setSclass("container");
		// data.setWidth("100%");
		d.appendChild(data);
		grantsGrid = new Div();
		grantsGrid.setStyle("display: table");
		data.appendChild(grantsGrid);
		grantsGrid.setSclass("noBorderGrid grantsGrid");
		grantsGrid.setWidth("100%");
		grants = (List<RoleRequestInfo>) getVariables().get("grants");
		if (grants == null)
		{
			grants = new LinkedList<RoleRequestInfo>();
			getVariables().put("grants", grants);
		}
		grantsReadOnly = readonly || Boolean.TRUE.equals( field.getReadOnly() );
		regenerateAppRows(grantsGrid);
		if ( ! grantsReadOnly && 
				( pageInfo.getWorkflowType() == WorkflowType.WT_USER || 
				  pageInfo.getWorkflowType() == WorkflowType.WT_PERMISSION ) && 
				pageInfo.getNodeType() != NodeType.NT_GRANT_SCREEN)
		{
			Button b = new Button( Labels.getLabel("bpm.addApplication"));
			b.setParent(data);
			b.addEventListener("onClick", new EventListener() {
				public void onEvent(Event event) throws Exception {
					buscarAplicaciones();
				}
			});
		}
		inputFields.put (field.getName(), d);
		
	}

	private void calculateRisks() throws InternalErrorException {
		LinkedList<RoleAccount> ra = new LinkedList<>();
		HashSet<String> users = new HashSet<>();
		long i  = 0;
		for (RoleRequestInfo grant: grants) {
			if (grant.getRoleId() != null) {
				RoleAccount r = new RoleAccount();
				r.setInformationSystemName(grant.getApplicationName());
				Role role = ServiceLocator.instance().getApplicationService().findRoleById(grant.getRoleId());
				r.setRoleName(role.getName());
				r.setSystem(role.getSystem());
				r.setUserCode(grant.getUserName());
				r.setId(i);
				ra.add(r);
			}
			i++;
		}
		
		ServiceLocator.instance().getSoDRuleService().qualifyRolAccountList(ra);
		for (RoleAccount r: ra) {
			RoleRequestInfo grant = grants.get(r.getId().intValue());
			grant.setSodRisk(r.getSodRisk());
			grant.setSodRules(r.getSodRules());
		}
	}

	private void regenerateAppRows(Div grantsGrid) throws Exception {
		grantsGrid.getChildren().clear();
		int i = 1;
		log.info("Generating screen for "+grants.size()+" grants");
		for ( RoleRequestInfo grant: grants)
		{
			if (grant.getParentRole() == null) {
				log.info("Generating row for "+grant);
				generateApplicationRow(grantsGrid, i, grant);
			}
			i++;
		}
		
	}

	private void createStandardField(Field field)
			throws InternalErrorException, NamingException, CreateException, IOException {
		CustomField3 f = null;
		for  (Attribute att: pageInfo.getAttributes())
		{
			if (att.getName().equals(field.getName()))
			{
				f = new CustomField3();
				f.setLabel(field.getLabel());
				if (att.getType() == null)
					f.setDataType(TypeEnumeration.STRING_TYPE.toString());
				else
					f.setDataType(att.getType().toString());
				f.setDataObjectType(att.getDataObjectType());
				f.setBind( toBind ( att.getName()) );
				f.setReadonly( field.getReadOnly() != null && field.getReadOnly().booleanValue());
				f.setRequired(Boolean.TRUE.equals(field.getRequired()));
				f.setMaxLength(att.getSize());
				if (att.getMultiValued() != null)
					f.setMultiValue(att.getMultiValued());
				if (att.getValues() != null)
					f.setListOfValues(att.getValues().toArray(new String[0]));
				f.setAttribute("processAttributeDef", att);
				inputFields.put (field.getName(), f);
				break;
			}
		}
		
		if (f == null)
		{
			for (DataType att: ServiceLocator.instance().getAdditionalDataService().findDataTypes2(MetadataScope.USER) )
			{
				if (att.getCode().equals(field.getName()))
				{
					WebDataType wdt = new WebDataType(att);
					f = new CustomField3();
					f.setLabel(field.getLabel() == null || field.getLabel().trim().isEmpty() ? wdt.getLabel(): field.getLabel());
					f.setDataType(att.getType().toString());
					f.setDataObjectType(att.getDataObjectType());
					f.setBind( toBind ( att.getCode()) );
					f.setMaxLength(att.getSize());
					f.setReadonly(field.getReadOnly() != null && field.getReadOnly().booleanValue());
					f.setRequired(Boolean.TRUE.equals(field.getRequired()));
					f.setMultiValue(att.isMultiValued());
					if (att.getValues() != null)
						f.setListOfValues(att.getValues().toArray(new String[0]));
					f.setAttribute("standardAttributeDefinition", att);
					f.setFilterExpression(att.getFilterExpression());
					inputFields.put (field.getName(), f);
					break ;
				}
			}
		}
		if (f == null)
		{
			f = new CustomField3();
			f.setLabel(field.getLabel());
			f.setDataType("STRING");
			f.setDataObjectType(null);
			f.setBind( toBind ( field.getName()) );
			f.setMultiValue( false );
		}

		grid.appendChild(f);
		
		BindContext ctx = XPathUtils.getComponentContext(this);
		f.setContext(ctx.getXPath());
		f.setOwnerObject( getTask() == null ? getProcessInstance() : getTask());
		f.setOwnerContext(getProcessInstance().getDescription());

		f.setReadonly( getTask() == null || getTask().getStart() == null ||
				Boolean.TRUE.equals(field.getReadOnly()));
		if (field.getValidationScript() != null && !field.getValidationScript().trim().isEmpty())
			f.setValidationScript(field.getValidationScript());
		if (field.getVisibilityScript() != null && !field.getVisibilityScript().trim().isEmpty())
			f.setVisibilityScript(field.getVisibilityScript());
		if (field.getFilterExpression() != null && !field.getFilterExpression().trim().isEmpty())
			f.setFilterExpression(field.getFilterExpression());
		f.afterCompose();
		f.addEventListener("onChange", onChangeField);
		f.setAttribute("fieldDef", field);
		return ;
	}

	private String toBind(String name) {
		String s = name;
		StringBuffer sb = new StringBuffer();
		Map map = getVariables();
		do
		{
			int i = s.indexOf("{\"");
			if (i < 0)
			{
				sb.append(name);
				return sb.toString();
			}
			String part = s.substring(0,  i);
			sb.append(part);
			if ( ! map.containsKey(part))
			{
				map.put(part, new HashMap<String,Object>());
			}
			map = (Map) map.get(part);
			int j = s.indexOf("\"}");
			if (j < 0)
				throw new UiException("Invalid attribute name "+name);
			
			s = s.substring(i+2, j) + s.substring(j+2);
		} while (true);
	}

	protected Map getVariables() {
		if (getTask() != null)
			return getTask().getVariables();
		else
			return getProcessInstance().getVariables();
	}
	
	EventListener onChangeField = new EventListener() {
		public void onEvent(Event event) throws Exception {
			onChangeField(event);
		}
	};

	EventListener onChangeRole = new EventListener() {
		public void onEvent(Event event) throws Exception {
			DataListbox listbox = (DataListbox) event.getTarget();
			Long id = (Long) listbox.getSelectedItem().getValue();
			String bind = listbox.getBind();
			bind = bind.substring(0, bind.indexOf("/"))+"/roleDescription";
			if (id == null)
			{
				XPathUtils.setValue(listbox.getParent(), bind, null);
			}
			else
			{
				Security.nestedLogin(Security.ALL_PERMISSIONS);
				try
				{
					ApplicationService service = ServiceLocator.instance().getApplicationService();
					Role role = service.findRoleById(id);
					XPathUtils.setValue(listbox.getParent(), bind, role == null ? null: role.getDescription());
					
				} finally {
					Security.nestedLogoff();
				}
			}
		}
	};

	private void onChangeField(Event event) throws Exception {
		CustomField3 customField = (CustomField3) event.getTarget();
		Field fieldDef = (Field) customField.getAttribute("fieldDef");
		if ( fieldDef.getName().equals("userSelector") && customField.isValid())
		{
			fetchUserAttributes();
			refresh();
			generateFields();
		}
		if (pageInfo.getTriggers() != null) {
			for ( Trigger trigger: pageInfo.getTriggers())
			{
				if ("onChange".equals(trigger.getName()) && 
						fieldDef.getName().equals(trigger.getField()))
				{
					runTrigger(trigger, customField);
				}
			}
		}
	}
	

	public void fetchUserAttributes() throws Exception {
		String user = (String) getVariables().get("userSelector");
		User u = com.soffid.iam.EJBLocator.getUserService().findUserByUserName(user);
		if (u != null)
		{
			Map<String, Object> atts = u.getAttributes();
			if (atts == null)
				atts = new HashMap<String, Object>();
			for (DataType dt: ServiceLocator.instance().getAdditionalDataService().findDataTypes2(MetadataScope.USER))
			{
				if (dt.getType() != TypeEnumeration.SEPARATOR) {
					if (dt.getBuiltin() != null && dt.getBuiltin().booleanValue())
					{
						Object o = PropertyUtils.getProperty(u, dt.getCode());
						getVariables().put(dt.getCode(), o);
						getVariables().put("old/"+dt.getCode(), o);
					}
					else
					{
						getVariables().put(dt.getCode(), atts.get(dt.getCode()));
						getVariables().put("old/"+dt.getCode(), atts.get(dt.getCode()));
					}
				}
			}
			TaskUtils.populatePermissions(getVariables() , u);
			if (! "D".equals(getVariables().get("action")))
			{
				if (u.getActive().booleanValue())
					getVariables().put("action", "M");
				else
					getVariables().put("action", "E");
			}
			refresh ();
		}
		refresh ();
				
	}
	
	private void populatePermissions() throws Exception {
		if (grantsGrid == null)
			return;

		for (Div row : new LinkedList<Div>((List<Div>) grantsGrid.getChildren())) {
			if (row.getAttribute("permRow") != null)
				row.setParent(null);
		}

		regenerateAppRows(grantsGrid);
		refresh();
	}

	private SecureInterpreter createInterpreter(CustomField3 cf, Field fieldDefinition, Object value) throws EvalError {
		Component grandpa = getParent().getParent();
		SecureInterpreter i = new SecureInterpreter();

		i.set("value", value);
		i.set("attributes", getVariables());
		i.set("serviceLocator", new com.soffid.iam.EJBLocator());
		if (getTask() != null)
		{
			i.set("object", getTask());
			i.set("task", getTask());
			i.set("process", getProcessInstance());
		}
		else
		{
			i.set("object", getProcessInstance());
			i.set("process", getProcessInstance());
		}
		BindContext ctx = XPathUtils.getComponentContext(this);
		i.set("context", ctx.getXPath());
		return i;
	}


	boolean validate ()
	{
		for (CustomField3 customField: (Collection<CustomField3>) grid.getChildren())
		{
			if (customField.isVisible() && ! customField.isReadonly())
			{
				if (! customField.validate())
					return false;
//				Field fieldDef = (Field) customField.getAttribute("fieldDef");
//				DataType dataType = (DataType) customField.getAttribute("standardAttributeDefinition");
//				Attribute attribute = (Attribute) customField.getAttribute("processAttributeDefinition");
//				if ( fieldDef.getValidationScript() != null && ! fieldDef.getValidationScript().trim().isEmpty())
//				{
//					Object value = customField.getValue();
//					try {
//						SecureInterpreter interp = createInterpreter(customField, fieldDef, value);
//						if (Boolean.FALSE.equals(  interp.eval( fieldDef.getVisibilityScript() )) )
//							throw new UiException ("Value not allowed for "+fieldDef.getLabel());
//					} catch (TargetError e) {
//						throw new UiException (String.format("Error evaluating validation expression for field %s: %s at %s",
//								fieldDef.getLabel(), e.getMessage(), e.getScriptStackTrace()),
//								e.getTarget());
//					} catch (Exception e) {
//						throw new UiException (String.format("Error evaluating validation expression for field %s",
//								fieldDef.getLabel()), e);
//					}
//					
//				}
			}
		}
		return true;
	}

	@Override
	protected void prepareTransition(String trasition) throws WorkflowException {
		for (Component d: (Collection<Component>)grid.getChildren())
		{
			CustomField3 customField;
			if (d instanceof CustomField3)
			{
				customField = (CustomField3) d;
				if (customField.isVisible() && ! customField.isReadonly())
				{
					if (! customField.validate())
						throw new UserWorkflowException ( String.format("Field %s is not valid", customField.getLabel()));
				}
			}
		}
		
		if (approveGrantsGrid != null && !grantsReadOnly )
		{
			for ( RoleRequestInfo grant: grants)
			{
				Long ti = (Long) grant.getTaskInstance();
				if (ti != null && ti.equals( getTask().getId() ))
				{
					if (! grant.isApproved() && ! grant.isDenied())
					{
						throw new UserWorkflowException(Labels.getLabel("bpm.missingApproval"));
					}
				}
			}
		}
		
		for ( Trigger trigger: pageInfo.getTriggers()) {
			if ( "onPrepareTransition".equals( trigger.getName() )) {
				try {
					runTrigger(trigger, null);
				} catch (WorkflowException e) {
					throw e;
				} catch (InternalErrorException e) {
					throw new SystemWorkflowException(e);
				}
			}
		}
		
		if (pageInfo.getWorkflowType() == WorkflowType.WT_ACCOUNT_RESERVATION) {
			String user = Security.getCurrentUser();
			Security.nestedLogin(Security.ALL_PERMISSIONS);
			try {
				String accountName = (String) getVariables().get("account");
				String dispatcherName = (String) getVariables().get("systemName");
				if (accountName == null || accountName.trim().isEmpty() ||
						dispatcherName == null || dispatcherName.trim().isEmpty()) {
					throw new UserWorkflowException("Please, select an account");
				}
				Account acc = com.soffid.iam.EJBLocator.getAccountService().findAccount(accountName, dispatcherName);
				getVariables().put("accountObject", acc);
				List<String> owners = new LinkedList<String>();
				if ( acc.getOwnerUsers() != null);
				owners.addAll(acc.getOwnerGroups());
				owners.addAll(acc.getOwnerRoles());
				getVariables().put("owners", owners);
			} catch (InternalErrorException e) {
				throw new UiException(e);
			} catch (NamingException e) {
				throw new UiException(e);
			} catch (CreateException e) {
				throw new UiException(e);
			} finally {
				Security.nestedLogoff();
			}
		}
		
		StringBuffer sb = new StringBuffer();
		for (RoleRequestInfo request: grants) {
			if (request.getRoleId() == null ? request.getPreviousRoleId() != null : 
					! request.getRoleId().equals(request.getPreviousRoleId())) {
				sb.append(request.getApplicationName())
					.append(": ")
					.append(request.getRoleId() != null ? request.getRoleDescription() :
							request.getPreviousRoleDescription());
				if (request.isApproved()) 
					sb.append(" [")
						.append(Labels.getLabel("bpm.approve"))
						.append("]");
				else if (request.isDenied()) 
					sb.append(" [")
					.append(Labels.getLabel("bpm.deny"))
					.append("]");
				sb.append("\n");
			}
		}
		if (sb.length() > 0) {
			if (sb.length() >= 2048)
				getVariables().put("grants_txt", sb.substring(0,  2048)+" (+)");
			else
				getVariables().put("grants_txt", sb.toString());
		}
	}
	
	private void generateApplicationRow(final Div g, final int i, final RoleRequestInfo perm) throws Exception {
		ApplicationService service = ServiceLocator.instance().getApplicationService();
		
		Div r = new Div();
		r.setStyle("width: 100%; display: table-row; border: 3px solid #e0e0e0;") ;
		r.setParent(g);
		//new Label("Aplicación "+perm.getApplicationName()).setParent(r);
			
		Div r1 = new Div();
		if (perm.getParentRole() == null)
			r1.setStyle("display: table-cell; width: 150px; padding-right: 10px; padding-top: 7px;");
		else
			r1.setStyle("display: block;");
			
		Label label = new Label();
		if (i == 1 || 
				! grants.get(i-2).getApplicationName().equals(perm.getApplicationName()))
			label.setValue(perm.getApplicationName());
		label.setParent(r1);
		r1.setParent(r);
		
		Div r0 = new Div();
		if (perm.getParentRole() == null)
			r0.setStyle("display: table-cell; width: 18px; "
				+ "vertical-align: top; "
				+ "text-align: center; "
				+ "background-repeat: no-repeat; "
				+ "background-size: contain; "
				+ "padding-top: 4px;\n"
				+ "background-position-y: 4px;");
		else
			r0.setStyle("display: none");
		r.appendChild(r0);

		final Div d = new Div();
		if (perm.getParentRole() == null)
			d.setStyle("display: table-cell; width: auto; padding-left: 10px; vertical-align:top;");
		else
			d.setStyle("width: 100%; padding-left: 32px;");
		d.setParent(r);
		
		Div rr = new Div();
		rr.setStyle("display: table-cell; width: 48px; vertical-align: top; text-align: center");
		r.appendChild(rr);
		
		if ( grantsReadOnly || perm.isMandatory())
		{
			if (perm.getPreviousRoleId() != null && ! perm.getPreviousRoleId().equals(perm.getRoleId()))
			{
				Label l = new Label( String.format(Labels.getLabel("bpm.revokeRole"), perm.getPreviousRoleDescription()));
				l.setStyle("color: red; vertical-align: top");
				l.setParent(d);
			}
			
			if (perm.getPreviousRoleId() != null && perm.getPreviousRoleId().equals(perm.getRoleId()))
			{
				Label l = new Label(perm.getRoleDescription());
				l.setStyle("vertical-align: top");
				l.setParent(d);
			}
			else 
			{
				Label l = new Label( String.format(Labels.getLabel("bpm.grantRole"), perm.getRoleDescription()));
				l.setStyle("color: blue; vertical-align: top");
				l.setParent(d);
			}
		} else {
			
			Security.nestedLogin(Security.getCurrentAccount(), new String[] {
				Security.AUTO_USER_QUERY+Security.AUTO_ALL,
				Security.AUTO_APPLICATION_QUERY+Security.AUTO_ALL,
				Security.AUTO_ROLE_QUERY+Security.AUTO_ALL
			});
			try {
				
				final Div childrenDiv = new Div();
				childrenDiv.setStyle("display: table; border-collapse: collapse; width: 100%");
				if ( perm.getSuggestedRoleId() == null )
				{
					CustomField3 field = new CustomField3();
					field.setNoLabel(true);
					field.setRaisePrivileges(true);
					field.setDataType("ROLE");
					field.setHideUserName(true);
					field.setFilterExpression("informationSystem.name eq \""+encodeScim (perm.getApplicationName())+"\" and manageableWF eq true") ;
					if (perm.getRoleId() != null) {
						Role rol = service.findRoleById( perm.getRoleId());
						field.setValue(rol.getName()+"@"+rol.getSystem());
					}
					field.addEventListener("onChange", (event) -> {
						Role role = (Role) field.getValueObject();
						RoleRequestInfo grant = grants.get(i-1);
						if (role == null) {
							grant.setRoleId(null);
							grant.setRoleDescription(null);
						} else {
							grant.setRoleId(role.getId());
							grant.setRoleDescription(role.getDescription());
						}
						createChildRoles (g, i-1);
						updateStatus (event.getTarget(), i-1);
					});
					d.appendChild(field);
					field.afterCompose();
					d.appendChild(new Label()); // Status label
					d.appendChild(childrenDiv); // Children
					updateStatus (field, i-1);
				}
				else
				{
					Label lb = new Label();
					lb.setValue(perm.getRoleDescription());
					lb.setStyle("display: block");
					lb.setParent(d);
					d.appendChild(new Label()); // Status label
					d.appendChild(childrenDiv); // Children
					updateStatus (lb, i-1);
				}
				r0.addEventListener("onClick", (ev) -> {
					Div b = (Div) ev.getTarget();
					if (b.getSclass() == null) {
						// Ignore
					}
					else if (b.getSclass().equals("collapser")) {
						b.setSclass("collapser open");
						childrenDiv.setStyle("display: table; width: 100%");
					}
					else if (b.getSclass().equals("collapser open")) {
						b.setSclass("collapser");
						childrenDiv.setStyle("display: none");
					}
				});
	
	
				ImageClic ci = new ImageClic("/img/close-on.svg");
				rr.appendChild(ci);
				ci.setTitle(Labels.getLabel("usuaris.zul.Esborrarolsseleccion"));
				ci.addEventListener("onClick", new EventListener() {
					
					public void onEvent(Event event) throws Exception {
						if (perm.getPreviousRoleId() == null)
							grants.remove(i-1);
						else
							perm.setRoleId(null);
						regenerateAppRows(grantsGrid);
					}
		
				});
				if (perm.getPreviousRoleId() == null && perm.getParentRole() == null) {
					DataTextbox dtb = new DataTextbox();
					dtb.setBind("grants["+i+"]/comments");
					dtb.setStyle("display: block; width: 100%; width: calc(100% - 64px); margin-left: 64px;");
					dtb.setMultiline(true);
					dtb.setRows(1);
					dtb.setPlaceholder( Labels.getLabel("task.comentari"));
					dtb.setReadonly(grantsReadOnly);
					d.appendChild(dtb);
				}
	
	
			} finally {
				Security.nestedLogoff();
			}
			
		}
	}

	private void updateStatus(Component lb, int i) throws Exception {
		Label l = (Label) lb.getNextSibling();
		Div expandDiv = (Div) lb.getParent().getPreviousSibling();
		Div childrenDiv = (Div) lb.getNextSibling().getNextSibling();
		childrenDiv.getChildren().clear();
		l.setStyle("vertical-align: middle");
		RoleRequestInfo perm = grants.get(i);
		if (perm.getRoleId() == null)
		{
			expandDiv.setSclass("");
			expandDiv.getChildren().clear();
			if (perm.getPreviousRoleId() == null)
				l.setValue("");
			else
			{
				l.setValue("The permission "+perm.getPreviousRoleDescription()+" will be removed");
				l.setStyle("color: red");
			}
			childrenDiv.setStyle("display: none");
		} else {
			Role r = null;
			List<RoleRequestInfo> children = getChildRoles(perm.getRoleId());
			
			childrenDiv.setStyle("display: none");
			if (! children.isEmpty()) {
				expandDiv.getChildren().clear();
				Image ci = new Image("/img/foldBar.svg");
				expandDiv.appendChild(ci);
				for (int j = 0; j < grants.size(); j++) {
					RoleRequestInfo grant = grants.get(j);
					if (grant.getParentRole() != null && grant.getParentRole().equals(perm.getRoleId()))
						generateApplicationRow(childrenDiv, j+1, grant);
				}
				if (anyIsOptional(children)) {
					expandDiv.setSclass("collapser open");
					childrenDiv.setStyle("display: table; width: 100%");
				} else {
					expandDiv.setSclass("collapser");
				}
			} else {
				expandDiv.setSclass("");
				expandDiv.getChildren().clear();
			}
			if (perm.getPreviousRoleId() == null)
			{
				l.setValue("New permission");
				l.setStyle("color: green");
				
			}
			else if (perm.getPreviousRoleId().equals(perm.getRoleId()))
				l.setValue("");
			else
			{
				l.setValue("Permission "+perm.getPreviousRoleDescription()+" will be replaced");
				l.setStyle("color: blue");
			}
		}
		
	}

	private boolean anyIsOptional(List<RoleRequestInfo> child) {
		for (RoleRequestInfo g: child) {
			if (!g.isMandatory())
				return true;
		}
		return false;
	}

	private List<RoleRequestInfo> getChildRoles(Long roleId) {
		List<RoleRequestInfo> l = new LinkedList<>();
		for (RoleRequestInfo grant: grants) {
			if (grant.getParentRole() != null && grant.getParentRole().equals(roleId))
				l.add(grant);
		}
		return l;
	}

	protected String encodeScim(String s) {
		return s.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\'", "\\\'");
	}
	
	private void createChildRoles(Div g, int i) throws Exception {
		RoleRequestInfo grant = grants.get(i);
		
		removeOrphanApps();

		i = grants.indexOf(grant);
		
		com.soffid.iam.addons.bpm.tools.TaskUtils.createChildRolesNoRefresh(grants, i);
		
//		regenerateAppRows(g);
		
	}



	private void removeOrphanApps() {
		for (Iterator<RoleRequestInfo> it = grants.iterator(); it.hasNext ();)
		{
			RoleRequestInfo app2 = it.next();
			Long parentRole = (Long) app2.getParentRole();
			if (parentRole != null)
			{
				boolean parentFound = false;
				for (RoleRequestInfo parent: grants)
				{
					if (parentRole.equals(parent.getRoleId()) )
					{
						parentFound = true;
						break;
					}
				}
				if (!parentFound)
					it.remove();
			}
		}
	}
	

	public void buscarAplicaciones() throws Exception
	{
		FinderHandler.startWizard("Select application", Application.class.getName(),
				this, true, 
				"wfManagement eq 'S'",
				(event) -> {
					List values = (List) event.getData();
					for (Object value: values) {
						addApplication(null, value.toString(), true, null);
					}
				});
	}
	
	public void addPermission () throws Exception
	{
		Window w = (Window) getFellow("searchApp");
		Listbox lstAplicaciones = (Listbox) w.getFellow("lstAplicaciones"); //$NON-NLS-1$
		Listitem selected = lstAplicaciones.getSelectedItem();
		if (selected != null)
		{
			String appName = ((Listcell)selected.getChildren().get(0)).getLabel();
			addApplication(w, appName, true, null);
		}
	}


	private void addApplication(Window w, String appName, boolean manual, Long parentRole) throws Exception {
		String appDesc = appName;
		Security.nestedLogin(Security.getCurrentAccount(), new String []
			{ Security.AUTO_APPLICATION_QUERY+Security.AUTO_ALL,
					Security.AUTO_ROLE_QUERY+Security.AUTO_ALL,
		});
		try {
			Application app = ServiceLocator.instance().getApplicationService().findApplicationByApplicationName(appName);
			appDesc = app.getDescription();
			if (app.isSingleRole())
			{
				if (aplicacionYaAsignada(appName))
				{
					if (manual) Missatgebox.avis( String.format("Only one permission can be assigned for application %s ", appDesc));
					return;
				}
			}

			RoleRequestInfo ri = new RoleRequestInfo();
			ri.setApplicationName(appName);
			ri.setApplicationDescription(appDesc);
			ri.setParentRole(parentRole);
			
			Role r = null;
			for (Role role: ServiceLocator.instance().getApplicationService().findRolesByApplicationName(app.getName())) {
				if (Boolean.TRUE.equals( role.getBpmEnabled())) {
					if (r == null) 
						r = role;
					else {
						r = null;
						break;
					}
				}
			}
			if (r != null) {
				ri.setRoleId(r.getId());
				ri.setRoleDescription(r.getDescription());
			}

			grants.add(ri);
			com.soffid.iam.addons.bpm.tools.TaskUtils.createChildRolesNoRefresh(grants, grants.size()-1);

			regenerateAppRows(grantsGrid);
		} finally {
			Security.nestedLogoff();
		}
		
		
		if (manual && w != null)
			w.setVisible(false);
	}

	private boolean aplicacionYaAsignada(String appName) {
		int i = 0;
		for (RoleRequestInfo req: grants)
		{
			i ++ ;
			if (req.getApplicationName().equals(appName))
			{
				return true;
			}
		}
		return false;
	}



	private void createApproveGrants(Field field) throws Exception {
		Div d = new Div();
		d.setSclass("databox");
		grid.appendChild(d);
		Label label = new Label(field.getLabel());
		label.setSclass("label");
		d.appendChild(label);
		Div data = new Div();
		data.setSclass("container");
		data.setWidth("100%");
		d.appendChild(data);
		approveGrantsGrid = new Listbox();
		data.appendChild(approveGrantsGrid);
		approveGrantsGrid.setSclass("grantsGrid");
		approveGrantsGrid.setFixedLayout(true);
		approveGrantsGrid.setWidth("100%");
		approveGrantsGrid.setStyle("margin-top: 25px");
		approveGrantsGrid.setRows(15);
		Listhead h = new Listhead();
		approveGrantsGrid.appendChild(h);
		Listheader lh1 = new Listheader( Labels.getLabel("bpm.user") ); lh1.setWidth("100px");
		h.appendChild(lh1);
		lh1 = new Listheader ( Labels.getLabel("com.soffid.iam.api.User.fullName")); lh1.setWidth("250px");
		h.appendChild(lh1);
		Listheader lh2 = new Listheader( Labels.getLabel("bpm.permission") ); // lh2.setWidth("400px");
		h.appendChild(lh2);
		Listheader lh3 = new Listheader( Labels.getLabel("bpm.approve") ); lh3.setWidth("80px");
		h.appendChild(lh3);
		Listheader lh4 = new Listheader( Labels.getLabel("bpm.deny") ); lh4.setWidth("80px");
		h.appendChild(lh4);

		grants = (List<RoleRequestInfo>) getVariables().get("grants");
		if (grants == null)
		{
			grants = new LinkedList<RoleRequestInfo>();
			getVariables().put("grants", grants);
		}
		calculateRisks();
		grantsReadOnly = readonly || Boolean.TRUE.equals( field.getReadOnly() );
		regenerateApproveAppRows(approveGrantsGrid);
		inputFields.put (field.getName(), d);
	}

	private void regenerateApproveAppRows(Listbox lb) throws InternalErrorException {
		lb.getItems().clear();
		int i = 1;
		for ( RoleRequestInfo grant: grants)
		{
			Long ti = (Long) grant.getTaskInstance();
			if ( pageInfo.getNodeType() != NodeType.NT_GRANT_SCREEN ||
					(ti != null && ti.equals( getTask().getId() )))
				if (grant.getParentRole() == null)
					generateApproveApplicationRow(lb, i, grant);
			i++;
		}
		
	}

	private void generateApproveApplicationRow(Listbox lb, int i, RoleRequestInfo grant) throws InternalErrorException {
		Security.nestedLogin(Security.ALL_PERMISSIONS);
		try
		{
			boolean showBoxes = true;
			String user = (String) grant.getUserName();
			if (user == null)
				user = (String) getVariables().get("userName");
			
			User u = ServiceLocator.instance().getUserService().findUserByUserName(user);

			String fullName = "";
			if (u == null) {
				String fn = (String) getVariables().get("firstName");
				String ln = (String) getVariables().get("lastName");
				if (fn != null && ln != null)
					fullName = fn+ " "+ln;
			} else {
				fullName = u.getFullName();
			}
			
			Listitem item = new Listitem();
			lb.getItems().add(item);

			if (grant.getParentRole() == null)
				item.appendChild( new Listcell(user));
			else
				item.appendChild(new Listcell());
			if (grant.getParentRole() == null) {
				Listcell listCell = new Listcell(fullName);
				item.appendChild( listCell);
				if (u != null)
					addIconPermissions(listCell, u);
			} else {
				item.appendChild(new Listcell());
			}
			
			Listcell permsCell = new Listcell();
			item.appendChild(permsCell);
			Long roleId = (Long) grant.getRoleId();
			Long previousRoleId = (Long) grant.getPreviousRoleId();
			if (roleId != null && previousRoleId == null)
			{
				addRisk (permsCell, grant);
				Role role = ServiceLocator.instance().getApplicationService().findRoleById(roleId);
				final Label l = new Label(role.getName()+": "+role.getDescription());
				l.setStyle("white-space: normal");
				permsCell.appendChild(l);
				if (grant.getParentRole() == null) {
					generateApproveChildRoles(grant, lb, i, roleId, l);
				}
				else if (grant.isMandatory())
					showBoxes = false;
			}
			else if (roleId != null)
			{
				addRisk (permsCell, grant);
				// New role
				Role role = ServiceLocator.instance().getApplicationService().findRoleById(roleId);
				Div d = new Div();
				d.setStyle("display: inline-block; max-width: calc( 100% - 48px)");
				final Label l = new Label(role.getName()+": "+role.getDescription());
				l.setStyle("white-space: normal");
				d.appendChild(l);
				permsCell.appendChild(d);
				
				if (! roleId.equals(grant.getPreviousRoleId())) {
					// Provious role
					role = ServiceLocator.instance().getApplicationService().findRoleById(previousRoleId);
					Label label = new Label(String.format ( Labels.getLabel("bpm.previouslyAssigned"), role.getName()+": "+role.getDescription()));
					label.setStyle("color: blue");
					d = new Div();
					d.appendChild(label);
					permsCell.appendChild(d);
					if (grant.getParentRole() == null)
						generateApproveChildRoles(grant, lb, i, roleId, l);
				} else {
					showBoxes = false;
					grant.setApproved(true);
					grant.setDenied(false);
				}
			}
			else
			{
				Role role = ServiceLocator.instance().getApplicationService().findRoleById(previousRoleId);
				Label label = new Label(String.format ( Labels.getLabel("bpm.remove"), role.getName()+": "+role.getDescription()));
				label.setStyle("color: red");
				permsCell.appendChild(label);
			}
			if (grant.getComments() != null && ! grant.getComments().trim().isEmpty()) {
				Label l = new Label(grant.getComments());
				permsCell.appendChild(l);
				l.setStyle("display: block; padding-left: 24px");
			}
			
			if (showBoxes) {
				Listcell c = new Listcell();
				item.appendChild(c);
				Checkbox cb = new Checkbox();
				cb.setDisabled( grantsReadOnly || ! showBoxes);
				cb.addEventListener("onCheck", onApprove);
				c.appendChild(cb);
				cb.setChecked( grant.isApproved());
				cb.setSclass("custom-checkbox-green custom-checkbox");
		
				c = new Listcell();
				item.appendChild(c);
				cb = new Checkbox();
				cb.setDisabled( grantsReadOnly || ! showBoxes);
				cb.addEventListener("onCheck", onDeny);
				c.appendChild(cb);
				cb.setChecked(grant.isDenied());
				cb.setSclass("custom-checkbox-red custom-checkbox");
			}			
			item.setValue(grant);
	
		} finally {
			Security.nestedLogoff();
		}
	}

	private void generateApproveChildRoles(RoleRequestInfo grant, Listbox lb, int i, Long roleId, Label l) throws InternalErrorException {
		List<RoleRequestInfo> children = getChildRoles(roleId);
		String title = "";
		for (int j = 0; j < grants.size(); j++) {
			RoleRequestInfo grant2 = grants.get(j);
			Long ti = (Long) grant2.getTaskInstance();
			if (grant2.getParentRole() != null && grant2.getParentRole().equals(roleId)) {
				if (grant2.isMandatory())
					title = title + String.format(Labels.getLabel("bpm.grantRole"), grant2.getRoleDescription())+"\n";
				else if (grantsReadOnly || grant.isApproved())
					generateApproveApplicationRow(lb, j, grant2);
			}
		}
		l.setTooltiptext(title);
	}
	
	private void addRisk(Listcell permsCell, RoleRequestInfo grant) {
		if (grant.getSodRisk() != null) {
			ImageClic ic = new ImageClic();
			ic.setSrc("/img/risk." + grant.getSodRisk().getValue()+".svg");
			ic.setStyle("vertical-align: middle; float: left");
			ic.setTitle(Labels.getLabel("com.soffid.iam.api.RoleAccount.sodRules"));
			ic.addEventListener("onClick", onClickSoD);
			ic.setAttribute("grant", grant);
			permsCell.appendChild(ic);
		}
	}

	private void addIconPermissions(Listcell listCell, final User u) {
		if (getTask() != null && getTask().isOpen())
		{
			ImageClic ic = new ImageClic();
			ic.setSrc("/img/info.svg");
			ic.setStyle("vertical-align: middle");
			ic.setTitle(Labels.getLabel("bpm.currentPermissions"));
			ic.addEventListener("onClick", onClickCurrentPermissions);
			ic.setAttribute("user", u);
			listCell.appendChild(ic);
		}
	}

	
	EventListener onClickCurrentPermissions = new EventListener() {
		
		public void onEvent(Event event) throws Exception {
			User u = (User) event.getTarget().getAttribute("user");
			Window w = (Window) getFellow("currentPermissions");
			Grid g = (Grid) w.getFirstChild();
			Rows rows = g.getRows();
			rows.getChildren().clear();
			Security.nestedLogin(Security.ALL_PERMISSIONS);
			try {
				ApplicationService aplicacioService = ServiceLocator.instance().getApplicationService();
				List<RoleAccount> roles = new LinkedList<RoleAccount>(aplicacioService.findUserRolesByUserName(u.getUserName()));
				Collections.sort(roles, new Comparator<RoleAccount>() {
					public int compare(RoleAccount o1, RoleAccount o2) {
						int i = o1.getInformationSystemName().compareTo(o2.getInformationSystemName());
						if (i == 0)
							i = o1.getRoleName().compareTo(o2.getRoleName());
						return i;
					}
				});
				for (RoleAccount roleAccount: roles)
				{
					Role role = aplicacioService.findRoleByNameAndSystem(roleAccount.getRoleName(), roleAccount.getSystem());
					if (role != null && role.getBpmEnforced() != null && role.getBpmEnforced().booleanValue())
					{
						Application app = aplicacioService.findApplicationByApplicationName(role.getInformationSystemName());
						if (app != null && app.getBpmEnforced() != null && app.getBpmEnforced().booleanValue())
						{
							Row r = new Row();
							rows.appendChild(r);
							if (roleAccount.getSodRisk() == null)
								r.appendChild(new Label());
							else
							{
								Image image = new Image();
								image.setSrc("/img/risk." + roleAccount.getSodRisk().getValue()+".svg");
								r.appendChild(image);
							}
							
							r.appendChild( new Label (app.getDescription()));
							r.appendChild( new Label ( role.getDescription() ));
						}
					}
				}
			} finally {
				Security.nestedLogoff();
			}
			((Button)w.getFellow("closeButton")).setDisabled(false);
			w.doHighlighted();
		}
	};
	
	EventListener onClickSoD = new EventListener() {
		public void onEvent(Event event) throws Exception {
			RoleRequestInfo u = (RoleRequestInfo) event.getTarget().getAttribute("grant");
			Window w = (Window) getFellow("sod");
			StringBuffer sb = new StringBuffer();
			List<SoDRule> rules = (List<SoDRule>) u.getSodRules();
			for (SoDRule rule: rules) {
				if (rule.getRisk() != null)
					sb.append("<img class='small-icon' src='"+getDesktop().getExecution().getContextPath()+"/img/risk."+rule.getRisk().getValue()+".svg'> </img>");
				sb.append(rule.getName());
				sb.append("<BR>");
			}
			CustomField3 risk = (CustomField3) w.getFellow("sodRisk");
			risk.setValue(u.getSodRisk());
			CustomField3 rf = (CustomField3) w.getFellow("sodRules");
			rf.setValue(sb.toString());
			w.doHighlighted();
		}
	};

	EventListener onApprove = new EventListener() {
		public void onEvent(Event event) throws Exception {
			Listitem item = (Listitem) event.getTarget().getParent().getParent();
			RoleRequestInfo grant = (RoleRequestInfo) item.getValue();
			grant.setApproved(true);
			grant.setDenied(false);
			((Checkbox)event.getTarget().getParent().getNextSibling().getFirstChild()).setChecked(false);
			Listbox lb = item.getListbox();
			for (Listitem item2:  (List<Listitem>) lb.getItems() ) {
				RoleRequestInfo grant2 = (RoleRequestInfo) item2.getValue();
				if (grant2.getParentRole() != null && grant2.getParentRole().equals(grant.getRoleId())) {
					grant2.setApproved(false);
					grant2.setDenied(false);
					item2.setVisible(true);
					((Checkbox) (item2.getLastChild().getFirstChild())).setChecked(false);
					((Checkbox) (item2.getLastChild().getPreviousSibling().getFirstChild())).setChecked(false);
				}
			}
		}
	};

	EventListener onDeny = new EventListener() {
		public void onEvent(Event event) throws Exception {
			Listitem item = (Listitem) event.getTarget().getParent().getParent();
			RoleRequestInfo grant = (RoleRequestInfo) item.getValue();
			grant.setDenied(true);
			grant.setApproved(false);
			((Checkbox)event.getTarget().getParent().getPreviousSibling().getFirstChild()).setChecked(false);
			Listbox lb = item.getListbox();
			for (Listitem item2:  (List<Listitem>) lb.getItems() ) {
				RoleRequestInfo grant2 = (RoleRequestInfo) item2.getValue();
				if (grant2.getParentRole() != null && grant2.getParentRole().equals(grant.getRoleId())) {
					grant2.setApproved(false);
					grant2.setDenied(true);
					item2.setVisible(false);
				}
			}
		}
	};
	protected Collection<RoleGrant> myGrants;

	@Override
	public Map<String, InputField3> getInputFieldsMap() {
		Map<String,InputField3> m = new HashMap<>();
		for (String key: inputFields.keySet()) {
			Object o = inputFields.get(key);
			if (o instanceof InputField3)
				m.put(key, (InputField3) o);
		}
		return m;
	}

	@Override
	public Map getAttributesMap() {
		return getTask().getVariables();
	}

	@Override
	public void adjustVisibility() {
		String action = (String) getVariables().get("action");
		boolean add = "A".equals(action);
		boolean disable = "D".equals(action);

		InputField3 container = null;
		boolean visible = false;
		for (Object o: inputFields.values())
		{
			if (o instanceof CustomField3) {
				CustomField3  customField = (CustomField3) o;
				Field fieldDef = (Field) customField.getAttribute("fieldDef");
				DataType dataType = (DataType) customField.getAttribute("standardAttributeDefinition");
				Attribute attribute = (Attribute) customField.getAttribute("processAttributeDefinition");
				if (fieldDef.getReadOnly() != null && fieldDef.getReadOnly().booleanValue())
					customField.setReadonly(true);
				else if ("action".equals(customField.getBind()))
				{
					customField.setReadonly(false);
					customField.adjustVisibility();
				}
				else if ("userSelector".equals(customField.getBind()))
				{
					customField.setVisible( ! add );
					customField.setReadonly(false);
					continue; // Do not apply attributeVisibl expression
				}
				else if ( dataType != null)
				{
					customField.setReadonly( disable );
					customField.adjustVisibility();
				}
				else
				{
					customField.setReadonly(false);
					customField.adjustVisibility();
				}

				
				if (customField.getDataType() != null && customField.getDataType().getType() == TypeEnumeration.SEPARATOR) {
					if (container != null) {
						container.setVisible(visible);
//						container.getParent().setVisible(visible);
					}
					container = customField;
					visible = false;
				}
				
				if (getTask() == null) {
					customField.setOwnerObject(getProcessInstance()); 
					customField.setOwnerContext(getProcessInstance().getDescription());
				} else {
					customField.setOwnerObject(getTask()); 
					customField.setOwnerContext(getTask().getProcessName());
				}
				boolean v = customField.attributeVisible();
				if (customField != container && container != null && v) visible = true;
				customField.setVisible(v);
			}
		}
		if (container != null) {
			container.setVisible(visible);
//			container.getParent().setVisible(visible);
		}
	}

	public boolean isAcceptable(Role role) throws InternalErrorException, IOException, Exception {
		if (pageInfo.getRoleFilter() == null || pageInfo.getRoleFilter().trim().isEmpty())
			return true;
		HashMap<String, Object> m = new HashMap<>();
		m.put("task", getTask());
		m.put("role", role);
		m.put("isGranted", false);
		for (RoleGrant grant: myGrants) {
			if (grant.getRoleName().equals(role.getName())) {
				m.put("isGranted", true);
				break;
			}
		}
		String userName = (String) getTask().getVariables().get("userSelector");
		String current = Security.getSoffidPrincipal().getUserName();
		if (userName == null || userName.trim().isEmpty() || userName.equals(current) || ("*"+userName).equals(current))
			m.put("selfRequest", true);
		else
			m.put("selfRequest", false);
		Object r = Evaluator.instance().evaluate(pageInfo.getRoleFilter(), 
				m, 
				"role filter");
		return r != null && !Boolean.FALSE.equals( r );
				
	}

	protected boolean isAcceptable(Application app) throws InternalErrorException, IOException, Exception {
		if (pageInfo.getApplicationFilter() == null || pageInfo.getApplicationFilter().trim().isEmpty())
			return true;
		HashMap<String, Object> m = new HashMap<>();
		m.put("task", getTask());
		m.put("application", app);
		m.put("isGranted", false);
		for (RoleGrant grant: myGrants) {
			if (grant.getInformationSystem().equals(app.getName())) {
				m.put("isGranted", true);
				break;
			}
		}
		String userName = (String) getTask().getVariables().get("userSelector");
		String current = Security.getSoffidPrincipal().getUserName();
		if (userName == null || userName.trim().isEmpty() || userName.equals(current) || ("*"+userName).equals(current))
			m.put("selfRequest", true);
		else
			m.put("selfRequest", false);
		Object r = Evaluator.instance().evaluate(pageInfo.getApplicationFilter(), 
				m, 
				"application filter");
		return r != null && !Boolean.FALSE.equals( r );
				
	}

}
