package com.soffid.iam.addons.bpm.common;

import java.util.List;

import com.soffid.iam.addons.bpm.common.NodeType;
import com.soffid.iam.addons.bpm.common.WorkflowType;
import com.soffid.mda.annotation.Attribute;
import com.soffid.mda.annotation.Column;
import com.soffid.mda.annotation.Description;
import com.soffid.mda.annotation.Entity;
import com.soffid.mda.annotation.Identifier;
import com.soffid.mda.annotation.Nullable;
import com.soffid.mda.annotation.ValueObject;

@ValueObject
public class Node {
	@Identifier
	@Nullable
	Long id;
	
	@Nullable
	@Column(name="NOD_TYPE")
	NodeType type;
	
	@Nullable
	@Column(name="NOD_NAME")
	String name;
	
	@Nullable
	String taskName;

	@Nullable
	String description;
	
	@Nullable
	String customScript;

	@Nullable
	String mailSubject;
	
	@Nullable
	String mailActor;
	
	@Nullable
	String mailAddress;
	
	@Nullable
	String mailMessage;

	@Nullable
	Long matchThreshold;

	@Nullable
	@Description("Admits three values: request / enter / approve / review")
	@Attribute(defaultValue = "\"request\"")
	String grantScreenType;
	
	@Nullable
	@Attribute(defaultValue="new java.util.LinkedList()")
	List<Field> fields;

	@Nullable
	@Attribute(defaultValue="new java.util.LinkedList()")
	List<Filter> filters;

	@Nullable
	@Attribute(defaultValue="new java.util.LinkedList()")
	List<Trigger> triggers;

	@Nullable
	@Attribute(defaultValue="new java.util.LinkedList()")
	List<Transition> inTransitions;

	@Nullable
	@Attribute(defaultValue="new java.util.LinkedList()")
	List<Transition> outTransitions;

	@Nullable
	Boolean applyUserChanges;
	
	@Nullable
	Boolean applyEntitlements;

	@Nullable
	Boolean mailShortcut;
	
	@Nullable
	String approveTransition;

	@Nullable
	String denyTransition;

	@Nullable
	Boolean uploadDocuments;
}
