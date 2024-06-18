package com.soffid.iam.addons.bpm.common;

import java.util.List;

import com.soffid.mda.annotation.Attribute;
import com.soffid.mda.annotation.Column;
import com.soffid.mda.annotation.Nullable;
import com.soffid.mda.annotation.ValueObject;

@ValueObject
public class Process {
	@Nullable
	Long id;
	
	@Nullable
	@Column(name="PRO_TYPE")
	WorkflowType type;
	
	@Column(name="PRO_NAME")
	String name;
	
	@Nullable
	String description;
	
	@Nullable
	String initiators;
	
	@Nullable
	String observers;
	
	@Nullable
	String managers;

	@Nullable
	String diagram;

	@Attribute(type = "PHOTO")
	@Nullable
	byte[] image;

	@Nullable
	@Attribute(defaultValue="new java.util.LinkedList<com.soffid.iam.addons.bpm.common.Node>()")
	List<Node> nodes;
	
	@Nullable
	@Attribute(defaultValue="new java.util.LinkedList<com.soffid.iam.addons.bpm.common.Attribute>()")
	List<com.soffid.iam.addons.bpm.common.Attribute> attributes;
	
}
