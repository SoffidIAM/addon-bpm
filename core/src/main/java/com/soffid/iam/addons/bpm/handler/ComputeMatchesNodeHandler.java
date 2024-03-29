package com.soffid.iam.addons.bpm.handler;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.jpdl.el.VariableResolver;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;

import com.soffid.iam.ServiceLocator;
import com.soffid.iam.addons.bpm.common.Filter;
import com.soffid.iam.addons.bpm.common.PageInfo;
import com.soffid.iam.addons.bpm.core.BpmUserService;
import com.soffid.iam.api.User;
import com.soffid.iam.bpm.mail.Mail;
import com.soffid.iam.service.ApplicationService;
import com.soffid.iam.service.UserService;
import com.soffid.iam.utils.Security;

public class ComputeMatchesNodeHandler implements ActionHandler {
	Log log = LogFactory.getLog(getClass());

	final ApplicationService appService = ServiceLocator.instance().getApplicationService();

	@Override
	public void execute(ExecutionContext executionContext) throws Exception {
		TaskNode tn = (TaskNode) executionContext.getNode();
		log.info("Searching user matches for node " +tn.getName());
		boolean skip = false;
		Security.nestedLogin(Security.ALL_PERMISSIONS);
		try {
			String action = (String) executionContext.getVariable("action");
			if (action == null || action.equals("A")) {
				executionContext.setVariable("action", "A");
				BpmUserService svc = (BpmUserService) ServiceLocator.instance().getService(BpmUserService.SERVICE_NAME);
				UserService userService = ServiceLocator.instance().getUserService();
				PageInfo pi = svc.getPageInfoByNodeId(executionContext.getNode().getId());
				
				// Fetch users
				Map<String, Long> users = new HashMap<String, Long>();
				for (Filter filter: pi.getFilters()) {
					Long weight = filter.getWeight();
					if (weight == null)
						weight = new Long(1);
					List<User> usersList;
					VariableResolver vr = new DateFormatterVariableResolver();
					String q = (String) JbpmExpressionEvaluator.evaluate(filter.getQuery(), 
							executionContext, 
							vr, 
							JbpmExpressionEvaluator.getUsedFunctionMapper());
					if (filter.getType() == null || filter.getType().equals("scim")) {
						usersList = userService.findUserByJsonQuery(q, 0, 1000).getResources();
					} else {
						usersList = userService.findUserByTextAndFilter(q, null, 0, 1000).getResources();
					}
					for (User user: usersList ) {
						Long i = users.get(user.getUserName());
						if (i == null)
							users.put(user.getUserName(), weight);
						else
							users.put(user.getUserName(), weight.longValue() + i.longValue());
					}
				}
				// Filter
				List<String> userNames = new LinkedList<String>();
				for (String userName: users.keySet()) {
					Long w = users.get(userName);
					if (pi.getMatchThreshold() == null || pi.getMatchThreshold().longValue() <= w.longValue()) {
						userNames.add(userName);
					}
				}
				executionContext.setVariable("$matches$", userNames);
				if (userNames.isEmpty())
					skip = true;
			} else {
				skip = true;
			}
		} finally {
			Security.nestedLogoff();
		}
		if (skip) {
			executionContext.getTaskInstance().setSignalling(false); // To leave node
			executionContext.getTaskInstance().cancel(); // To cancel task
		} else {
			Mail m = new com.soffid.iam.bpm.mail.Mail();
			m.setTemplate("task-assign");
			m.execute(executionContext);
		}
	}

}
