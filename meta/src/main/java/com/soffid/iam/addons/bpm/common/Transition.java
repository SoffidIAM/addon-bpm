package com.soffid.iam.addons.bpm.common;

import com.soffid.mda.annotation.Column;
import com.soffid.mda.annotation.Identifier;
import com.soffid.mda.annotation.Nullable;
import com.soffid.mda.annotation.ValueObject;

@ValueObject
public abstract class Transition {
	@Nullable
	Long id;
	
	@Nullable
	Node source;
	
	@Nullable
	Node target;
	
	@Nullable
	String name;
	
	@Nullable
	String script;
	
	/**
	 * Custom method to prevent loops
	 */
	public String toString() {return null;}
}
