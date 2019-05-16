package com.soffid.iam.addons.bpm.model;

import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.Trigger;
import com.soffid.mda.annotation.Column;
import com.soffid.mda.annotation.Depends;
import com.soffid.mda.annotation.Entity;
import com.soffid.mda.annotation.Identifier;
import com.soffid.mda.annotation.Nullable;

@Entity(table="SCB_TRIGGER")
@Depends({Trigger.class})
public class TriggerEntity {
	@Column(name="TRG_ID")
	@Identifier
	@Nullable
	Long id;
	
	@Column(reverseAttribute="triggers", name="TRG_NOD_ID")
	NodeEntity node;
	
	@Column(name="TRG_NAME")
	String name;
	
	@Nullable
	@Column(name="TRG_FIELD")
	String field;

	@Nullable
	@Column(name="TRG_ACTION", length=128000)
	String action;
	
}
