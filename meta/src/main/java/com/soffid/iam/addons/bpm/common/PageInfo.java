package com.soffid.iam.addons.bpm.common;

import com.soffid.mda.annotation.Nullable;
import com.soffid.mda.annotation.ValueObject;

@ValueObject
public class PageInfo {
	NodeType nodeType;
	
	WorkflowType workflowType;
	
	Field[] fields;
	
	Filter[] filters;
	
	Trigger[] triggers;
	
	Attribute[] attributes;

	@Nullable
	Long matchThreshold;

	@Nullable
	String approveTransition;

	@Nullable
	String denyTransition;
}
