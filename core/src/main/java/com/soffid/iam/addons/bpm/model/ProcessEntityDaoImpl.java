package com.soffid.iam.addons.bpm.model;

import java.util.List;

import com.soffid.iam.addons.bpm.common.Node;
import com.soffid.iam.addons.bpm.common.Process;
import com.soffid.iam.addons.bpm.common.Transition;

public class ProcessEntityDaoImpl extends ProcessEntityDaoBase {

	@Override
	public void toProcess(ProcessEntity source, Process target) {
		super.toProcess(source, target);
		target.setNodes( getNodeEntityDao().toNodeList(source.getNodes()) );
		for ( NodeEntity nodeEntity: source.getNodes())
		{
			Node src = findNode (nodeEntity, target.getNodes());
			for (TransitionEntity transitionEntity: nodeEntity.getOutTransitions())
			{
				Transition transition = getTransitionEntityDao().toTransition(transitionEntity);
				transition.setSource(src);
				Node to = findNode(transitionEntity.getTarget(), target.getNodes());
				transition.setTarget(to);
				src.getOutTransitions().add(transition);
				to.getInTransitions().add(transition);
			}
		}
	}

	private Node findNode(NodeEntity nodeEntity, List<Node> nodes) {
		for (Node node: nodes)
		{
			if (node.getId().equals(nodeEntity.getId()))
				return node;
		}
		throw new RuntimeException("Unable to find node "+nodeEntity.getId()+" in process nodes");
	}

}
