package com.soffid.iam.addons.bpm.model;

import java.util.List;

import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.WorkflowType;
import com.soffid.iam.model.TenantEntity;
import com.soffid.mda.annotation.Column;
import com.soffid.mda.annotation.Depends;
import com.soffid.mda.annotation.Entity;
import com.soffid.mda.annotation.Identifier;
import com.soffid.mda.annotation.Nullable;

@Entity(table="SCB_PROCES")
@Depends({com.soffid.iam.addons.bpm.common.Process.class, 
	NodeEntity.class, TransitionEntity.class})
public class ProcessEntity {
	@Column(name="PRO_ID")
	@Identifier
	@Nullable
	Long id;
	
	@Column(name="PRO_TEN_ID")
	TenantEntity tenant;
	
	@Column(name="PRO_TYPE")
	WorkflowType type;
	
	@Column(name="PRO_NAME")
	String name;
	
	@Column(name="PRO_DESCRI", length = 250)
	@Nullable
	String description;
	
	@Column(name="PRO_PRODEF")
	@Nullable
	Long processDefinition;
	
	@Column(name="PRO_INITIA")
	@Nullable
	String initiators;
	
	@Column(name="PRO_OBSERV")
	@Nullable
	String observers;
	
	@Column(name="PRO_MANAGE")
	@Nullable
	String managers;
	
	@Column(name="PRO_VERSION")
	@Nullable
	Long version;

	public List<ProcessEntity> findByName(String name){ return null;}
	
}
