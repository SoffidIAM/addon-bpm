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
	
	@Column(name="NOD_TASNAM")
	@Nullable
	String taskName;
	
	@Column(name="NOD_DESCRI", length = 4000)
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
	
	@Column(name="NOD_MTCTHR")
	@Nullable
	Long matchThreshold;

	@Column(name="NOD_GRSCTY")
	@Nullable
	@Description("Admits four values: request / enter / displayApproved / displayPendig / displayRejected")
	String grantScreenType;
	
	@Column(name="NOD_APLUSE")
	@Nullable
	Boolean applyUserChanges;
	
	@Column(name="NOD_APLENT")
	@Nullable
	Boolean applyEntitlements;
	
	@Column(name="NOD_MAISHC")
	@Nullable
	Boolean mailShortcut;
	
	@Column(name="NOD_APRTRA")
	@Nullable
	String approveTransition;

	@Column(name="NOD_DENTRA")
	@Nullable
	String denyTransition;

	@Column(name="NOD_UPLDOC")
	@Nullable
	Boolean uploadDocuments;
	
	@Column(name="NOD_ROLFIL")
	@Nullable
	String roleFilter;
	
	@Column(name="NOD_APLFIL")
	@Nullable
	String applicationFilter;
	
	@Nullable
	@Column(name="NOD_SYSTEM")
	@Description("System to invoke")
	String system;

	@Nullable
	@Column(name="NOD_METHOD")
	@Description("Method to execute")
	String method;

	@Nullable
	@Column(name="NOD_PATH")
	@Description("Path to execute")
	String path;

	@Nullable
	@Column(name="NOD_TARVAR")
	@Description("Variable to fetch results")
	String returnVariable;
	
	@Nullable
	@Column(name="NOD_DIAID")
	@Description("Diagram id")
	String diagramId;
}
