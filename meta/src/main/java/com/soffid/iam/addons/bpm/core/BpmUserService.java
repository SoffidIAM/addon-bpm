package com.soffid.iam.addons.bpm.core;

import com.soffid.iam.addons.bpm.anyone;
import com.soffid.iam.addons.bpm.common.PageInfo;
import com.soffid.iam.addons.bpm.model.FieldEntity;
import com.soffid.iam.addons.bpm.model.NodeEntity;
import com.soffid.iam.addons.bpm.model.ProcessEntity;
import com.soffid.iam.addons.bpm.model.TransitionEntity;
import com.soffid.mda.annotation.Depends;
import com.soffid.mda.annotation.Service;

import es.caib.bpm.servei.BpmEngine;
import es.caib.bpm.vo.ProcessInstance;
import es.caib.bpm.vo.TaskInstance;
import es.caib.seycon.ng.model.UsuariEntity;

@Service(grantees= {anyone.class})
@Depends({FieldEntity.class, NodeEntity.class, ProcessEntity.class, TransitionEntity.class, BpmEngine.class, UsuariEntity.class,
	BpmEditorService.class})
public class BpmUserService {
	public PageInfo getPageInfo (TaskInstance task) { return null; }

	public PageInfo getPageInfo (ProcessInstance task) { return null; }
}
