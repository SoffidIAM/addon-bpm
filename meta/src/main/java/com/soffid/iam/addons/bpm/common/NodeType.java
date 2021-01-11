package com.soffid.iam.addons.bpm.common;

import com.soffid.mda.annotation.Enumeration;

@Enumeration
public class NodeType {
	public static String NT_SCREEN = "screen";
	public static String NT_GRANT_SCREEN = "grant-screen";
	public static String NT_APPLY = "apply";
	public static String NT_CUSTOM = "custom";
	public static String NT_MAIL = "mail";
	public static String NT_START = "start";
	public static String NT_END = "end";
	public static String NT_FORK = "fork";
	public static String NT_JOIN = "join";
}
