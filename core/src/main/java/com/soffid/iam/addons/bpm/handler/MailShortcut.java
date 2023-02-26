/**
 * Modificación de la clase org.jbpm.Mail para que se pueda extender sus funcionalidades.
 */
/**
 * Modificación de la clase org.jbpm.Mail para que se pueda extender sus funcionalidades.
 */
/**
 * Modificación de la clase org.jbpm.Mail para que se pueda extender sus funcionalidades.
 */
package com.soffid.iam.addons.bpm.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import javax.ejb.CreateException;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.jpdl.el.ELException;
import org.jbpm.jpdl.el.VariableResolver;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.SwimlaneInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.soffid.iam.ServiceLocator;
import com.soffid.iam.addons.bpm.common.Attribute;
import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.NodeType;
import com.soffid.iam.addons.bpm.common.PageInfo;
import com.soffid.iam.addons.bpm.common.RoleRequestInfo;
import com.soffid.iam.addons.bpm.core.BpmUserService;
import com.soffid.iam.api.Account;
import com.soffid.iam.api.Application;
import com.soffid.iam.api.AuthorizationRole;
import com.soffid.iam.api.CustomObject;
import com.soffid.iam.api.DataType;
import com.soffid.iam.api.Group;
import com.soffid.iam.api.GroupUser;
import com.soffid.iam.api.Host;
import com.soffid.iam.api.MailDomain;
import com.soffid.iam.api.MailList;
import com.soffid.iam.api.Network;
import com.soffid.iam.api.OUType;
import com.soffid.iam.api.OsType;
import com.soffid.iam.api.Role;
import com.soffid.iam.api.RoleGrant;
import com.soffid.iam.api.User;
import com.soffid.iam.api.UserData;
import com.soffid.iam.api.UserType;
import com.soffid.iam.lang.MessageFactory;
import com.soffid.iam.model.SystemEntity;
import com.soffid.iam.model.SystemEntityDao;
import com.soffid.iam.service.ApplicationService;
import com.soffid.iam.service.AuthorizationService;
import com.soffid.iam.service.GroupService;
import com.soffid.iam.utils.ConfigurationCache;
import com.soffid.iam.utils.MailUtils;
import com.soffid.iam.utils.Security;

import es.caib.seycon.ng.comu.TypeEnumeration;
import es.caib.seycon.ng.exception.InternalErrorException;

public class MailShortcut implements ActionHandler {
	public String getActors() {
		return actors;
	}

