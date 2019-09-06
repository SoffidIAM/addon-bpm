package com.soffid.iam.addons.bpm.ui;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
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
import com.soffid.iam.api.Application;
import com.soffid.iam.api.DataType;
import com.soffid.iam.api.Group;
import com.soffid.iam.api.MetadataScope;
import com.soffid.iam.api.Role;
import com.soffid.iam.api.RoleAccount;
import com.soffid.iam.api.RoleGrant;
import com.soffid.iam.api.User;
import com.soffid.iam.bpm.api.ProcessInstance;
import com.soffid.iam.bpm.api.TaskInstance;
import com.soffid.iam.model.SystemEntity;
import com.soffid.iam.model.UserDataEntity;
import com.soffid.iam.model.UserGroupEntity;
import com.soffid.iam.service.ApplicationService;
import com.soffid.iam.service.impl.bshjail.SecureInterpreter;
import com.soffid.iam.web.users.additionalData.CustomField;

import bsh.EvalError;
import bsh.ParseException;
import bsh.TargetError;
import es.caib.bpm.toolkit.WorkflowWindow;
import es.caib.bpm.toolkit.exception.SystemWorkflowException;
import es.caib.bpm.toolkit.exception.UserWorkflowException;
import es.caib.bpm.toolkit.exception.WorkflowException;
import es.caib.seycon.ng.comu.Rol;
import es.caib.seycon.ng.exception.InternalErrorException;
import es.caib.seycon.ng.utils.Security;
import es.caib.zkib.binder.BindContext;
import es.caib.zkib.component.DataListbox;
import es.caib.zkib.component.DataTextbox;
import es.caib.zkib.datasource.XPathUtils;
import es.caib.zkib.zkiblaf.ImageClic;
import es.caib.zkib.zkiblaf.Missatgebox;

public class StandardUserWindow extends WorkflowWindow {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Div grid;
	private List<RoleRequestInfo> grants;
	private Grid grantsGrid;
	private Listbox approveGrantsGrid;
	private PageInfo pageInfo;
	private boolean grantsReadOnly;
	private boolean readonly;

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
		grid.setStyle("display: table; width: 100%; table-layout: fixed");
		appendChild(grid);
		try {
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
				
			}
			else
			{
				ProcessInstance proc = ProcessInstance.toProcessInstance( getProcessInstance() );
				pageInfo = EJBLocator.getBpmUserService().getPageInfo( proc );
			}
			
			generateFields();
			
			updateFieldsVisibility();
			
