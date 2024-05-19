<?xml version="1.0" encoding="UTF-8"?>
<%@ taglib uri="/WEB-INF/tld/web/core.dsp.tld" prefix="c" %>
<mxEditor cycleAttributeName="noFillcolor">
	<include name="${c:encodeURL('/addon/bpm/mxgraph/config/wfeditor-commons.xml.dsp')}"/>
	<ui>
		<add as="graph" />
		<add as="status"
			style="height:20px;bottom:20px;left:20px;right:20px"/>
		<add as="toolbar" x="16" y="20" width="90" style="padding:5px;padding-top:8px;padding-right:0px;"/>
	</ui>
	<mxGraph as="graph">
		<include name="${c:encodeURL('/addon/bpm/mxgraph/config/wfgraph-commons.xml.dsp')}"/>
	</mxGraph>
	<mxDefaultToolbar as="toolbar">
		<include name="${c:encodeURL('/addon/bpm/mxgraph/config/wftoolbar-commons.xml.dsp')}"/>
	</mxDefaultToolbar>
</mxEditor>
