package com.soffid.iam.addons.bpm.model;

import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.mda.annotation.Column;
import com.soffid.mda.annotation.Depends;
import com.soffid.mda.annotation.Entity;
import com.soffid.mda.annotation.Identifier;
import com.soffid.mda.annotation.Nullable;

@Entity(table="SCB_FIELD")
@Depends({Field.class})
public class FieldEntity {
	@Column(name="FIE_ID")
	@Identifier
	@Nullable
	Long id;
	
	@Column(reverseAttribute="fields", name="FIE_NOD_ID")
	NodeEntity node;
	
	@Column(name="FIE_ORDER")
	Long order;
	
	@Nullable
	@Column(name="FIE_READON")
	Boolean readOnly;

	@Nullable
	@Column(name="FIE_LABEL")
	String label;

	@Column(name="FIE_NAME")
	String name;
	
	@Column(name="FIE_VALUES")
	@Nullable
	String allowedValues;
	
	@Column(name="FIE_VALIDA", length=128000)
	@Nullable
	String validationScript;
	
	@Column(name="FIE_VISIBI", length=128000)
	@Nullable
	String visibilityScript;
	
}
