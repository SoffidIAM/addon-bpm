package com.soffid.iam.addons.bpm.core;

import java.util.List;

import com.soffid.iam.addons.bpm.model.AttributeEntity;
import com.soffid.iam.addons.bpm.model.FieldEntity;
import com.soffid.iam.addons.bpm.model.FilterEntity;
import com.soffid.iam.addons.bpm.model.InvocationFieldEntity;
import com.soffid.iam.addons.bpm.model.NodeEntity;
import com.soffid.iam.addons.bpm.model.ProcessEntity;
import com.soffid.iam.addons.bpm.model.TransitionEntity;
import com.soffid.iam.addons.bpm.model.TriggerEntity;
import com.soffid.iam.addons.bpm.role.Editor;
import com.soffid.iam.addons.bpm.role.Publisher;
import com.soffid.mda.annotation.Depends;
import com.soffid.mda.annotation.Operation;
import com.soffid.mda.annotation.Service;
import com.soffid.iam.addons.bpm.common.Node;
import com.soffid.iam.addons.bpm.common.Process;

import es.caib.bpm.servei.BpmEngine;
import es.caib.bpm.vo.TaskInstance;
import es.caib.seycon.ng.model.AuditoriaEntity;
import es.caib.seycon.ng.model.UsuariEntity;
import roles.Tothom;

@Service(grantees= {Editor.class, Publisher.class})
@Depends({FieldEntity.class, NodeEntity.class, ProcessEntity.class, TransitionEntity.class, 
	BpmEngine.class, UsuariEntity.class, AttributeEntity.class, TriggerEntity.class,
	FilterEntity.class,	AuditoriaEntity.class, InvocationFieldEntity.class})
public class BpmEditorService {
	@Operation(grantees= {Editor.class})
	public List<Process> findAll(){ return null;}
	
	@Operation(grantees= {Editor.class})
	public List<Process> findByName(String name){ return null;}

	@Operation(grantees= {Editor.class})
	public Process findById(Long id){ return null;}

	@Operation(grantees= {Editor.class})
	public Process create(Process process){ return null;}

	@Operation(grantees= {Editor.class})
	public Process update(Process process){ return null;}

	@Operation(grantees= {Editor.class})
	public void remove(Process process){ }

	@Operation(grantees= {Publisher.class})
	public void publish(Process process){ }

	@Operation(grantees= {Tothom.class})
	public Node getTaskNode(TaskInstance task){ return null; }
}
