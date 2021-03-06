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
package org.ms123.common.camel.components.localdata;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultEndpoint;
import org.ms123.common.data.api.DataLayer;
import flexjson.*;

/**
 * Represents a LocalData endpoint.
 */
@SuppressWarnings({"unchecked","deprecation"})
public class LocalDataEndpoint extends DefaultEndpoint {

	protected JSONSerializer js = new JSONSerializer();
	protected JSONDeserializer ds = new JSONDeserializer();
	private LocalDataComponent m_component = null;

	private LocalDataConsumer m_localDataConsumer;

	private String m_namespace;

	private String m_filterName;
	private String m_destination;
	private String m_paramHeaders;

	private String m_objectId;
	private String m_where;
	private String m_source;
	private int m_max=100;

	private String m_entityType;

	private String m_relation;
	private Boolean m_noUpdate=false;
	private Boolean m_disableStateSelect=false;
	private String m_lookupRelationObjectExpr;
	private String m_lookupUpdateObjectExpr;
	private List<Map<String, String>> objectCriteria = new ArrayList<Map<String, String>>();
	private List<Map<String, String>> filterParameter = new ArrayList<Map<String, String>>();

	private Map m_options;

	public LocalDataEndpoint() {
	}

	public LocalDataEndpoint(String uri, LocalDataComponent component) {
		super(uri, component);
		m_component = component;
	}

	void addConsumer(LocalDataConsumer consumer) {
		if (m_localDataConsumer != null) {
			throw new RuntimeException("LocalData consumer already defined for " + getEndpointUri() + "!");
		}
		m_localDataConsumer = consumer;
	}

	public void process(Exchange ex) throws Exception {
		if (m_localDataConsumer == null) {
			throw new RuntimeException("LocalData consumer not defined for " + getEndpointUri());
		}
		m_localDataConsumer.getProcessor().process(ex);
	}

	public void configureProperties(Map<String, Object> options) {
		info("LocalDataEndpoint:" + options);
		m_options = options;
	}

	public Map getOptions() {
		return m_options;
	}

	public void setFilterName(String data) {
		this.m_filterName = data;
	}

	public String getFilterName() {
		return m_filterName;
	}

	public void setDestination(String data) {
		this.m_destination = data;
	}

	public String getDestination() {
		return m_destination;
	}

	public void setParamHeaders(String data) {
		this.m_paramHeaders = data;
	}

	public String getParamHeaders() {
		return m_paramHeaders;
	}

	public void setEntityType(String data) {
		this.m_entityType = data;
	}

	public String getEntityType() {
		return m_entityType;
	}
	public Boolean isDisableStateSelect() {
		return m_disableStateSelect;
	}
	public void setDisableStateSelect(boolean nou) {
		m_disableStateSelect=nou;
	}
	public Boolean isNoUpdate() {
		return m_noUpdate;
	}
	public void setNoUpdate(boolean nou) {
		m_noUpdate=nou;
	}
	public String getRelation() {
		return m_relation;
	}
	public void setRelation(String relation) {
		m_relation=relation;
	}
	public String getLookupRelationObjectExpr() {
		return m_lookupRelationObjectExpr;
	}
	public void setLookupRelationObjectExpr(String lookup) {
		m_lookupRelationObjectExpr=lookup;
	}
	public String getLookupUpdateObjectExpr() {
		return m_lookupUpdateObjectExpr;
	}
	public void setLookupUpdateObjectExpr(String update) {
		m_lookupUpdateObjectExpr=update;
	}

	public void setObjectId(String data) {
		this.m_objectId = data;
	}

	public String getObjectId() {
		return m_objectId;
	}

	public void setWhere(String data) {
		this.m_where = data;
	}

	public String getWhere() {
		return m_where;
	}

	public void setObjectCriteria(String data) {
		if (data != null) {
			this.objectCriteria = (List) ds.deserialize(data);
		}
	}

	public List<Map<String, String>> getObjectCriteria() {
		return this.objectCriteria;
	}

	public void setFilterParameter(String data) {
		if (data != null) {
			this.filterParameter = (List) ds.deserialize(data);
		}
	}

	public List<Map<String, String>> getFilterParameter() {
		return this.filterParameter;
	}

	public void setSource(String data) {
		this.m_source = data;
	}

	public String getSource() {
		return m_source;
	}

	public void setMax(String data) {
		this.m_max = Integer.parseInt( data);
	}

	public int getMax() {
		return m_max;
	}

	public void setNamespace(String namespace) {
		this.m_namespace = namespace;
	}

	public String getNamespace() {
		return m_namespace;
	}

	public LocalDataEndpoint(String endpointUri) {
		super(endpointUri);
	}

	public Producer createProducer() throws Exception {
		return new LocalDataProducer(this);
	}

	public Consumer createConsumer(Processor processor) throws Exception {
		return new LocalDataConsumer(this, processor);
	}

	protected DataLayer getDataLayer() {
		return m_component.getDataLayer();
	}

	public boolean isSingleton() {
		return true;
	}
	private void info(String msg) {
		m_logger.info(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(LocalDataEndpoint.class);
}
