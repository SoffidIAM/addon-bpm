package com.soffid.iam.addons.bpm.web;

import java.util.LinkedList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.zkoss.zk.ui.UiException;

import com.soffid.iam.addons.bpm.common.Attribute;
import com.soffid.iam.addons.bpm.common.Field;
import com.soffid.iam.addons.bpm.common.Filter;
import com.soffid.iam.addons.bpm.common.Node;
import com.soffid.iam.addons.bpm.common.NodeType;
import com.soffid.iam.addons.bpm.common.Process;
import com.soffid.iam.addons.bpm.common.Transition;
import com.soffid.iam.addons.bpm.common.Trigger;
import com.soffid.iam.addons.bpm.common.WorkflowType;

import es.caib.seycon.ng.comu.TypeEnumeration;


public class ProcessSerializer {
	public static JsonObject toJson (com.soffid.iam.addons.bpm.common.Process def)
	{
		JsonObjectBuilder builder = Json.createObjectBuilder();
		if (def.getAttributes() != null) builder.add("attributes", toJson(def.getAttributes()));
		if (def.getDescription() != null) builder.add("description", def.getDescription());
		if (def.getInitiators() != null) builder.add("initiators", def.getInitiators());
		if (def.getManagers() != null) builder.add("managers", def.getManagers());
		if (def.getName() != null) builder.add("name", def.getName());
		if (def.getObservers() != null) builder.add("observers", def.getObservers());
		if (def.getType() != null) builder.add("type", def.getType().toString());
		if (def.getNodes() != null) builder.add("nodes", nodesToJson(def.getNodes(), def));

		JsonObjectBuilder builder2 = Json.createObjectBuilder();
		builder2.add("type", "ProcessDefinition");
		builder2.add("version", 1);
		builder2.add("data", builder);

		JsonObject json = builder2.build();
		
		return json;
	}

