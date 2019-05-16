package com.soffid.iam.addons.bpm.common;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.soffid.iam.addons.bpm.core.ejb.BpmEditorService;
import com.soffid.iam.addons.bpm.core.ejb.BpmEditorServiceHome;
import com.soffid.iam.addons.bpm.core.ejb.BpmUserService;
import com.soffid.iam.addons.bpm.core.ejb.BpmUserServiceHome;

public class EJBLocator {
	public static BpmEditorService getBpmEditorService () throws NamingException {
		return (BpmEditorService) new InitialContext().lookup( BpmEditorServiceHome.JNDI_NAME );
	}
	
	public static BpmUserService getBpmUserService () throws NamingException {
		return (BpmUserService) new InitialContext().lookup(BpmUserServiceHome.JNDI_NAME);
	}
}
