package com.soffid.iam.addons.bpm.common;

import com.soffid.mda.annotation.Identifier;
import com.soffid.mda.annotation.Nullable;
import com.soffid.mda.annotation.ValueObject;

@ValueObject
public class InvocationField {
	@Nullable @Identifier
	Long id;
	
	@Nullable
	String field;
	
	@Nullable
	String expression;
}
