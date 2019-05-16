package com.soffid.iam.addons.bpm.common;

import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.mda.annotation.Column;
import com.soffid.mda.annotation.Depends;
import com.soffid.mda.annotation.Entity;
import com.soffid.mda.annotation.Identifier;
import com.soffid.mda.annotation.Nullable;
import com.soffid.mda.annotation.ValueObject;

@ValueObject
public class Trigger {
	@Identifier
	@Nullable
	Long id;
	
	@Column(name="TRG_NAME")
	String name;
	
	@Nullable
	String field;

	@Nullable
	String action;
	
}
