package com.soffid.iam.addons.bpm.common;

import com.soffid.mda.annotation.Enumeration;

@Enumeration
public class WorkflowType {
	public static String WT_USER = "user";
	public static String WT_PERMISSION = "permission";
	public static String WT_REQUEST = "request";
	public static String WT_ACCOUNT_RESERVATION = "account-reservation";
	public static String WT_DELEGATION = "delegation";
}
