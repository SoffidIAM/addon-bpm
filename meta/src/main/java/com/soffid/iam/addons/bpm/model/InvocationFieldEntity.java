package com.soffid.iam.addons.bpm.model;

import com.soffid.iam.addons.bpm.common.InvocationField;
import com.soffid.mda.annotation.Column;
import com.soffid.mda.annotation.Depends;
import com.soffid.mda.annotation.Entity;
import com.soffid.mda.annotation.Identifier;
import com.soffid.mda.annotation.Nullable;

@Entity(table="SCB_INVFIE")
@Depends({InvocationField.class})
public class InvocationFieldEntity {
	@Nullable @Identifier @Column(name = "IFI_ID")
	Long id;
	
	@Column(name="IFI_NOD_ID", reverseAttribute = "invocationFields")
	NodeEntity node;
	
	@Nullable @Column(name="IFI_FIELD")
	String field;
	
	@Nullable @Column(name="IFI_EXPRES")
	String expression;
}
