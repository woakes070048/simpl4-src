/**
 * This file is part of SIMPL4(http://simpl4.org).
 *
 * 	Copyright [2014] [Manfred Sattler] <manfred@ms123.org>
 *
 * SIMPL4 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SIMPL4 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SIMPL4.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ms123.common.camel.components.activiti;

import org.activiti.engine.RuntimeService;
import org.apache.camel.*;
import org.activiti.camel.ActivitiConsumer;
import org.apache.camel.impl.DefaultEndpoint;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.workflow.api.WorkflowService;
import java.util.Map;

/**
 */
public class ActivitiEndpoint extends org.activiti.camel.ActivitiEndpoint {

	private RuntimeService m_runtimeService;
	private Map m_options;

	private PermissionService m_permissionService;
	private WorkflowService m_workflowService;

	public ActivitiEndpoint(String uri, CamelContext camelContext, WorkflowService ws, PermissionService ps) {
		super(uri, camelContext );
		info("ActivitiEndpoint.create:" + uri +"/WorkflowService:"+ws+"/permissionService:"+ps);
		m_runtimeService = ws.getProcessEngine().getRuntimeService();
		setRuntimeService( m_runtimeService);
		m_permissionService = ps;
		m_workflowService = ws;
	}

	public Producer createProducer() throws Exception {
		info("ActivitiEndpoint.createProducer" );
		return new org.ms123.common.camel.components.activiti.ActivitiProducer(this, m_workflowService,m_permissionService);
	}
	public void configureProperties(Map<String, Object> options) {
		info("ActivitiEndpoint:" + options);
		m_options = options;
	}

	public Map getOptions() {
		return m_options;
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(ActivitiEndpoint.class);
	private void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
}