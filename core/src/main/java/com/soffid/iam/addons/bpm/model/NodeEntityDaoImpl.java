package com.soffid.iam.addons.bpm.model;

import com.soffid.iam.addons.bpm.common.Node;

public class NodeEntityDaoImpl extends NodeEntityDaoBase {

	@Override
	public void toNode(NodeEntity source, Node target) {
		super.toNode(source, target);
		target.setFields(getFieldEntityDao().toFieldList(source.getFields()));
		target.setFilters(getFilterEntityDao().toFilterList(source.getFilters()));
		target.setTriggers(getTriggerEntityDao().toTriggerList(source.getTriggers()));
	}

}
