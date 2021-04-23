package com.soffid.iam.addons.bpm.core;

import com.soffid.iam.addons.bpm.anyone;
import com.soffid.iam.addons.bpm.common.PageInfo;
import com.soffid.iam.addons.bpm.model.FieldEntity;
import com.soffid.iam.addons.bpm.model.NodeEntity;
import com.soffid.iam.addons.bpm.model.ProcessEntity;
import com.soffid.iam.addons.bpm.model.TransitionEntity;
import com.soffid.iam.common.security.SoffidPrincipal;
import com.soffid.mda.annotation.Depends;
import com.soffid.mda.annotation.Description;
import com.soffid.mda.annotation.Operation;
import com.soffid.mda.annotation.Service;

import es.caib.bpm.servei.BpmEngine;
import es.caib.bpm.vo.ProcessInstance;
import es.caib.bpm.vo.TaskInstance;
import es.caib.seycon.ng.model.UsuariEntity;
import es.caib.seycon.ng.servei.AccountService;
import es.caib.seycon.ng.servei.AutoritzacioService;
import es.caib.seycon.ng.servei.DispatcherService;

@Service(grantees= {anyone.class})
@Depends({FieldEntity.class, NodeEntity.class, ProcessEntity.class, TransitionEntity.class, BpmEngine.class, UsuariEntity.class,
	AccountService.class,
	BpmEditorService.class,
	AutoritzacioService.class,
	DispatcherService.class})
public class BpmUserService {
	public PageInfo getPageInfo (TaskInstance task) { return null; }

	public PageInfo getPageInfo (ProcessInstance task) { return null; }

	public PageInfo getPageInfoByNodeId (Long nodeId) { return null; }

	@Description("Returns approve deny or null depending on the type of action")
	public String processAnonymousAction (String hash) {return null;}

	@Description("Returns the owner of this action")
	public SoffidPrincipal getAnonymousActionPrincipal (String hash) {return null;}
}
