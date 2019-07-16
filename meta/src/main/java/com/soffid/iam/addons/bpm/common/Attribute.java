package com.soffid.iam.addons.bpm.common;

import java.util.List;

import com.soffid.mda.annotation.Column;
import com.soffid.mda.annotation.Description;
import com.soffid.mda.annotation.Nullable;
import com.soffid.mda.annotation.ValueObject;

@ValueObject
public class Attribute {
	@Nullable
	public java.lang.Long id;

	public java.lang.String name;

	@Nullable
	@com.soffid.mda.annotation.Attribute(defaultValue="0L")
	public java.lang.Long order;

	public es.caib.seycon.ng.comu.TypeEnumeration type;
	
	@Nullable
	public String dataObjectType;

	@Nullable
	public java.lang.Integer size;
	
	public Boolean multiValued;

	@Nullable
	@Description("List of allowed values")
	@com.soffid.mda.annotation.Attribute(defaultValue="new java.util.LinkedList<String>()")
	public List<String> values;

	@Description("Label to display")
	@Nullable
	public String label;
}

