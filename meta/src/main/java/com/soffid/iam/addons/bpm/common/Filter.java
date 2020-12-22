package com.soffid.iam.addons.bpm.common;

import com.soffid.mda.annotation.Column;
import com.soffid.mda.annotation.Nullable;
import com.soffid.mda.annotation.ValueObject;

@ValueObject
public class Filter {
	@Nullable
	Long id;
	
	@Nullable
	Long weight;
	
	@Nullable
	String type;

	@Nullable
	String query;
}
