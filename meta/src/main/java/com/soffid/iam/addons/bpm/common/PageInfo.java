package com.soffid.iam.addons.bpm.common;

import com.soffid.mda.annotation.ValueObject;

@ValueObject
public class PageInfo {
	NodeType nodeType;
	
	WorkflowType workflowType;
	
	Field[] fields;
	
	Trigger[] triggers;
	
	Attribute[] attributes;
}
