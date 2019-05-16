package com.soffid.iam.addons.bpm.model;

import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.Node;
import com.soffid.iam.addons.bpm.common.NodeType;
import com.soffid.iam.addons.bpm.common.WorkflowType;
import com.soffid.mda.annotation.Column;
import com.soffid.mda.annotation.Depends;
import com.soffid.mda.annotation.Description;
import com.soffid.mda.annotation.Entity;
import com.soffid.mda.annotation.Identifier;
import com.soffid.mda.annotation.Nullable;

@Entity(table="SCB_NODE")
@Depends({Node.class})
public class NodeEntity {
	@Column(name="NOD_ID")
	@Identifier
	@Nullable
	Long id;
	
	@Column(reverseAttribute="nodes", name="NOD_PRO_ID")
	ProcessEntity process;
	
	@Column(name="NOD_TYPE")
	NodeType type;
	
	@Column(name="NOD_NAME")
	String name;
	
	@Column(name="NOD_DESCRI")
	@Nullable
	String description;
	
	@Column(name="NOD_SCRIPT", length=128000)
	@Nullable
	String customScript;
	
	@Column(name="NOD_SUBJEC")
	@Nullable
	String mailSubject;
	
	@Column(name="NOD_MAIACT")
	@Nullable
	String mailActor;
	
	@Column(name="NOD_MAIADR")
	@Nullable
	String mailAddress;
	
	@Column(name="NOD_MAIMSG", length=2000)
	@Nullable
	String mailMessage;
	
	@Column(name="NOD_GRSCTY")
	@Nullable
	@Description("Admits three values: enter / approve / review")
	String grantScreenType;
	
	@Column(name="NOD_APLUSE")
	@Nullable
	Boolean applyUserChanges;
	
	@Column(name="NOD_APLENT")
	@Nullable
	Boolean applyEntitlements;
	
}
