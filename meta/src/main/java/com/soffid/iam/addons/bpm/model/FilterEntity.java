package com.soffid.iam.addons.bpm.model;

import com.soffid.iam.addons.bpm.common.Filter;
import com.soffid.mda.annotation.Column;
import com.soffid.mda.annotation.Depends;
import com.soffid.mda.annotation.Entity;
import com.soffid.mda.annotation.Identifier;
import com.soffid.mda.annotation.Nullable;

@Entity(table="SCB_FILTER")
@Depends({Filter.class})
public class FilterEntity {
	@Column(name="FIL_ID")
	@Identifier
	@Nullable
	Long id;
	
	@Column(reverseAttribute="filters", name="FIL_NOD_ID")
	NodeEntity node;
	
	@Column(name="FIL_WEIGHT")
	Long weight;
	
	@Nullable
	@Column(name="FIL_TYPE")
	String type;

	@Column(name="FIL_QUERY")
	String query;
	
}
