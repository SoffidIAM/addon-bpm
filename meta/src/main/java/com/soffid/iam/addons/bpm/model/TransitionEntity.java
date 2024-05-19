package com.soffid.iam.addons.bpm.model;

import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.Transition;
import com.soffid.mda.annotation.Column;
import com.soffid.mda.annotation.Depends;
import com.soffid.mda.annotation.Entity;
import com.soffid.mda.annotation.Identifier;
import com.soffid.mda.annotation.Nullable;

@Entity(table="SCB_TRANSI")
@Depends({Transition.class})
public class TransitionEntity {
	@Column(name="TRA_ID")
	@Identifier
	@Nullable
	Long id;
	
	@Column(reverseAttribute="outTransitions", name="TRA_SRCNOD_ID")
	NodeEntity source;
	
	@Column(reverseAttribute="inTransitions", name="TRA_TRGNOD_ID")
	NodeEntity target;
	
	@Column(name="TRA_NAME")
	@Nullable
	String name;
	
	@Column(name="TRA_SCRIPT", length=64000)
	@Nullable
	String script;
	
	@Column(name="TRA_DIAID")
	@Nullable
	String diagramId;
}