			for ( Trigger trigger: pageInfo.getTriggers())
			{
				if ("onLoad".equals(trigger.getName()))
					runTrigger(trigger);
			}
		} catch (Exception e) {
			throw new RuntimeException ("Error getting task information", e);
		}
	}

	
	private void runTrigger(Trigger trigger) throws InternalErrorException {
		SecureInterpreter interpreter = new SecureInterpreter();

		try {
			interpreter.set("serviceLocator", new com.soffid.iam.EJBLocator()); //$NON-NLS-1$
			interpreter.set("task", getTask()); //$NON-NLS-1$
			interpreter.set("workflowWindow", this); //$NON-NLS-1$
					
			interpreter.eval(trigger.getAction());
		} catch (TargetError e) {
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

	private void generateFields() throws Exception {
		grid.getChildren().clear();
		Arrays.sort(pageInfo.getFields(), new Comparator<Field>() {
			public int compare(Field o1, Field o2) {
				return o1.getOrder().compareTo(o2.getOrder());
			}
			
		});
		for ( Field field: pageInfo.getFields())
		{
			createField (field);
		}
	}

	private void createField(Field field) throws Exception {
		if (field.getName().equals("grants"))
		{
			if ( pageInfo.getNodeType() == NodeType.NT_GRANT_SCREEN)
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

	private void createGrants(Field field) throws Exception {
		Div d = new Div();
		d.setSclass("inputField");
		grid.appendChild(d);
		Label label = new Label(field.getLabel());
		label.setSclass("inputField_label");
		d.appendChild(label);
		Div data = new Div();
		data.setSclass("inputField_input");
		// data.setWidth("100%");
		d.appendChild(data);
		grantsGrid = new Grid();
		data.appendChild(grantsGrid);
		grantsGrid.setSclass("noBorderGrid grantsGrid");
		Columns cols = new Columns();
		grantsGrid.appendChild(cols);
		grantsGrid.setFixedLayout(true);
		grantsGrid.setWidth("100%");
		Column col1 = new Column(); col1.setWidth("150px");
		cols.appendChild(col1);
		Column col2 = new Column(); col2.setWidth("*");
		cols.appendChild(col2);
		grants = (List<RoleRequestInfo>) getVariables().get("grants");
		if (grants == null)
		{
			grants = new LinkedList<RoleRequestInfo>();
			getVariables().put("grants", grants);
		}
		grantsReadOnly = readonly || Boolean.TRUE.equals( field.getReadOnly() );
		regenerateAppRows(grantsGrid);
		if ( ! grantsReadOnly && pageInfo.getWorkflowType() == WorkflowType.WT_USER &&
				pageInfo.getNodeType() != NodeType.NT_GRANT_SCREEN)
		{
			Button b = new Button( Labels.getLabel("bpm.addApplication"));
			b.setParent(data);
			b.addEventListener("onClick", new EventListener() {
				public void onEvent(Event event) throws Exception {
					Window w = (Window) getFellow("searchApp");
					w.doHighlighted();
					Textbox e = (Textbox) w.getFellow("txtCodigoAplicacion");
					e.focus();
				}
			});
		}
		
	}

	private void regenerateAppRows(Grid grantsGrid) throws Exception {
		if (grantsGrid.getRows() == null)
		{
			grantsGrid.appendChild( new Rows() );
		}
		
		grantsGrid.getRows().getChildren().clear();
		int i = 1;
		for ( RoleRequestInfo grant: grants)
		{
			generateApplicationRow(grantsGrid, i, grant);
			i++;
		}
		
	}

	private void createStandardField(Field field)
			throws InternalErrorException, NamingException, CreateException, IOException {
		CustomField f = null;
		for  (Attribute att: pageInfo.getAttributes())
		{
			if (att.getName().equals(field.getName()))
			{
				f = new CustomField();
				f.setLabel(field.getLabel()+" :");
				f.setDataType(att.getType().toString());
				f.setDataObjectType(att.getDataObjectType());
				f.setBind( toBind ( att.getName()) );
				f.setReadonly( field.getReadOnly() != null && field.getReadOnly().booleanValue());
				f.setMaxLength(att.getSize());
				if (att.getMultiValued() != null)
					f.setMultiValue(att.getMultiValued());
				if (att.getValues() != null)
					f.setListOfValues(att.getValues().toArray(new String[0]));
				f.setAttribute("processAttributeDef", att);
				break;
			}
		}
		
		if (f == null)
		{
			for (DataType att: ServiceLocator.instance().getAdditionalDataService().findDataTypes2(MetadataScope.USER) )
			{
				if (att.getCode().equals(field.getName()))
				{
					f = new CustomField();
					f.setLabel(field.getLabel()+" :");
					f.setDataType(att.getType().toString());
					f.setDataObjectType(att.getDataObjectType());
					f.setBind( toBind ( att.getCode()) );
					f.setMaxLength(att.getSize());
					f.setReadonly(field.getReadOnly() != null && field.getReadOnly().booleanValue());
					f.setMultiValue(att.isMultiValued());
					if (att.getValues() != null)
						f.setListOfValues(att.getValues().toArray(new String[0]));
					f.setAttribute("standardAttributeDefinition", att);
					f.setFilterExpression(att.getFilterExpression());
					break ;
				}
			}
		}
		if (f == null)
		{
			f = new CustomField();
			f.setLabel(field.getLabel());
			f.setDataType("string");
			f.setDataObjectType(null);
			f.setBind( toBind ( field.getName()) );
			f.setMultiValue( false );
		}
		f.setReadonly( getTask() == null || getTask().getStart() == null ||
				Boolean.TRUE.equals(field.getReadOnly()));
		if (field.getValidationScript() != null && !field.getValidationScript().trim().isEmpty())
			f.setValidationScript(field.getValidationScript());
		if (field.getVisibilityScript() != null && !field.getVisibilityScript().trim().isEmpty())
			f.setVisibilityScript(field.getVisibilityScript());
		
		grid.appendChild(f);
		
		BindContext ctx = XPathUtils.getComponentContext(this);
		f.setContext(ctx.getXPath());
		f.setOwnerObject( getVariables());
		
		f.onCreate();
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

	private Map getVariables() {
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
		CustomField customField = (CustomField) event.getTarget();
		Field fieldDef = (Field) customField.getAttribute("fieldDef");
		if ( fieldDef.getName().equals("userSelector"))
		{
			fetchUserAttributes();
			refresh();
		}
		updateFieldsVisibility();
		for ( Trigger trigger: pageInfo.getTriggers())
		{
			if ("onChange".equals(trigger.getName()) && 
					fieldDef.getName().equals(trigger.getField()))
			{
				runTrigger(trigger);
			}
		}
	}
	

	protected void fetchUserAttributes() throws Exception {
		String user = (String) getVariables().get("userSelector");
		User u = com.soffid.iam.EJBLocator.getUserService().findUserByUserName(user);
		if (u != null)
		{
			Map<String, Object> atts = com.soffid.iam.EJBLocator.getUserService().findUserAttributes(user);
			if (atts == null)
				atts = new HashMap<String, Object>();
			for (DataType dt: ServiceLocator.instance().getAdditionalDataService().findDataTypes2(MetadataScope.USER))
			{
				if (dt.getBuiltin() != null && dt.getBuiltin().booleanValue())
				{
					Object o = PropertyUtils.getProperty(u, dt.getCode());
					getVariables().put(dt.getCode(), o);
				}
				else
				{
					getVariables().put(dt.getCode(), atts.get(dt.getCode()));
				}
			}
			populatePermissions(u);
			if (! "D".equals(getVariables().get("action")))
			{
				if (u.getActive().booleanValue())
					getVariables().put("action", "E");
				else
					getVariables().put("action", "M");
			}
			refresh ();
		}
				
	}
	
	protected void updateFieldsVisibility() {
		String action = (String) getVariables().get("action");
		boolean add = "A".equals(action);
		boolean disable = "D".equals(action);
		
		for (Div div: (Collection<Div>) grid.getChildren())
		{
			if (div instanceof CustomField)
			{
				CustomField customField = (CustomField) div;
				
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
				
			}
		}
	}
	
	private SecureInterpreter createInterpreter(CustomField cf, Field fieldDefinition, Object value) throws EvalError {
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
		for (CustomField customField: (Collection<CustomField>) grid.getChildren())
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
			CustomField customField;
			if (d instanceof CustomField)
			{
				customField = (CustomField) d;
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
	}
	
	private void generateApplicationRow(final Grid g, final int i, final RoleRequestInfo perm) throws Exception {
		ApplicationService service = ServiceLocator.instance().getApplicationService();
		
		Row r = new Row();
		r.setParent(g.getRows());
		//new Label("Aplicación "+perm.getApplicationName()).setParent(r);
		Label label = new Label((String) perm.getApplicationName());
		label.setParent(r);
		
		final Div d = new Div();
		d.setParent(r);
		
		if ( grantsReadOnly )
		{
			if (perm.getPreviousRoleId() != null && ! perm.getPreviousRoleId().equals(perm.getRoleId()))
			{
				Label l = new Label( String.format(Labels.getLabel("bpm.revokeRole"), perm.getPreviousRoleDescription()));
				l.setStyle("color: red");
				l.setParent(d);
			}
			
			if (perm.getPreviousRoleId() != null && perm.getPreviousRoleId().equals(perm.getRoleId()))
			{
				Label l = new Label(perm.getRoleDescription());
				l.setParent(d);
			}
			else 
			{
				Label l = new Label( String.format(Labels.getLabel("bpm.grantRole"), perm.getRoleDescription()));
				l.setStyle("color: blue");
				l.setParent(d);
			}
		} else {
			DataListbox lb = new DataListbox();
			lb.setBind("grants["+i+"]/roleId");
			lb.addEventListener("onSelect", onChangeRole);
			lb.setMold("select");
			lb.setParent(d);
			lb.setDisabled(grantsReadOnly);
			
			Security.nestedLogin(Security.getCurrentAccount(), new String[] {
				Security.AUTO_USER_QUERY+Security.AUTO_ALL,
				Security.AUTO_APPLICATION_QUERY+Security.AUTO_ALL,
				Security.AUTO_ROLE_QUERY+Security.AUTO_ALL
			});
			try {
				
				if ( perm.getParentRole() == null )
				{
					new Listitem("- No access -", null).setParent(lb);
					List<Role> roles = new LinkedList<Role>( ServiceLocator.instance().getApplicationService().findRolesByApplicationName(perm.getApplicationName()));
					Collections.sort(roles, new Comparator<Role>() {
						public int compare(Role o1, Role o2) {
							return o1.getDescription().compareTo(o2.getDescription());
						}
					});
					for (Role rol: roles)
					{
						if (rol.getBpmEnforced().booleanValue())
						{
							Listitem listitem = new Listitem(rol.getDescription(), rol.getId());
							lb.appendChild(listitem);
							if (rol.getId().equals( perm.getRoleId()))
								lb.setSelectedItem(listitem);
						}
					}
					
					lb.addEventListener("onSelect", new EventListener() {
						public void onEvent(Event event) throws Exception {
							createChildRoles (g, i-1);
						}
					});
					
				}
				else
				{
					lb.setMold("label");
					Role rol = service.findRoleById((Long) perm.getRoleId());
					Listitem listitem = new Listitem(rol.getDescription(), rol.getId());
					lb.appendChild(listitem);
					lb.setSelectedItem(listitem);
					lb.setDisabled(true);
				}
	
	
				if (perm.getParentRole() == null && perm.getPreviousRoleId() == null) 
				{
					ImageClic ci = new ImageClic("~./img/list-remove.gif");
					d.appendChild(ci);
					ci.setTitle(Labels.getLabel("usuaris.zul.Esborrarolsseleccion"));
					ci.addEventListener("onClick", new EventListener() {
						
						public void onEvent(Event event) throws Exception {
							grants.remove(i-1);
							regenerateAppRows(g);
							
						}
			
					});
					DataTextbox dtb = new DataTextbox();
					dtb.setBind("grants["+i+"]/comments");
					dtb.setMultiline(true);
					dtb.setRows(2);
					dtb.setWidth("100%");
					dtb.setReadonly(grantsReadOnly);
					d.appendChild(dtb);
				}
	
	
			} finally {
				Security.nestedLogoff();
			}
			
		}
	}

	private void createChildRoles(Grid g, int i) throws Exception {
		removeOrphanApps();

		createChildRolesNoRefresh(i);
		
		regenerateAppRows(g);
		
	}


	private void createChildRolesNoRefresh(int i) throws Exception {
		RoleRequestInfo app = grants.get(i);
		Long roleId = (Long) app.getRoleId();
		if (roleId != null)
		{
			Security.nestedLogin(Security.getCurrentAccount(), 
					new String [] {
				Security.AUTO_ROLE_QUERY+Security.AUTO_ALL,
				Security.AUTO_APPLICATION_QUERY+Security.AUTO_ALL
			});
			try {
				Role r = ServiceLocator.instance().getApplicationService().findRoleById(roleId);
				int first = i+1;
				List<RoleGrant> ownedRoles = new LinkedList<RoleGrant>( r.getOwnedRoles() );
				Collections.sort(ownedRoles, new Comparator<RoleGrant>() {
					public int compare(RoleGrant o1, RoleGrant o2) {
						int o = o1.getInformationSystem().compareTo(o2.getInformationSystem());
						if (o == 0)
							o = o1.getRoleName().compareTo(o2.getRoleName());
						return o;
					}
					
				});
				for ( RoleGrant grant: ownedRoles)
				{
					if (! Boolean.TRUE.equals(grant.getMandatory()))
					{
						Role r2 = ServiceLocator.instance().getApplicationService().findRoleById(grant.getRoleId());
						Application appDesc = ServiceLocator.instance().getApplicationService().findApplicationByApplicationName(r2.getInformationSystemName());
						RoleRequestInfo ri = new RoleRequestInfo();
						ri.setApplicationName(r2.getInformationSystemName());
						ri.setApplicationDescription(appDesc.getDescription());
						ri.setUserName(null);
						ri.setRoleId(r2.getId());
						ri.setParentRole(roleId);
						ri.setRoleDescription(r2.getDescription());
						grants.add(++i, ri);
					}
				}
			} finally {
				Security.nestedLogoff();
			}
		}
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
	

	public void buscarAplicaciones() throws NamingException, InterruptedException, RemoteException, CreateException, LoginException, SystemWorkflowException, SystemException, NotSupportedException, InternalErrorException
	{
	
		Listbox lstAplicaciones= null;
		Collection colAplicaciones= null, colAplicaciones1 = null, colAplicaciones2 = null;
		Textbox txtNombreAplicacion= null;
		Textbox txtCodigoAplicacion= null;
		Application aplicacion= null;
		Listitem item= null;
		
		String tipoSolicitud= null;
		
		com.soffid.iam.service.ejb.ApplicationService aplicacioService= com.soffid.iam.EJBLocator.getApplicationService();

		lstAplicaciones= (Listbox) getFellow("searchApp").getFellow("lstAplicaciones"); //$NON-NLS-1$
		txtCodigoAplicacion = (Textbox) getFellow("searchApp").getFellow("txtCodigoAplicacion"); //$NON-NLS-1$ //$NON-NLS-2$
		
		colAplicaciones = aplicacioService.findApplicationByText(txtCodigoAplicacion.getValue());
		lstAplicaciones.getItems().clear();
		lstAplicaciones.setSelectedItem(null);
       	
        for (Iterator it= colAplicaciones.iterator(); it.hasNext();)
        {
        	aplicacion= (Application)it.next();
        	
        	if ( Boolean.TRUE.equals( aplicacion.getBpmEnforced() ) )
        	{
        		item= new Listitem();
        		item.setValue(aplicacion.getName());
        		item.getChildren().add(new Listcell(aplicacion.getName()));
        		item.getChildren().add(new Listcell(aplicacion.getDescription()));	        	
        		lstAplicaciones.getItems().add(item);
        	}
        }
        lstAplicaciones.setVisible(true);
	}
	
	public void cancelPermission () throws InternalErrorException, NamingException, CreateException
	{
		Window w = (Window) getFellow("searchApp");
		w.setVisible(false);
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

			grants.add(ri);

			regenerateAppRows(grantsGrid);
		} finally {
			Security.nestedLogoff();
		}
		
		
		if (manual)
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


	private void populatePermissions(User u) throws Exception {
		// Elevo los permisos:
		Security.nestedLogin(Security.getCurrentAccount(), new String[] { Security.AUTO_METADATA_UPDATE_ALL,
				Security.AUTO_ACCOUNT_QUERY, Security.AUTO_ACCOUNT_QUERY + Security.AUTO_ALL,
				Security.AUTO_USER_QUERY + Security.AUTO_ALL, Security.AUTO_USER_ROLE_CREATE + Security.AUTO_ALL,
				Security.AUTO_USER_ROLE_DELETE + Security.AUTO_ALL, Security.AUTO_USER_CREATE + Security.AUTO_ALL,
				Security.AUTO_USER_UPDATE + Security.AUTO_ALL, Security.AUTO_USER_GROUP_CREATE + Security.AUTO_ALL,
				Security.AUTO_USER_GROUP_DELETE + Security.AUTO_ALL,
				Security.AUTO_USER_SET_PASSWORD + Security.AUTO_ALL,
				Security.AUTO_USER_UPDATE_PASSWORD + Security.AUTO_ALL,
				Security.AUTO_APPLICATION_QUERY + Security.AUTO_ALL, Security.AUTO_GROUP_CREATE + Security.AUTO_ALL,
				Security.AUTO_GROUP_QUERY + Security.AUTO_ALL, Security.AUTO_ROLE_QUERY + Security.AUTO_ALL });
		try {

			// es.caib.seycon.ng.servei.ejb.AplicacioService appSvc =
			// EJBLocator.getAplicacioService();

			ApplicationService appSvc = ServiceLocator.instance().getApplicationService();

			Collection<RoleAccount> userGrants = u == null ? new LinkedList<RoleAccount>()
					: appSvc.findUserRolesByUserName(u.getUserName());
			grants.clear();

			Grid g = (Grid) grantsGrid;
			if (grantsGrid == null)
				return;

			for (Row row : new LinkedList<Row>((List<Row>) g.getRows().getChildren())) {
				if (row.getAttribute("permRow") != null)
					row.setParent(null);
			}

			Long role = null;
			for (RoleAccount ra : userGrants) {
				Role r = appSvc.findRoleByNameAndSystem(ra.getRoleName(), ra.getSystem());

				if (r.getBpmEnforced() != null && r.getBpmEnforced().booleanValue()) {
					Application app = appSvc.findApplicationByApplicationName(r.getInformationSystemName());
					if (app.getBpmEnforced() != null && app.getBpmEnforced().booleanValue()) {
						RoleRequestInfo ri = new RoleRequestInfo();
						ri.setApplicationName( r.getInformationSystemName());
						ri.setPreviousRoleId(r.getId());
						ri.setPreviousRoleDescription(r.getDescription());
						ri.setRoleId(r.getId());
						ri.setUserName(ra.getUserCode());
						ri.setUserFullName( app.getDescription());

						grants.add(ri);
						createChildRolesNoRefresh(grants.size()-1);
					}
				}
			}
			getVariables().put("grants", grants);
			regenerateAppRows(grantsGrid);
			refresh();
		} finally {
			Security.nestedLogoff();
		}
	}

	private void createApproveGrants(Field field) throws Exception {
		Div d = new Div();
		d.setSclass("inputField");
		grid.appendChild(d);
		Label label = new Label(field.getLabel());
		label.setSclass("inputField_label");
		d.appendChild(label);
		Div data = new Div();
		data.setSclass("inputField_input");
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
		Listheader lh1 = new Listheader( Labels.getLabel("bpm.user") ); lh1.setWidth("150px");
		h.appendChild(lh1);
		lh1 = new Listheader ( Labels.getLabel("com.soffid.iam.api.User.fullName")); lh1.setWidth("250px");
		h.appendChild(lh1);
		Listheader lh2 = new Listheader( Labels.getLabel("bpm.permission") ); lh2.setWidth("*");
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
		grantsReadOnly = readonly || Boolean.TRUE.equals( field.getReadOnly() );
		regenerateApproveAppRows(approveGrantsGrid);
	}

	private void regenerateApproveAppRows(Listbox lb) throws InternalErrorException {
		lb.getItems().clear();
		int i = 1;
		for ( RoleRequestInfo grant: grants)
		{
			Long ti = (Long) grant.getTaskInstance();
			if (ti != null && ti.equals( getTask().getId() ))
				generateApproveApplicationRow(lb, i, grant);
			i++;
		}
		
	}

	private void generateApproveApplicationRow(Listbox lb, int i, RoleRequestInfo grant) throws InternalErrorException {
		Security.nestedLogin(Security.ALL_PERMISSIONS);
		try
		{
			String user = (String) grant.getUserName();
			if (user == null)
				user = (String) getVariables().get("userName");
			
			User u = ServiceLocator.instance().getUserService().findUserByUserName(user);
			
			if (u == null)
				return;
			
			Listitem item = new Listitem();
			item.appendChild( new Listcell(u.getUserName()));
			item.appendChild( new Listcell(u.getFullName()));
			
			Listcell permsCell = new Listcell();
			item.appendChild(permsCell);
			Long roleId = (Long) grant.getRoleId();
			Long previousRoleId = (Long) grant.getPreviousRoleId();
			if (roleId != null && previousRoleId == null)
			{
				Role role = ServiceLocator.instance().getApplicationService().findRoleById(roleId);
				permsCell.appendChild(new Label(role.getName()+": "+role.getDescription()));
			}
			else if (roleId != null)
			{
				// New role
				Role role = ServiceLocator.instance().getApplicationService().findRoleById(roleId);
				Div d = new Div();
				d.appendChild(new Label(role.getName()+": "+role.getDescription()));
				permsCell.appendChild(d);
				
				// Provious role
				role = ServiceLocator.instance().getApplicationService().findRoleById(previousRoleId);
				Label label = new Label(String.format ( Labels.getLabel("bpm.previouslyAssigned"), role.getName()+": "+role.getDescription()));
				label.setStyle("color: blue");
				d = new Div();
				d.appendChild(label);
				permsCell.appendChild(d);
			}
			else
			{
				Role role = ServiceLocator.instance().getApplicationService().findRoleById(previousRoleId);
				Label label = new Label(String.format ( Labels.getLabel("bpm.remove"), role.getName()+": "+role.getDescription()));
				label.setStyle("color: red");
				permsCell.appendChild(label);
			}
	
			Listcell c = new Listcell();
			item.appendChild(c);
			Checkbox cb = new Checkbox();
			cb.setDisabled( grantsReadOnly );
			cb.addEventListener("onCheck", onApprove);
			c.appendChild(cb);
			cb.setChecked( grant.isApproved());
			cb.setSclass("custom-checkbox-green custom-checkbox");
	
			c = new Listcell();
			item.appendChild(c);
			cb = new Checkbox();
			cb.setDisabled( grantsReadOnly );
			cb.addEventListener("onCheck", onDeny);
			c.appendChild(cb);
			cb.setChecked(grant.isDenied());
			cb.setSclass("custom-checkbox-red custom-checkbox");
			
			item.setValue(grant);
	
			lb.getItems().add(item);
		} finally {
			Security.nestedLogoff();
		}
	}
	
	EventListener onApprove = new EventListener() {
		public void onEvent(Event event) throws Exception {
			Listitem item = (Listitem) event.getTarget().getParent().getParent();
			RoleRequestInfo grant = (RoleRequestInfo) item.getValue();
			grant.setApproved(true);
			grant.setDenied(false);
			((Checkbox)event.getTarget().getParent().getNextSibling().getFirstChild()).setChecked(false);
		}
	};

	EventListener onDeny = new EventListener() {
		public void onEvent(Event event) throws Exception {
			Listitem item = (Listitem) event.getTarget().getParent().getParent();
			RoleRequestInfo grant = (RoleRequestInfo) item.getValue();
			grant.setDenied(true);
			grant.setApproved(false);
			((Checkbox)event.getTarget().getParent().getPreviousSibling().getFirstChild()).setChecked(false);
		}
	};


}
