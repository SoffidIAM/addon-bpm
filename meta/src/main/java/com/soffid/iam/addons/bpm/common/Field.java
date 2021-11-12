package com.soffid.iam.addons.bpm.common;

import com.soffid.mda.annotation.Column;
import com.soffid.mda.annotation.Nullable;
import com.soffid.mda.annotation.ValueObject;

@ValueObject
public class Field {
	@Nullable
	Long id;
	
	@Nullable
	Long order;
	
	@Nullable
	String label;

	@Nullable
	String name;
	
	@Nullable
	Boolean readOnly;

	@Nullable
	Boolean required;

	@Nullable
	String allowedValues;
	
	@Nullable
	String validationScript;
	
	@Nullable
	String visibilityScript;
	
}
