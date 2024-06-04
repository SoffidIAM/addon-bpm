package com.soffid.iam.addons.bpm.web;

import java.io.IOException;

import com.soffid.addons.bpm.web.mxgraph.ZkdbFns;
import com.soffid.iam.addons.bpm.common.Node;
import com.soffid.iam.addons.bpm.common.NodeType;
import com.soffid.iam.addons.bpm.common.Process;
import com.soffid.iam.addons.bpm.common.Transition;

public class DiagramDrawer {
	StringBuffer sb = new StringBuffer();
	int num ;
	int pos = 0;
	public String draw(Process process) throws IOException {
		sb = new StringBuffer();
		num = 2;
		pos = -100;
		sb.append("<mxGraphModel><root><Workflow label=\"MyWorkflow\" description=\"\" href=\"\" id=\"0\"><mxCell/></Workflow>"
				+ "<Layer label=\"Default Layer\" id=\"1\"><mxCell parent=\"0\"/></Layer>");
		for (Node node: process.getNodes()) {
			generateNode(node);
		}
		for (Node node: process.getNodes()) {
			for (Transition transition: node.getOutTransitions()) {
				generateTransition(transition);
			}
		}

		sb.append("</root></mxGraphModel>");
		return sb.toString();
	}

	private void generateTransition(Transition t) throws IOException {
		int srcPos = Integer.parseInt(t.getSource().getDiagramId()) * 128 - 228;
		int targetPos = Integer.parseInt(t.getTarget().getDiagramId()) * 128 - 228;
		if (srcPos < targetPos) srcPos += 64;
		else targetPos += 64;
		t.setDiagramId(Integer.toString(num++));
		sb.append("<Edge label='")
			.append(encodeValue(t.getName()))
			.append("' description='' id='")
			.append(t.getDiagramId())
			.append("'><mxCell style='straightEdge' edge='1' parent='1' source='")
			.append(t.getSource().getDiagramId())
			.append("' target='")
			.append(t.getTarget().getDiagramId())
			.append("'><mxGeometry relative='1' as='geometry'/></mxCell></Edge>");
	}
		
	private void generateNode(Node node) throws IOException {
		node.setDiagramId(Integer.toString(num++));
		if (node.getType() == NodeType.NT_START) {
			sb.append("<Symbol label='")
				.append(encodeValue(node.getName()))
				.append("' id='")
				.append(node.getDiagramId())
				.append("' type='start'><mxCell style='start' vertex='1' parent='1'><mxGeometry x='10' y='")
				.append(pos += 128)
				.append("' width='64' height='64' as='geometry'/></mxCell></Symbol>");
		}
		else if (node.getType() == NodeType.NT_APPLY) {
			sb.append("<Node label='")
				.append(encodeValue(node.getName()))
				.append("' id='")
				.append(node.getDiagramId())
				.append("' type='apply'><mxCell style='apply' vertex='1' parent='1'><mxGeometry x='10' y='")
				.append(pos += 128)
				.append("' width='128' height='64' as='geometry'/></mxCell></Node>");
		}
		else if (node.getType() == NodeType.NT_CUSTOM) {
			sb.append("<Symbol label='")
				.append(encodeValue(node.getName()))
				.append("' id='")
				.append(node.getDiagramId())
				.append("' type='decision'><mxCell style='decision' vertex='1' parent='1'><mxGeometry x='10' y='")
				.append(pos += 128)
				.append("' width='64' height='64' as='geometry'/></mxCell></Symbol>");
		}
		else if (node.getType() == NodeType.NT_END) {
			sb.append("<Symbol label='")
				.append(encodeValue(node.getName()))
				.append("' id='")
				.append(node.getDiagramId())
				.append("' type='end'><mxCell style='end' vertex='1' parent='1'><mxGeometry x='10' y='")
				.append(pos += 128)
				.append("' width='64' height='64' as='geometry'/></mxCell></Symbol>");
		}
		else if (node.getType() == NodeType.NT_FORK) {
			sb.append("<Symbol label='")
				.append(encodeValue(node.getName()))
				.append("' id='")
				.append(node.getDiagramId())
				.append("' type='fork'><mxCell style='fork' vertex='1' parent='1'><mxGeometry x='10' y='")
				.append(pos += 128)
				.append("' width='64' height='64' as='geometry'/></mxCell></Symbol>");
		}
		else if (node.getType() == NodeType.NT_MATCH_SCREEN) {
			sb.append("<Task label='")
				.append(encodeValue(node.getName()))
				.append("' id='")
				.append(node.getDiagramId())
				.append("' type='match'><mxCell style='match' vertex='1' parent='1'><mxGeometry x='10' y='")
				.append(pos += 128)
				.append("' width='128' height='64' as='geometry'/></mxCell></Task>");
		}
		else if (node.getType() == NodeType.NT_SCREEN) {
			sb.append("<Task label='")
				.append(encodeValue(node.getName()))
				.append("' id='")
				.append(node.getDiagramId())
				.append("' type='task' ><mxCell style='task' vertex='1' parent='1'><mxGeometry x='10' y='")
				.append(pos += 128)
				.append("' width='128' height='64' as='geometry'/></mxCell></Task>");
		}
		else if (node.getType() == NodeType.NT_GRANT_SCREEN) {
			sb.append("<Task label='")
				.append(encodeValue(node.getName()))
				.append("' id='")
				.append(node.getDiagramId())
				.append("' type='grant'><mxCell style='grant' vertex='1' parent='1'><mxGeometry x='10' y='")
				.append(pos += 128)
				.append("' width='128' height='64' as='geometry'/></mxCell></Task>");
		}
		else if (node.getType() == NodeType.NT_JOIN) {
			sb.append("<Symbol label='")
				.append(encodeValue(node.getName()))
				.append("' id='")
				.append(node.getDiagramId())
				.append("' type='join'><mxCell style='join' vertex='1' parent='1'><mxGeometry x='10' y='")
				.append(pos += 128)
				.append("' width='64' height='64' as='geometry'/></mxCell></Symbol>");
		}
		else if (node.getType() == NodeType.NT_MAIL) {
			sb.append("<Symbol label='")
				.append(encodeValue(node.getName()))
				.append("' id='")
				.append(node.getDiagramId())
				.append("' type='mail'><mxCell style='mail' vertex='1' parent='1'><mxGeometry x='10' y='")
				.append(pos += 128)
				.append("' width='64' height='64' as='geometry'/></mxCell></Symbol>");
		}
		else if (node.getType() == NodeType.NT_TIMER) {
			sb.append("<Symbol label='")
				.append(encodeValue(node.getName()))
				.append("' id='")
				.append(node.getDiagramId())
				.append("' type='timer'><mxCell style='timer' vertex='1' parent='1'><mxGeometry x='10' y='")
				.append(pos += 128)
				.append("' width='64' height='64' as='geometry'/></mxCell></Symbol>");
		}
		else {
			sb.append("<Symbol label='")
				.append(encodeValue(node.getName()))
				.append("' id='")
				.append(node.getDiagramId())
				.append("' type='system'><mxCell style='system' vertex='1' parent='1'><mxGeometry x='10' y='")
				.append(pos += 128)
				.append("' width='64' height='64' as='geometry'/></mxCell></Symbol>");
		}
	}
	
	private String encodeValue(String name) {
		return name == null ? "": name.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

}
