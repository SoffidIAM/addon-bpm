package com.soffid.iam.addons.bpm.web;

import java.io.IOException;

import javax.ejb.CreateException;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.LogFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.au.out.AuScript;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.ext.AfterCompose;
import org.zkoss.zul.Timer;

import com.soffid.iam.EJBLocator;
import com.soffid.iam.bpm.api.TaskInstance;
import com.soffid.iam.bpm.service.ejb.BpmEngine;
import com.soffid.iam.common.security.SoffidPrincipal;
import com.soffid.iam.utils.Security;
import com.soffid.iam.web.bpm.WorkflowWindowInterface;

import es.caib.bpm.exception.BPMException;
import es.caib.seycon.ng.exception.InternalErrorException;
import es.caib.zkib.zkiblaf.Application;
import es.caib.zkib.zkiblaf.Missatgebox;

public class TaskUI extends com.soffid.iam.web.bpm.TaskUI implements AfterCompose {

	private String shortcut;
	private SoffidPrincipal principal;

	public TaskUI() throws InternalErrorException, NamingException, CreateException {
		super();
		HttpServletRequest req = (HttpServletRequest) Executions.getCurrent().getNativeRequest();
		shortcut = req.getParameter("shortcut"); //$NON-NLS-1$

	}

	public void onCreate() {
	}
	@Override
	public void afterCompose() {
		super.afterCompose();
		try {
			super.onCreate();
			if (shortcut != null) {
				String[] split = shortcut.split("\\.");
				if (split.length > 1) {
					TaskInstance ti;
					String id = split[1];
					Long taskId = Long.decode(id);
	
					Security.nestedLogin(Security.ALL_PERMISSIONS);
					try {
						principal = com.soffid.iam.addons.bpm.common.EJBLocator.getBpmUserService().getAnonymousActionPrincipal(shortcut);
					} finally {
						Security.nestedLogoff();
					}
					if (principal != null) {
						com.soffid.iam.utils.Security.nestedLogin(principal);
						try {
							BpmEngine engine = EJBLocator.getBpmEngine();
							
							ti = engine.getTask(taskId);
							if (ti != null) {
								openTaskInstance(ti);
								processShortcut2();
//								Timer t = (Timer) getFellow("timer");
//								t.setRepeats(true);
//								t.start();
								return;
							}
						} finally {
							com.soffid.iam.utils.Security.nestedLogoff();
						}
					}
				}
			}
		} catch (Exception e) {
			LogFactory.getLog(getClass()).warn("Error loading anonymous task", e);
		}
		setVisible(false);
		Missatgebox.avis(Labels.getLabel("bpm.alreadyClosed"));
		
	}
	
	public void processShortcut(Event event) throws ClassNotFoundException, IOException, Exception {
		Timer t = (Timer) getFellow("timer");
		if (!t.isRunning()) return;
		t.stop();
		processShortcut2();
	}
	
	public void processShortcut2() throws ClassNotFoundException, IOException, Exception {
		if (getCurrentTask().getEnd() != null) {
			Missatgebox.avis(Labels.getLabel("bpm.alreadyClosed"));
		} else {
			String action ;
			com.soffid.iam.utils.Security.nestedLogin(principal);
			try {
				action = com.soffid.iam.addons.bpm.common.EJBLocator.getBpmUserService().processAnonymousAction(shortcut);
			} finally {
				com.soffid.iam.utils.Security.nestedLogoff();
			}
			Security.nestedLogin(new String[] {Security.AUTO_AUTHORIZATION_ALL, "BPM_INTERNAL"});
			try {
				BpmEngine engine = EJBLocator.getBpmEngine();
				
				TaskInstance ti = engine.getTask(getCurrentTask().getId());
				if (ti != null) 
					openTaskInstance(ti);
			} finally {
				Security.nestedLogoff();
			}
			String msg = Labels.getLabel( "approve".equals(action) ? "bpm.approved" :
				"deny".equals(action) ? "bpm.denied" :
					"bpm.done");
			Missatgebox.avis(msg, ev -> {
			});
		}
	}

    public void updateBotonera(WorkflowWindowInterface componenteGenerado) {
    	// Do nothing
    }

}