	public void setActors(String actors) {
		this.actors = actors;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public Log getLog() {
		return log;
	}

	public void setLog(Log log) {
		this.log = log;
	}
	private static final long serialVersionUID = 1L;

	protected ExecutionContext executionContext = null;

	// Template can be: 
	// Event.EVENTTYPE_TASK_ASSIGN ("task-assign")
	// "task-reminder"
	
	private String template;

	private String actors;

	private String to;

	private String subject;

	private String text;

	private String from;

	private HashMap<String, String> buttons;

	public String getTemplate ()
	{
		return template;
	}

	public void setTemplate (String template)
	{
		this.template = template;
	}

	public void initialize() {
		from = ConfigurationCache.getProperty("mail.from"); //$NON-NLS-1$
		if (from == null) from = "no-reply@soffid.com"; //$NON-NLS-1$
	}

	public MailShortcut() {
		initialize();
	}

    public MailShortcut(String template, String actors, String to, String subject, String text)
    {
        this.template = template;
        this.actors = actors;
        this.to = to;
        this.subject = subject;
        this.text = text;
        initialize();
    }


	@Override
	public void execute(ExecutionContext executionContext) throws Exception {
		this.__processInstanceId = executionContext.getProcessInstance().getId();
		this.executionContext = executionContext;
		send ();
	}
	
	public void calculateButtons() throws InternalErrorException {
		this.template = "shortcut"; //$NON-NLS-1$
		BpmUserService svc = (BpmUserService) ServiceLocator.instance().getService(BpmUserService.SERVICE_NAME);
		PageInfo pi = svc.getPageInfoByNodeId(executionContext.getNode().getId());
		buttons = new HashMap<String, String>();
		if (pi.getNodeType() == NodeType.NT_GRANT_SCREEN) {
			String acceptRandom = randomString();
			String name =  "actions_"+executionContext.getTaskInstance().getId()+"_"+acceptRandom; //$NON-NLS-1$ //$NON-NLS-2$
			String link = Long.toString(executionContext.getProcessInstance().getId())+"."+Long.toString(executionContext.getTaskInstance().getId())+"."+acceptRandom; //$NON-NLS-1$ //$NON-NLS-2$
			executionContext.setVariable(name, "approve:"+pi.getApproveTransition()); //$NON-NLS-1$
			buttons.put( Messages.getString("MailShortcut.Approve"), link); //$NON-NLS-1$
			String acceptRandom2 = randomString();
			String name2 =  "actions_"+executionContext.getTaskInstance().getId()+"_"+acceptRandom2; //$NON-NLS-1$ //$NON-NLS-2$
			String link2 = Long.toString(executionContext.getProcessInstance().getId())+"."+Long.toString(executionContext.getTaskInstance().getId())+"."+acceptRandom2; //$NON-NLS-1$ //$NON-NLS-2$
			executionContext.setVariable(name2, "deny:"+pi.getDenyTransition()); //$NON-NLS-1$
			buttons.put(Messages.getString("MailShortcut.Deny"), link2); //$NON-NLS-1$
		} else {
			for (Transition transition: executionContext.getNode().getLeavingTransitions()) {
				String r = randomString();
				String name = "actions_"+executionContext.getTaskInstance().getId()+"_"+r; //$NON-NLS-1$ //$NON-NLS-2$
				String link = Long.toString(executionContext.getProcessInstance().getId())+"."+Long.toString(executionContext.getTaskInstance().getId())+"."+r; //$NON-NLS-1$ //$NON-NLS-2$
				buttons.put(transition.getName(), link);
				executionContext.setVariable(name, ":"+transition.getName()); //$NON-NLS-1$
			}
		}
	}
	
	String randomString() {
		byte data[] = new byte[18];
		new SecureRandom().nextBytes(data);
		return Base64.getEncoder().encodeToString(data);
	}

	private ThreadLocal<Object> recursiveLock = new ThreadLocal<Object>();

	private String buttonsText;

	private String body;

	private DataType t;
	public void send() throws InternalErrorException, IOException, AddressException, NamingException, CreateException 
	{
		// Prevent recursive lock
		if (recursiveLock.get() != null)
			return;
		
		if (Event.EVENTTYPE_TASK_ASSIGN.equals(getTemplate()) )
		{
			if (executionContext.getTaskInstance() != null && 
					executionContext.getTask() != null &&
					executionContext.getTaskInstance().getActorId() == null &&
					(executionContext.getTaskInstance().getPooledActors() == null ||
						executionContext.getTaskInstance().getPooledActors().isEmpty() ))
			{
				recursiveLock.set(this);
				try {
					executionContext.getTaskInstance().assign(executionContext);
				} finally {
					recursiveLock.remove();
				}
			}
			template = "shortcut"; //$NON-NLS-1$
			sendPredefinedMail("Mail.4"); //$NON-NLS-1$
		} 
		else if ("delegate".equals(getTemplate()) ) //$NON-NLS-1$
		{
			template = "shortcut"; //$NON-NLS-1$
    		sendPredefinedMail("Mail.4"); //$NON-NLS-1$
		} 
		else if ("task-reminder".equals(getTemplate()) ) //$NON-NLS-1$
		{
			template = "shortcut"; //$NON-NLS-1$
    		sendPredefinedMail("Mail.8"); //$NON-NLS-1$
		}
		else
		{
    		sendCustomMail();
		}
	}

	private void sendPredefinedMail(String header) throws IOException,
			InternalErrorException, UnsupportedEncodingException, NamingException, CreateException {
		
		Locale previousLocale = MessageFactory.getThreadLocale();
		Security.nestedLogin("mail-server", new String[] { //$NON-NLS-1$
				Security.AUTO_USER_QUERY + Security.AUTO_ALL,
				Security.AUTO_USER_UPDATE + Security.AUTO_ALL,
				Security.AUTO_ROLE_QUERY + Security.AUTO_ALL,
				Security.AUTO_GROUP_QUERY + Security.AUTO_ALL,
				Security.AUTO_USER_ROLE_QUERY + Security.AUTO_ALL,
				Security.AUTO_ACCOUNT_QUERY + Security.AUTO_ALL,
				Security.AUTO_APPLICATION_QUERY + Security.AUTO_ALL});
		try {
			calculateButtons();
			for (String user: getUsers())
			{
				calculateBody(user);
				generateButtons(user);
				
				User usuari = ServiceLocator.instance().getUserService().findUserByUserName(user);
				
				Map<String, String> prefs = ServiceLocator.instance().getPreferencesService().findUserPreferences(user);
				String lang = prefs.get("lang"); //$NON-NLS-1$
				if (lang != null)
					MessageFactory.setThreadLocale(new Locale (lang));
				
				String subject = com.soffid.iam.bpm.mail.Messages.getString(header); //$NON-NLS-1$
				InputStream in = getMailContent();
				InputStreamReader reader = new InputStreamReader(in);
				StringBuffer buffer = new StringBuffer ();
				int ch = reader.read();
				while ( ch >= 0)
				{
					buffer.append((char) ch);
					ch = reader.read ();
				}
				
				text = buffer.toString();
				InternetAddress recipient = getUserAddress(usuari);
				if (recipient != null)
				{
					String fromDescription = null;
					String sender = Security.getCurrentUser();
					if (sender != null)
					{
						User u = ServiceLocator.instance().getUserService().findUserByUserName(sender);
						if (u != null)
							fromDescription = u.getFullName();
					}
					send(from, 
							Collections.singleton(recipient), 
							evaluate(subject, fromDescription, recipient.getPersonal()), 
							evaluate (text, fromDescription, recipient.getPersonal()));
				}
			}
		} finally {
			Security.nestedLogoff();
		}
		MessageFactory.setThreadLocale(previousLocale);
	}

	private void calculateBody(String user) throws InternalErrorException {
		BpmUserService svc = (BpmUserService) ServiceLocator.instance().getService(BpmUserService.SERVICE_NAME);
		PageInfo pi = svc.getPageInfoByNodeId(new Long( executionContext.getNode().getId()) );
		StringBuffer buffer = new StringBuffer();
		buffer.append("<table class='formdata'>"); //$NON-NLS-1$
		Arrays.sort(pi.getFields(), new Comparator<Field>() {
			public int compare(Field o1, Field o2) {
				return o1.getOrder().compareTo(o2.getOrder());
			}
			
		});
		for (com.soffid.iam.addons.bpm.common.Field field: pi.getFields()) {
			Attribute att = findAttribute (field, pi);
			if (att != null)
				addField(buffer, user, field, att);
		}
		buffer.append("</table>"); //$NON-NLS-1$
		body = buffer.toString();
	}

	private Attribute findAttribute(Field field, PageInfo pi) throws InternalErrorException {
		for (Attribute att: pi.getAttributes()) {
			if (att.getName().equals(field.getName())) {
				return att;
			}
		}
		Collection<DataType> dt = ServiceLocator.instance().getAdditionalDataService().findDataTypesByObjectTypeAndName2(User.class.getName(), field.getName());
		if (dt == null || dt.isEmpty())
			return null;
		t = dt.iterator().next();
		Attribute att = new Attribute();
		att.setDataObjectType(t.getDataObjectType());
		att.setLabel(field.getLabel() == null || field.getLabel().isEmpty() ? t.getLabel(): field.getLabel());
		att.setMultiValued(t.isMultiValued());
		att.setName(t.getName());
		att.setOrder(t.getOrder());
		att.setSize(t.getSize());
		att.setType(t.getType());
		att.setValues(t.getValues());
 		return att;
	}

	private void addField(StringBuffer buffer, String user, Field field, Attribute att) throws InternalErrorException {
		buffer.append("<tr><td>"); //$NON-NLS-1$
		if (att.getType() == TypeEnumeration.SEPARATOR) {
			buffer.append("<b>") //$NON-NLS-1$
				.append(quote(att.getLabel()))
				.append("</b>") //$NON-NLS-1$
				.append("</td><td>"); //$NON-NLS-1$
		} else if (att.getName().equals("grants")) { //$NON-NLS-1$
			buffer.append("<b>") //$NON-NLS-1$
			.append(quote(att.getLabel()))
			.append("</b>") //$NON-NLS-1$
			.append("</td><td>"); //$NON-NLS-1$
			List<RoleRequestInfo> grants = (List<RoleRequestInfo>) executionContext.getVariable(att.getName());
			addGrants(buffer, user, grants);
		} else {
			buffer.append("<tr><td style='color: #808080'>") //$NON-NLS-1$
				.append(quote(att.getLabel()))
				.append("</td><td>"); //$NON-NLS-1$
			Object value = executionContext.getVariable(att.getName());
			if (value != null &&  ! "".equals(value)) { //$NON-NLS-1$
				if (Boolean.TRUE.equals(att.getMultiValued()))
				{
					int i = 0;
					for (Object o: (Collection) value) {
						if (i > 0)
							buffer.append("<br>"); //$NON-NLS-1$
						i++;
						try {
							addFieldValue(buffer, user, att, o);
						} catch(Exception e) {}
					}
				} else {
					try {
						addFieldValue(buffer, user, att, value);
					} catch(Exception e) {}
				}
			}
		}
		buffer.append("</td></tr>"); //$NON-NLS-1$
	}

	private void addFieldValue(StringBuffer buffer, String user, Attribute att, Object value) throws InternalErrorException {
		String s = value.toString();
		if (att.getValues() != null && ! att.getValues().isEmpty()) {
			for (String option: att.getValues()) {
				int i = option.indexOf(":"); //$NON-NLS-1$
				String name;
				String literal;
				if (i > 0) {
					name = option.substring(0, i).trim();
					literal = option.substring(i+1).trim();
				} else {
					name = literal = option;
				}
				if (name.equals(s))
				{
					buffer.append(literal);
					return;
				}
			}
		}
		else if (att.getType() == TypeEnumeration.ACCOUNT_TYPE) {
			buffer.append(quote(s)).append(" "); //$NON-NLS-1$
			int i = s.lastIndexOf("@"); //$NON-NLS-1$
			if (i > 0) {
				Account account = ServiceLocator.instance().getAccountService().findAccount(s.substring(0,i), s.substring(i+1));
				if (account != null)
					buffer.append(" (").append(quote(account.getDescription())).append(")");
			}
		}
		else if (att.getType() == TypeEnumeration.APPLICATION_TYPE) {
			buffer.append(quote(s)).append(" "); //$NON-NLS-1$
			Application o = ServiceLocator.instance().getApplicationService().findApplicationByApplicationName(s);
			if (o != null)
				buffer.append(" (").append(quote(o.getDescription())).append(")");
		}
		else if (att.getType() == TypeEnumeration.BINARY_TYPE) {
		}
		else if (att.getType() == TypeEnumeration.BOOLEAN_TYPE) {
			if (Boolean.TRUE.equals(value))
				buffer.append("&#x2611;"); //$NON-NLS-1$
			else
				buffer.append("&#x2610;"); //$NON-NLS-1$
		}
		else if (att.getType() == TypeEnumeration.CUSTOM_OBJECT_TYPE) {
			buffer.append(quote(s)).append(" "); //$NON-NLS-1$
			CustomObject o = ServiceLocator.instance().getCustomObjectService().findCustomObjectByTypeAndName(att.getDataObjectType(), s);
			if (o != null)
				buffer.append(" (").append(quote(o.getDescription())).append(")");
		}
		else if (att.getType() == TypeEnumeration.DATE_TIME_TYPE) {
			Map<String, String> prefs = ServiceLocator.instance().getPreferencesService().findUserPreferences(user);
			String dateFormat = prefs.get("dateformat"); //$NON-NLS-1$
			String timeFormat = prefs.get("timeformat"); //$NON-NLS-1$
			String timeZone = prefs.get("timezone"); //$NON-NLS-1$
			String lang = prefs.get("lang"); //$NON-NLS-1$
			
			Locale locale = lang == null ? Locale.getDefault(): new Locale(lang);
			TimeZone tz = timeZone == null ? TimeZone.getDefault(): TimeZone.getTimeZone(timeZone);
			
			DateFormat df = dateFormat == null ? DateFormat.getDateInstance(DateFormat.SHORT, locale) : new SimpleDateFormat(dateFormat);
			df.setTimeZone(tz);
			DateFormat tf = timeFormat == null ? DateFormat.getTimeInstance(DateFormat.SHORT, locale) : new SimpleDateFormat(timeFormat);
			tf.setTimeZone(tz);

			if (value instanceof Calendar) {
				Date d = ((Calendar) value).getTime();
				buffer.append(df.format(d))
					.append(" ") //$NON-NLS-1$
					.append(tf.format(d));
			}
			else if (value instanceof Date) {
				Date d = (Date) value;
				buffer.append(df.format(d))
					.append(" ") //$NON-NLS-1$
					.append(tf.format(d));
			}
		}
		else if (att.getType() == TypeEnumeration.DATE_TYPE) {
			Map<String, String> prefs = ServiceLocator.instance().getPreferencesService().findUserPreferences(user);
			String dateFormat = prefs.get("dateformat"); //$NON-NLS-1$
			String timeFormat = prefs.get("timeformat"); //$NON-NLS-1$
			String timeZone = prefs.get("timezone"); //$NON-NLS-1$
			String lang = prefs.get("lang"); //$NON-NLS-1$
			
			Locale locale = lang == null ? Locale.getDefault(): new Locale(lang);
			TimeZone tz = timeZone == null ? TimeZone.getDefault(): TimeZone.getTimeZone(timeZone);
			
			DateFormat df = dateFormat == null ? DateFormat.getDateInstance(DateFormat.SHORT, locale) : new SimpleDateFormat(dateFormat);
			df.setTimeZone(tz);
			DateFormat tf = timeFormat == null ? DateFormat.getTimeInstance(DateFormat.SHORT, locale) : new SimpleDateFormat(timeFormat);
			tf.setTimeZone(tz);

			if (value instanceof Calendar) {
				Date d = ((Calendar) value).getTime();
				buffer.append(df.format(d));
			}
			else if (value instanceof Date) {
				Date d = (Date) value;
				buffer.append(df.format(d));
			}
		}
		else if (att.getType() == TypeEnumeration.EMAIL_TYPE) {
			buffer.append(quote(s));
		}
		else if (att.getType() == TypeEnumeration.GROUP_TYPE) {
			buffer.append(quote(s)).append(" "); //$NON-NLS-1$
			Group o = ServiceLocator.instance().getGroupService().findGroupByGroupName(s);
			if (o != null)
				buffer.append(" (").append(quote(o.getDescription())).append(")");
		}
		else if (att.getType() == TypeEnumeration.GROUP_TYPE_TYPE) {
			buffer.append(quote(s)).append(" "); //$NON-NLS-1$
			OUType o = ServiceLocator.instance().getOrganizationalUnitTypeService().findOUTypeByName(s);
			if (o != null)
				buffer.append(" (").append(quote(o.getDescription())).append(")");
		}
		else if (att.getType() == TypeEnumeration.HOST_TYPE) {
			buffer.append(quote(s)).append(" "); //$NON-NLS-1$
			Host o = ServiceLocator.instance().getNetworkService().findHostByName(s);
			if (o != null)
				buffer.append(" (").append(quote(o.getDescription())).append(")");
		}
		else if (att.getType() == TypeEnumeration.HTML) {
			buffer.append(s);
		}
		else if (att.getType() == TypeEnumeration.MAIL_DOMAIN_TYPE) {
			buffer.append(quote(s)).append(" "); //$NON-NLS-1$
			MailDomain o = ServiceLocator.instance().getMailListsService().findMailDomainByName(s);
			if (o != null)
				buffer.append(" (").append(quote(o.getDescription())).append(")");
		}
		else if (att.getType() == TypeEnumeration.MAIL_LIST_TYPE) {
			buffer.append(quote(s)).append(" "); //$NON-NLS-1$
			int i = s.lastIndexOf("@"); //$NON-NLS-1$
			if (i > 0) {
				MailList o = ServiceLocator.instance().getMailListsService().findMailListByNameAndDomainName(s.substring(0,i), s.substring(i+1));
				if (o != null)
					buffer.append(" (").append(quote(o.getDescription())).append(")");
			}
		}
		else if (att.getType() == TypeEnumeration.NETWORK_TYPE) {
			buffer.append(quote(s)).append(" "); //$NON-NLS-1$
			Network o = ServiceLocator.instance().getNetworkService().findNetworkByName(s);
			if (o != null)
				buffer.append(" (").append(quote(o.getDescription())).append(")");
		}
		else if (att.getType() == TypeEnumeration.NUMBER_TYPE) {
			buffer.append(quote(s));
		}
		else if (att.getType() == TypeEnumeration.OS_TYPE) {
			buffer.append(quote(s)).append(" "); //$NON-NLS-1$
			OsType o = ServiceLocator.instance().getNetworkService().findOSTypeByName(s);
			if (o != null)
				buffer.append(" (").append(quote(o.getDescription())).append(")");
		}
		else if (att.getType() == TypeEnumeration.PASSWORD_TYPE) {
			buffer.append("***************"); //$NON-NLS-1$
		}
		else if (att.getType() == TypeEnumeration.PHOTO_TYPE) {
			buffer.append(""); //$NON-NLS-1$
		}
		else if (att.getType() == TypeEnumeration.ROLE_TYPE) {
			buffer.append(quote(s)).append(" "); //$NON-NLS-1$
			int i = s.lastIndexOf("@"); //$NON-NLS-1$
			if (i > 0) {
				Role o = ServiceLocator.instance().getApplicationService().findRoleByNameAndSystem(s.substring(0,i), s.substring(i+1));
				if (o != null)
					buffer.append(" (").append(quote(o.getDescription())).append(")");
			}
		}
		else if (att.getType() == TypeEnumeration.SEPARATOR) {
		}
		else if (att.getType() == TypeEnumeration.USER_TYPE) {
			buffer.append(quote(s)).append(" "); //$NON-NLS-1$
			User o = ServiceLocator.instance().getUserService().findUserByUserName(s);
			if (o != null)
				buffer.append(" (").append(quote(o.getFullName())).append(")");
		}
		else if (att.getType() == TypeEnumeration.USER_TYPE_TYPE) {
			buffer.append(quote(s)).append(" "); //$NON-NLS-1$
			for (UserType ut: ServiceLocator.instance().getUserDomainService().findAllUserType()) {
				if (ut.getName().equals(s))
					buffer.append(" (").append(quote(ut.getDescription())).append(")");
			}
		} else {
			buffer.append(quote(s));
		}
	}

	private void addGrants(StringBuffer buffer, String user, List<RoleRequestInfo> value) throws InternalErrorException {
		buffer.append("<table class='perms'><tr><th>User</th><th>Application</th><th>Permission</th></tr>"); //$NON-NLS-1$
		for (RoleRequestInfo rri: value) {
			if (rri.getTaskInstance() != null && 
					rri.getTaskInstance().equals(executionContext.getTaskInstance().getId())) {
				Long roleId = rri.getRoleId();
				if (roleId == null) {
					Role role = ServiceLocator.instance().getApplicationService().findRoleById(rri.getPreviousRoleId());
					buffer.append("<tr><td><span style='color:red'>") //$NON-NLS-1$
						.append(com.soffid.iam.addons.bpm.handler.Messages.getString("MailShortcut.48")) //$NON-NLS-1$
						.append("</span> ") //$NON-NLS-1$
						.append(quote(rri.getUserName()))
						.append(" ") //$NON-NLS-1$
						.append(quote(rri.getUserFullName()))
						.append("</td><td>") //$NON-NLS-1$
						.append(quote(role.getInformationSystemName()))
						.append("</td><td>") //$NON-NLS-1$
						.append(quote(role.getName()))
						.append(" ") //$NON-NLS-1$
						.append(quote(rri.getRoleDescription()))
						.append("</td></tr>"); //$NON-NLS-1$
				} else {
					Role role = ServiceLocator.instance().getApplicationService().findRoleById(rri.getRoleId());
					buffer.append("<tr><td>") //$NON-NLS-1$
						.append(quote(rri.getUserName()))
						.append(" ") //$NON-NLS-1$
						.append(quote(rri.getUserFullName()))
						.append("</td><td>") //$NON-NLS-1$
						.append(quote(role.getInformationSystemName()))
						.append("</td><td>") //$NON-NLS-1$
						.append(quote(role.getName()))
						.append(" ") //$NON-NLS-1$
						.append(quote(rri.getRoleDescription()))
						.append("</td></tr>"); //$NON-NLS-1$
				}
			}
		}
		buffer.append("</table>"); //$NON-NLS-1$
	}

	public void generateButtons(String user) throws UnsupportedEncodingException {
		String hash = randomString();
		executionContext.setVariable("actions_user_translator_"+hash, user); //$NON-NLS-1$
		StringBuffer sb = new StringBuffer();
		List<String> keys = new LinkedList<String> (buttons.keySet());
		Collections.sort(keys);
		sb.append("<table class='buttonstable'><tr>");
		for (String key: keys) {
			String link = buttons.get(key);
			link = link + "."+hash; //$NON-NLS-1$

			sb.append("<td class='buttonstd'>");
			sb.append("<a class='buttonsa' href='"+quote(getExternalUrl()+"soffid/anonymous/bpm/action.zul?shortcut="+ URLEncoder.encode(link, "UTF-8"))+"'>");
			sb.append("<div class='buttonsdiv'>");
			sb.append(key);
			sb.append("</div>");
			sb.append("</a>");
			sb.append("</td>");
		}
		sb.append("</tr></table>");
		buttonsText = sb.toString();
	}

	private String quote(String string) {
		return string == null? "":
			string.replace("&", "&amp;") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\"", "&quot;") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("\'", "&apos;") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("<", "&lt;") //$NON-NLS-1$ //$NON-NLS-2$
				.replace(">", "&gt;"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private InputStream getMailContent ()
	{
		Locale locale = MessageFactory.getLocale();

		InputStream in = getClass().getResourceAsStream(template+"_"+locale.getLanguage()+"-custom.html"); //$NON-NLS-1$ //$NON-NLS-2$
		if (in == null)
			in = getClass().getResourceAsStream("/com/soffid/iam/bpm/mail/"+template+"-custom.html"); //$NON-NLS-1$ //$NON-NLS-2$
		if (in == null)
			in = getClass().getResourceAsStream("/com/soffid/iam/bpm/mail/"+template+"_"+locale.getLanguage()+"-custom.html"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (in == null)
			in = getClass().getResourceAsStream("/es/caib/bpm/mail/"+template+"_"+locale.getLanguage()+"-custom.html"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (in == null)
			in = getClass().getResourceAsStream("/es/caib/bpm/mail/"+template+"-custom.html"); //$NON-NLS-1$ //$NON-NLS-2$
		if (in == null)
			in = getClass().getResourceAsStream("/com/soffid/iam/bpm/mail/"+template+"_"+locale.getLanguage()+"-template.html"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (in == null)
			in = getClass().getResourceAsStream("/com/soffid/iam/bpm/mail/"+template+"-template.html"); //$NON-NLS-1$ //$NON-NLS-2$
		return in;
	}

	public void send(String fromAddress,
			Set<InternetAddress> targetAddresses, String subject, String text) {
		if ((targetAddresses == null) || (targetAddresses.isEmpty())) {
			debug(""); //$NON-NLS-1$
			return;
		}

		try {
			int retries = 5;
			while (0 < retries) {
				retries--;
				try {
					log.info("Sending mail ["+subject+"] to "+targetAddresses); //$NON-NLS-1$ //$NON-NLS-2$
					sendMailInternal(fromAddress,
							targetAddresses, subject, text);
					break;
				} catch (MessagingException msgex) {
					if (retries == 0)
						throw msgex;

					// System.out.println("Cannot send mail, now retrying: " +
					// msgex);
					error(String.format("", msgex));  //$NON-NLS-1$
					Thread.sleep(1000);
				}
			}
		} catch (Exception e) {
			throw new JbpmException("", e); //$NON-NLS-1$
		}
	}

	protected void sendMailInternal(String fromAddress, Set<InternetAddress> targetAddresses,
			String subject, String text) throws Exception {
		
		debug(String.format("", targetAddresses, subject)); //$NON-NLS-1$
		if (text != null && ! text.isEmpty())
		{
			while (subject != null && 
					(subject.startsWith(" ") || subject.startsWith("\t"))) //$NON-NLS-1$ //$NON-NLS-2$
					subject = subject.substring(1);
			subject = subject.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
			fromAddress = fromAddress.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
			MailUtils.sendHtmlMail(null, targetAddresses, fromAddress, subject, text);
		}
	}



	String evaluate(String expression, String from, String to) {
		if (expression == null) {
			return null;
		}
		VariableResolver variableResolver = JbpmExpressionEvaluator
				.getUsedVariableResolver();
		if (variableResolver != null) {
			Properties prop = new Properties();
			String externalURL = getExternalUrl();
			prop.put("AutoSSOURL", externalURL); //$NON-NLS-1$
			prop.put("buttons", buttonsText); //$NON-NLS-1$
			prop.put("body", body.toString()); //$NON-NLS-1$
			variableResolver = new MailVariableResolver(
					from, to,
					prop,
					variableResolver);
		}
		Object o = JbpmExpressionEvaluator.evaluate(expression,
				executionContext, variableResolver,
				JbpmExpressionEvaluator.getUsedFunctionMapper());
		String s = null;
		if (o == null ) s = null;
		else if (o instanceof Collection)
		{
			for (Object o2: (Collection) o)
			{
				if (s == null) s = o2.toString();
				else s = s +" "+o2.toString(); //$NON-NLS-1$
			}
		}
		else s = o.toString(); 
				
		log.info("Result: "+s); //$NON-NLS-1$
		return s;
	}

	public String getExternalUrl() {
		String externalURL = ConfigurationCache.getProperty("soffid.externalURL"); //$NON-NLS-1$
		if (externalURL == null)
			externalURL = ConfigurationCache.getProperty("AutoSSOURL"); //$NON-NLS-1$
		if (externalURL == null)
			externalURL = "http://localhost:8080/"; //$NON-NLS-1$
		if (!externalURL.endsWith("/")) //$NON-NLS-1$
			externalURL = externalURL + "/"; //$NON-NLS-1$
		return externalURL;
	}

	private void sendCustomMail() throws IOException,
	InternalErrorException, UnsupportedEncodingException, AddressException {
		Security.nestedLogin("mail-server", new String[] { //$NON-NLS-1$
				Security.AUTO_USER_QUERY + Security.AUTO_ALL,
				Security.AUTO_ROLE_QUERY + Security.AUTO_ALL,
				Security.AUTO_GROUP_QUERY + Security.AUTO_ALL,
				Security.AUTO_USER_ROLE_QUERY + Security.AUTO_ALL,
				Security.AUTO_ACCOUNT_QUERY + Security.AUTO_ALL,
				Security.AUTO_APPLICATION_QUERY + Security.AUTO_ALL});
		try {
			String realTo = evaluate(to, null, null);
			if (realTo != null)
			{
				Set<InternetAddress> users = new HashSet<InternetAddress>();
				for (String t: realTo.split("[, ]+")) //$NON-NLS-1$
				{
					if ( ! t.isEmpty())
						users.add(new InternetAddress(t));
				}

				String content;
				if (text != null && !text.trim().isEmpty())
					content = text;
				else if (template != null && !template.trim().isEmpty())
				{
					InputStream in = getMailContent();
					InputStreamReader reader = new InputStreamReader(in);
					StringBuffer buffer = new StringBuffer ();
					int ch = reader.read();
					while ( ch >= 0)
					{
						buffer.append((char) ch);
						ch = reader.read ();
					}
					content = buffer.toString();
				} else {
					content = subject;
				}

				for (InternetAddress user: users)
				{
					String fromDescription = null;
					String sender = Security.getCurrentUser();
					if (sender != null)
					{
						User u = ServiceLocator.instance().getUserService().findUserByUserName(sender);
						if (u != null)
							fromDescription = u.getFullName();
					}
					send(from, 
							Collections.singleton(user), 
							evaluate(subject, fromDescription, user.getPersonal()), 
							evaluate (text, fromDescription, user.getPersonal()));
				}
			}
			if (actors != null)
			{
				Set<String> users = new HashSet<String>();
				String actors2 = evaluate(actors, null, null);
				if (actors2 == null)
					return ;
				
				for (String t: actors2.split("[, ]+")) //$NON-NLS-1$
				{
					if ( ! t.isEmpty())
						users.addAll( getNameUsers(t));
				}
				for (String user: users)
				{
					
					User usuari = ServiceLocator.instance().getUserService().findUserByUserName(user);
			
					InternetAddress recipient = getUserAddress(usuari);
					if (recipient != null)
					{
						String content;
						if (text != null && !text.trim().isEmpty())
							content = text;
						else if (template != null && !template.trim().isEmpty())
						{
							InputStream in = getMailContent();
							InputStreamReader reader = new InputStreamReader(in);
							StringBuffer buffer = new StringBuffer ();
							int ch = reader.read();
							while ( ch >= 0)
							{
								buffer.append((char) ch);
								ch = reader.read ();
							}
							content = buffer.toString();
						} else {
							content = subject;
						}

						String fromDescription = null;
						String sender = Security.getCurrentUser();
						if (sender != null)
						{
							User u = ServiceLocator.instance().getUserService().findUserByUserName(sender);
							if (u != null)
								fromDescription = u.getFullName();
						}
						send(from, 
								Collections.singleton(recipient), 
								evaluate(subject, recipient.getPersonal(), fromDescription), 
								evaluate (content, recipient.getPersonal(), fromDescription));
					}
				}
			}
		} finally {
			Security.nestedLogoff();
		}
	}
		

	class MailVariableResolver implements VariableResolver, Serializable {
		private static final long serialVersionUID = 1L;
		Map templateVariables = null;
		VariableResolver variableResolver = null;
		private String to;
		private String from;

		public MailVariableResolver(
				String from,
				String to,
				Map templateVariables,
				VariableResolver variableResolver) {
			this.templateVariables = templateVariables;
			this.variableResolver = variableResolver;
			this.from = from;
			this.to = to;
		}

		public Object resolveVariable(String pName) throws ELException {
			if ((templateVariables != null)
					&& (templateVariables.containsKey(pName))) {
				return templateVariables.get(pName);
			}
			if (pName.equals("systemProperties")) //$NON-NLS-1$
				return System.getProperties();
			else if (pName.equals("from")) //$NON-NLS-1$
				return from;
			else if (pName.equals("to")) //$NON-NLS-1$
				return to;
			else
				return variableResolver.resolveVariable(pName);
		}
	}


	private long __processInstanceId = 0;

	private Log log = LogFactory.getLog(getClass());

	public void debug(String message) {
		log.info(
				"[" + __processInstanceId + "] " + message); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void error(String message, Throwable t) {
		log.error(
				"[" + __processInstanceId + "] " + message, t); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void error(String message) {
		log.error(
				"[" + __processInstanceId + "] " + message); //$NON-NLS-1$ //$NON-NLS-2$
	}

	
	public Set<String> getUsers () throws InternalErrorException, UnsupportedEncodingException
	{
		TaskInstance taskInstance = executionContext.getTaskInstance();
		if (taskInstance != null)
		{
			if (taskInstance.getActorId() != null)
			{
				return getNameUsers (taskInstance.getActorId());
			}
			else if (taskInstance.getSwimlaneInstance() != null)
			{
				SwimlaneInstance swimlane = taskInstance.getSwimlaneInstance();
				return getSwimlaneUsers(swimlane);
			}
			else if (taskInstance.getPooledActors() != null)
				return getNameUsers(taskInstance.getPooledActors());
		}
		return null;
	}

	private Set<String> getSwimlaneUsers (SwimlaneInstance swimlane) throws InternalErrorException, UnsupportedEncodingException
	{
		if (swimlane.getActorId() != null)
			return getNameUsers(swimlane.getActorId());
		else if (swimlane.getPooledActors() != null)
			return getNameUsers (swimlane.getPooledActors());
		else
			return null;
	}

	/**
	 * @param actorId
	 * @return
	 * @throws InternalErrorException 
	 * @throws UnsupportedEncodingException 
	 */
	private Set<String> getNameUsers (String actorId) throws InternalErrorException, UnsupportedEncodingException
	{
		HashSet<String> result = new HashSet<String>();
		if (actorId == null)
			return result;
		debug ("Resolving address for "+actorId); //$NON-NLS-1$
		if (actorId.startsWith("auth:")) //$NON-NLS-1$
		{
			String autorization = actorId.substring(5);
			String domain = null;
			int i = autorization.indexOf('/');
			if (i > 0)
			{
				domain = autorization.substring(i + 1);
				autorization = autorization.substring(0,i);
			}
			AuthorizationService autService = ServiceLocator.instance().getAuthorizationService();
			debug ("Resolving address for AUTHORIZATION "+autorization); //$NON-NLS-1$
			for (AuthorizationRole ar : autService.getAuthorizationRoles(autorization)) {
                String rol = ar.getRole().getName();
                if (domain != null) rol = rol + "/" + domain; //$NON-NLS-1$
                rol = rol + "@" + ar.getRole().getSystem(); //$NON-NLS-1$
                result.addAll(getNameUsers(rol));
            }
			return result;
			
		}
		Security.nestedLogin("mail-server", new String[] { //$NON-NLS-1$
						Security.AUTO_USER_QUERY + Security.AUTO_ALL,
						Security.AUTO_ROLE_QUERY + Security.AUTO_ALL,
						Security.AUTO_GROUP_QUERY + Security.AUTO_ALL,
						Security.AUTO_USER_ROLE_QUERY + Security.AUTO_ALL,
						Security.AUTO_ACCOUNT_QUERY + Security.AUTO_ALL,
						Security.AUTO_APPLICATION_QUERY + Security.AUTO_ALL});
		try {
    		User usuari = ServiceLocator.instance().getUserService().findUserByUserName(actorId);
    		if (usuari != null)
    		{
    			debug("Resolving address for user " + usuari.getUserName()); //$NON-NLS-1$
    			if (usuari.getActive().booleanValue())
    			{
    				result.add(usuari.getUserName());
    			}
    		}
    		else
    		{
    			GroupService gs = ServiceLocator.instance().getGroupService();
    			Group grup = gs.findGroupByGroupName(actorId);
    			if (grup != null)
    			{
    				StringBuffer sb = new StringBuffer();
        			debug("Resolving group members: " + grup.getName()); //$NON-NLS-1$
    				for (GroupUser ug : gs.findUsersBelongtoGroupByGroupName(actorId)) 
    				{
    					result.add( ug.getUser()) ;
    				}
    				return result;
    			}
    			else
    			{
    				int i = actorId.indexOf('@');
    				String roleName;
    				String dispatcher;
    				String scope = null;
    				if (i >= 0)
    				{
    					roleName = actorId.substring(0, i);
    					dispatcher = actorId.substring(i+1);
    				}
    				else
    				{
    					roleName = actorId;
    					SystemEntityDao dao = (SystemEntityDao) ServiceLocator.instance().getService("systemEntityDao"); //$NON-NLS-1$
						SystemEntity defaultDispatcher = dao.findSoffidSystem();
    					dispatcher = defaultDispatcher.getName();
    				}
    				
    				i = -1;
    				do {
    					i = roleName.indexOf('/', i+1);
	    				if (i >= 0)
	    				{
	    					scope = roleName.substring(i+1);
	    					roleName = roleName.substring(0, i);
	    				}
	        			debug ("Resolving role "+roleName+"@"+dispatcher); //$NON-NLS-1$ //$NON-NLS-2$
	    				ApplicationService aplicacioService = ServiceLocator.instance().getApplicationService();
						for (Role role : aplicacioService.findRolesByFilter(roleName, "%", "%", dispatcher, "%", "%")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	                        debug("Resolving role grantees: " + role.getName() + "@" + role.getSystem()); //$NON-NLS-1$ //$NON-NLS-2$
	                        for (RoleGrant grant : aplicacioService.findEffectiveRoleGrantsByRoleId(role.getId())) {
	                            if (scope == null || scope.equals(grant.getDomainValue())) {
	                            	if (grant.getUser() != null)
	                            		result.add(grant.getUser());
	                            }
	                        }
	                    }
    				} while (i >= 0);
    				return result;
    			}
    		}
			debug ("Unable to resolve address for "+actorId); //$NON-NLS-1$
    		return result;
		} finally {
			Security.nestedLogoff();
		}
		
	}

	
	private InternetAddress getUserAddress (User usuari) throws UnsupportedEncodingException, InternalErrorException
	{
		if (! usuari.getActive().booleanValue())
			return null;
		if (usuari.getShortName() != null && usuari.getMailDomain() != null)
		{
			return new InternetAddress( 
						usuari.getShortName()+"@"+usuari.getMailDomain(), //$NON-NLS-1$
						usuari.getFullName());
		}
		else
		{
			UserData dada = ServiceLocator.instance().getUserService().findDataByUserAndCode(usuari.getUserName(), "EMAIL"); //$NON-NLS-1$
			if (dada != null && dada.getValue() != null && ! dada.getValue().trim().isEmpty())
			{
				return new InternetAddress(dada.getValue(),
						usuari.getFullName());
			}
			else
				return null;
		}

	}
	/**
	 * @param pooledActors
	 * @return
	 * @throws InternalErrorException 
	 * @throws UnsupportedEncodingException 
	 */
	private Set<String> getNameUsers (Set<PooledActor> pooledActors) throws InternalErrorException, UnsupportedEncodingException
	{
		HashSet<String> result = new HashSet<String>();
		debug ("Resolving address for actor pool"); //$NON-NLS-1$
		for (PooledActor actor: pooledActors)
		{
			if (actor.getActorId() != null)
				result.addAll(getNameUsers(actor.getActorId()));
			else if (actor.getSwimlaneInstance() != null)
			{
				debug ("Resolving addres for swimlane "+actor.getSwimlaneInstance().getName()); //$NON-NLS-1$
				result.addAll(getSwimlaneUsers(actor.getSwimlaneInstance()));
			}
		}
		return result;
	}
}
