package com.soffid.iam.addons.bpm.model;

import com.soffid.iam.addons.bpm.common.Attribute;
import com.soffid.mda.annotation.Column;
import com.soffid.mda.annotation.Depends;
import com.soffid.mda.annotation.Description;
import com.soffid.mda.annotation.Entity;
import com.soffid.mda.annotation.Identifier;
import com.soffid.mda.annotation.Nullable;

@Entity (table="SCB_ATTRIB" )
@Depends ({Attribute.class})
public abstract class AttributeEntity {
	@Column (name="ATT_ID")
	@Identifier
	public java.lang.Long id;

	@Column (name="ATT_NAME", length=25, translated="name")
	public java.lang.String name;

	@Column (name="ATT_ORDER", translated="order")
	public java.lang.Long order;

	@Column (name="ATT_TYPE", length=50)
	@Nullable
	public es.caib.seycon.ng.comu.TypeEnumeration type;

	@Column (name="ATT_DAOBTY", length=50)
	@Nullable
	public String dataObjectType;

	@Column (name="ATT_SIZE")
	@Nullable
	public java.lang.Integer size;
	
	@Column(name = "ATT_MULTIV")
	@Nullable
	public Boolean multiValued;

	@Description ("blank separated list of url-encoded values")
	@Column (name="ATT_VALUE", length=64000)
	@Nullable
	public String values;

	@Description("Label to display")
	@Column (name="ATT_LABEL", length=64)
	@Nullable
	public String label;
	
	@Column (name="ATT_PRO_ID", reverseAttribute="attributes")
	ProcessEntity process;

}