	private static JsonArrayBuilder nodesToJson(List<Node> nodes, Process def) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for (Node node: nodes)
		{
			JsonObjectBuilder builder2 = Json.createObjectBuilder();
			if (node.getApplyEntitlements() != null) builder2.add("applyEntitlements", node.getApplyEntitlements());
			if (node.getApplyUserChanges() != null) builder2.add("applyUserChanges", node.getApplyUserChanges());
			if (node.getCustomScript() != null) builder2.add("customScript", node.getCustomScript());
			if (node.getDescription() != null) builder2.add("description", node.getDescription());
			if (node.getFields() != null) builder2.add("fields", fieldsToJson( node.getFields()) );
			if (node.getFilters() != null) builder2.add("filters", filtersToJson( node.getFilters()) );
			if (node.getGrantScreenType() != null) builder2.add("grantScreenType", node.getGrantScreenType());
			if (node.getMailActor() != null) builder2.add("mailActor", node.getMailActor());
			if (node.getMailAddress() != null) builder2.add("mailAddress", node.getMailAddress());
			if (node.getMailMessage() != null) builder2.add("mailMesage", node.getMailMessage());
			if (node.getMailSubject() != null) builder2.add("mailSubject", node.getMailSubject());
			if (node.getName() != null) builder2.add("name", node.getName());
			if (node.getOutTransitions() != null) builder2.add("outTransitions", transitionsToJson( node.getOutTransitions()));
			if (node.getTriggers() != null) builder2.add("triggers", triggersToJson( node.getTriggers()));
			if (node.getType() != null) builder2.add("type", node.getType().toString());
			if (node.getTaskName() != null) builder2.add("taskName", node.getTaskName().toString());
			if (node.getMatchThreshold() != null) builder2.add("matchThreshold", node.getMatchThreshold());
			if (node.getGrantScreenType() != null) builder2.add("grantScreentype", node.getGrantScreenType());
			if (node.getMailShortcut() != null) builder2.add("mailShortcut", node.getMailShortcut());
			if (node.getApproveTransition() != null) builder2.add("approveTransition", node.getApproveTransition());
			if (node.getDenyTransition() != null) builder2.add("denyTransition", node.getDenyTransition());
			builder.add(builder2);
		}
		return builder;
	}

	private static JsonArrayBuilder filtersToJson(List<Filter> filters) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for (Filter filter: filters)
		{
			JsonObjectBuilder builder2 = Json.createObjectBuilder();
			if (filter.getWeight() != null) builder2.add("weight", filter.getWeight());
			if (filter.getType() != null) builder2.add("type", filter.getType());
			if (filter.getQuery() != null) builder2.add("query", filter.getQuery());
			builder.add(builder2);
		}
		return builder;
	}

	private static JsonArrayBuilder triggersToJson(List<Trigger> triggers) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for (Trigger trigger: triggers)
		{
			JsonObjectBuilder builder2 = Json.createObjectBuilder();
			if (trigger.getAction() != null) builder2.add("action", trigger.getAction());
			if (trigger.getField() != null) builder2.add("field", trigger.getField());
			if (trigger.getName() != null) builder2.add("name", trigger.getName());
			builder.add(builder2);
		}
		return builder;
	}

	private static JsonArrayBuilder transitionsToJson(List<Transition> transitions) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for (Transition transition: transitions)
		{
			JsonObjectBuilder builder2 = Json.createObjectBuilder();
			if (transition.getName() != null) builder2.add("action", transition.getName());
			if (transition.getSource() != null && transition.getSource().getName() != null) builder2.add("source", transition.getSource().getName());
			if (transition.getScript() != null) builder2.add("script", transition.getScript());
			if (transition.getTarget() != null && transition.getTarget().getName() != null) builder2.add("target", transition.getTarget().getName());
			builder.add(builder2);
		}
		return builder;
	}

	private static JsonArrayBuilder fieldsToJson(List<Field> fields) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for (Field field: fields)
		{
			JsonObjectBuilder builder2 = Json.createObjectBuilder();
			if (field.getLabel() != null) builder2.add("label", field.getLabel());
			if (field.getName() != null) builder2.add("name", field.getName());
			if (field.getOrder() != null) builder2.add("order", field.getOrder());
			if (field.getReadOnly() != null) builder2.add("readOnly", field.getReadOnly());
			if (field.getValidationScript() != null) builder2.add("validationScript", field.getValidationScript());
			if (field.getVisibilityScript() != null) builder2.add("visibilityScript", field.getVisibilityScript());
			builder.add(builder2);
		}
		return builder;
	}

	private static JsonArrayBuilder toJson(List<Attribute> attributes) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for (Attribute attribute: attributes)
		{
			JsonObjectBuilder builder2 = Json.createObjectBuilder();
			if ( attribute.getDataObjectType() != null) builder2.add("dataObjectType", attribute.getDataObjectType());
			if ( attribute.getLabel() != null ) builder2.add("label", attribute.getLabel());
			if ( attribute.getMultiValued() != null) builder2.add("multiValued", attribute.getMultiValued());
			if ( attribute.getName() != null) builder2.add("name", attribute.getName());
			if ( attribute.getOrder() != null) builder2.add("order", attribute.getOrder());
			if ( attribute.getSize() != null) builder2.add("size", attribute.getSize());
			if ( attribute.getType() != null) builder2.add("type", attribute.getType().toString());
			if ( attribute.getValues() != null) builder2.add("values", stringListToJson (attribute.getValues()));
			builder.add(builder2);
		}
		return builder;
	}

	private static JsonArrayBuilder stringListToJson(List<String> values) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for (String value: values)
		{
			builder.add(value);
		}
		return builder;
	}

	
	public static Process processFromJson (JsonObject obj)
	{
		String type = obj.getString("type", "");
		int version = obj.getInt("version", 0);
		if (!type.equals("ProcessDefinition") || version != 1)
			throw new UiException("Wrong file format");
		JsonObject data = obj.getJsonObject("data");
		if (data == null)
			throw new UiException("Wrong file format");
		
		Process p = new Process();
		p.setAttributes( loadAttributes (data.getJsonArray("attributes")));
		p.setDescription( data.getString("description", null));
		p.setInitiators(data.getString("initiators", null));
		p.setManagers(data.getString("managers", null));
		p.setObservers(data.getString("observers", null));
		p.setName(data.getString("name", null));
		p.setType( WorkflowType.fromString( data.getString("type", WorkflowType.WT_USER.toString())));
		p.setNodes( loadNodes (data.getJsonArray("nodes")));
	
		return p;
	}

	private static List<Node> loadNodes(JsonArray jsonArray) {
		if (jsonArray == null)
			return null;
		List<Node> l = new LinkedList<Node>();
		for (JsonValue obj: jsonArray)
		{
			if (obj instanceof JsonObject) {
				JsonObject src = (JsonObject) obj;
				Node target = new Node();
				target.setApplyEntitlements( src.getBoolean("applyEntitlements", false));
				target.setApplyUserChanges(src.getBoolean("applyUserChanges", false));
				target.setCustomScript(src.getString("customScript", null));
				target.setDescription( src.getString("description", null));
				target.setFields( loadFields ( src.getJsonArray("fields")));
				target.setFilters( loadFilters ( src.getJsonArray("filters")));
				target.setGrantScreenType( src.getString("grantStringType", null));
				target.setInTransitions(new LinkedList<Transition>());
				target.setOutTransitions(new LinkedList<Transition>());
				target.setMailActor(src.getString("mailActor", null));
				target.setMailAddress(src.getString("mailAddress", null));
				target.setMailMessage(src.getString("mailMessage", null));
				target.setMailSubject(src.getString("mailSubject", null));
				target.setName(src.getString("name", null));
				target.setTriggers(loadTriggers ( src.getJsonArray("triggers")));
				target.setGrantScreenType(src.getString("grantScreenType", null));
				target.setMailShortcut(src.getBoolean("mailShortcut", false));
				target.setApproveTransition(src.getString("approveTransition", null));
				target.setDenyTransition(src.getString("denyTransition", null));
				if (src.containsKey("matchThreshold"))
						target.setMatchThreshold( new Long(src.getInt("matchThreshold")));
				if (src.containsKey("name") && ! src.containsKey("taskName"))
					target.setTaskName(src.getString("name"));
				else
					target.setTaskName(src.getString("taskName"));
				target.setType( NodeType.fromString( src.getString("type", NodeType.NT_END.toString())));
				l.add(target);
			}
		}
		
		for (JsonValue obj: jsonArray)
		{
			if (obj instanceof JsonObject) {
				JsonObject src = (JsonObject) obj;
				JsonArray transitions = src.getJsonArray("outTransitions");
				if (transitions != null)
				{
					loadTransitions (transitions, l);
				}
			}
		}

		return l;
	}

	private static void loadTransitions(JsonArray transitions, List<Node> l) {
		if (transitions == null)
			return ;
		for (JsonValue obj: transitions)
		{
			if (obj instanceof JsonObject) {
				JsonObject src = (JsonObject) obj;
				Transition t = new Transition();
				t.setName(src.getString("action", null));
				t.setScript(src.getString("script", null));
				Node source = findNode (l, src.getString("source", null));
				if (source != null)
				{
					source.getOutTransitions().add(t);
					t.setSource(source);
				}
				Node target = findNode (l, src.getString("target", null));
				if (target != null)
				{
					target.getInTransitions().add(t);
					t.setTarget(target);
				}
			}
		}
	}

	private static Node findNode(List<Node> l, String name) {
		for ( Node n: l)
			if (n.getName() != null && n.getName().equals(name))
				return n;
		return null;
	}

	private static List<Trigger> loadTriggers(JsonArray jsonArray) {
		if (jsonArray == null)
			return null;
		List<Trigger> l = new LinkedList<Trigger>();
		for (JsonValue obj: jsonArray)
		{
			if (obj instanceof JsonObject) {
				JsonObject src = (JsonObject) obj;
				Trigger target = new Trigger();
				target.setAction( src.getString("action", null));
				target.setField(src.getString("field", null));
				target.setName(src.getString("name", null));
				l.add(target);
			}
		}
		return l;
	}

	private static List<Field> loadFields(JsonArray jsonArray) {
		if (jsonArray == null)
			return null;
		List<Field> l = new LinkedList<Field>();
		for (JsonValue obj: jsonArray)
		{
			if (obj instanceof JsonObject) {
				JsonObject src = (JsonObject) obj;
				Field target = new Field();
				target.setAllowedValues(src.getString("allowedValues", null));
				target.setLabel( src.getString("label", null));
				target.setName(src.getString("name", null));
				if (src.containsKey("order")) target.setOrder(src.getJsonNumber("order").longValue());
				target.setReadOnly(src.getBoolean("readOnly", false));
				target.setValidationScript(src.getString("validationScript", null));
				l.add(target);
			}
		}
		return l;
	}

	private static List<Filter> loadFilters(JsonArray jsonArray) {
		List<Filter> l = new LinkedList<Filter>();
		if (jsonArray == null)
			return l;
		for (JsonValue obj: jsonArray)
		{
			if (obj instanceof JsonObject) {
				JsonObject src = (JsonObject) obj;
				Filter target = new Filter();
				if (src.containsKey("weight"))
					target.setWeight(src.getJsonNumber("weight").longValue());
				target.setType( src.getString("type", null));
				target.setQuery(src.getString("query", null));
				l.add(target);
			}
		}
		return l;
	}

	private static List<Attribute> loadAttributes(JsonArray jsonArray) {
		if (jsonArray == null)
			return null;
		List<Attribute> l = new LinkedList<Attribute>();
		for (JsonValue obj: jsonArray)
		{
			if (obj instanceof JsonObject) {
				JsonObject src = (JsonObject) obj;
				Attribute target = new Attribute();
				target.setDataObjectType( src.getString("dataObjectType", null));
				target.setLabel(src.getString("label", null));
				target.setMultiValued(src.getBoolean("multiValued", false));
				target.setName(src.getString("name", null));
				target.setOrder( new Long( src.getInt("order", 1)));
				target.setType(TypeEnumeration.fromString(src.getString("type")));
				if (src.containsKey("size"))
				target.setSize( src.getJsonNumber("size").intValue());
				target.setValues(loadStringList(src.getJsonArray("values")));
				l.add(target);
			}
		}
		return l;
	}

	private static List<String> loadStringList(JsonArray jsonArray) {
		if (jsonArray == null)
			return null;
		List<String> l = new LinkedList<String>();
		for (int i = 0; i < jsonArray.size(); i++)
		{
			l.add( jsonArray.getJsonString(i).getString());
		}
		return l;
	}


}
