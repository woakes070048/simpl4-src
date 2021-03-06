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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.StringBuilder;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.Message;
import org.apache.camel.util.CamelContextHelper;
import org.apache.camel.util.MessageHelper;
import org.apache.camel.util.ObjectHelper;
import org.ms123.common.camel.api.CamelService;
import org.ms123.common.camel.api.ExchangeUtils;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.system.thread.ThreadContext;
import static com.jcabi.log.Logger.info;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * The LocalData producer.
 */
@SuppressWarnings({ "unchecked", "deprecation" })
public class LocalDataProducer extends DefaultProducer implements LocalDataConstants {

	private String m_filterName = null;
	private String m_destination = null;

	private String m_namespace = null;

	private String m_objectId = null;
	private String m_where = null;
	private String m_source = null;
	private int m_max = 100;
	private String m_lookupUpdateObjectExpr = null;
	private String m_lookupRelationObjectExpr = null;
	private String m_relation = null;
	private Boolean m_noUpdate = false;
	private Boolean m_disableStateSelect = false;
	private List<Map<String, String>> m_objectCriteria = null;
	private List<Map<String, String>> m_filterParameter = null;

	private String m_entityType = null;

	private LocalDataOperation m_operation;

	private LocalDataEndpoint m_endpoint;

	private Map m_options;
	private PermissionService m_permissionService;
	private CamelService camelService;

	DataLayer m_dataLayer = null;

	public LocalDataProducer(LocalDataEndpoint endpoint) {
		super(endpoint);
		CamelContext camelContext = endpoint.getCamelContext();
		m_endpoint = endpoint;
		m_dataLayer = endpoint.getDataLayer();
		m_options = endpoint.getOptions();
		m_namespace = endpoint.getNamespace();
		m_filterName = endpoint.getFilterName();
		m_destination = endpoint.getDestination();
		m_objectId = endpoint.getObjectId();
		m_where = endpoint.getWhere();
		m_objectCriteria = endpoint.getObjectCriteria();
		m_filterParameter = endpoint.getFilterParameter();
		m_source = endpoint.getSource();
		m_entityType = endpoint.getEntityType();
		m_lookupRelationObjectExpr = endpoint.getLookupRelationObjectExpr();
		m_lookupUpdateObjectExpr = endpoint.getLookupUpdateObjectExpr();
		m_relation = endpoint.getRelation();
		m_noUpdate = endpoint.isNoUpdate();
		m_disableStateSelect = endpoint.isDisableStateSelect();
		String endpointKey = endpoint.getEndpointKey();
		if (endpointKey.indexOf("?") != -1) {
			endpointKey = endpointKey.split("\\?")[0];
		}
		if (endpointKey.indexOf(":") == -1) {
			throw new RuntimeException("LocalDataProducer.no_operation_in_uri:" + endpointKey);
		}
		String[] path = endpointKey.split(":");
		m_operation = LocalDataOperation.valueOf(path[1].replace("//", ""));
		info(this, "m_operation:" + m_operation);
		if (path.length > 2) {
			m_filterName = path[2].split("\\?")[0];
		}
		if (m_namespace == null) {
			m_namespace = (String) CamelContextHelper.mandatoryLookup(camelContext, "namespace");
		}
		m_permissionService = CamelContextHelper.mandatoryLookup(camelContext, PermissionService.class.getName(), PermissionService.class);
		this.camelService = (CamelService) endpoint.getCamelContext().getRegistry().lookupByName(CamelService.class.getName());
		m_max = endpoint.getMax();
	}

	public void process(Exchange exchange) throws Exception {
		String ns = m_namespace;
		if (ThreadContext.getThreadContext() == null) {
			ThreadContext.loadThreadContext(ns, "admin");
			m_permissionService.loginInternal(ns);
		}
		invokeOperation(m_operation, exchange);
	}

	/**
	 * Entry method that selects the appropriate MongoDB operation and executes it
	 * @param operation
	 * @param exchange
	 * @throws Exception
	 */
	protected void invokeOperation(LocalDataOperation operation, Exchange exchange) throws Exception {
		switch (operation) {
		case findOneByFilter:
		//	doFindOneByFilter(exchange);
			break;
		case insert:
			doInsert(exchange);
			break;

		case update:
		case updateById:
			doUpdateById(exchange,false);
			break;
		case updateByFilter:
			doUpdateByFilter(exchange,false);
			break;
		case updateByWhere:
			doUpdateByWhere(exchange,false);
			break;
		case updateByCriteria:
			doUpdateByCriteria(exchange,false);
			break;

		case upsert:
		case upsertById:
			doUpdateById(exchange,true);
			break;
		case upsertByFilter:
			doUpdateByFilter(exchange,true);
			break;
		case upsertByWhere:
			doUpdateByWhere(exchange,true);
			break;
		case upsertByCriteria:
			doUpdateByCriteria(exchange,true);
			break;

		case deleteById:
			doDelete(exchange, operation);
			break;
		case deleteByFilter:
			doDelete(exchange, operation);
			break;
		case deleteByWhere:
			doDelete(exchange, operation);
			break;
		case deleteByCriteria:
			doDelete(exchange, operation);
			break;

		case findById:
			doFind(exchange, operation);
			break;
		case findByFilter:
			doFind(exchange, operation);
			break;
		case findByWhere:
			doFind(exchange, operation);
			break;
		case findByCriteria:
			doFind(exchange, operation);
			break;

		case multiInsertUpdate:
			doMultiInsertUpdate(exchange);
			break;
		default:
			throw new RuntimeException("LocalDataProducer.Operation not supported. Value: " + operation);
		}
	}

	private String getStringCheck(String key, String def) {
		if (isEmpty(def)) {
			throw new RuntimeException("LocalDataProducer." + key + "_is_null");
		}
		info(this, "getStringCheck:" + key + "=" + def);
		return def;
	}

	private void doMultiInsertUpdate(Exchange exchange) {
		String relation = m_relation;
		if ("-".equals(relation)){
			relation = null;
		}
		Map<String, Object> persistenceSpecification = new HashMap();
		persistenceSpecification.put(LocalDataConstants.LOOKUP_RELATION_OBJECT_EXPR, m_lookupRelationObjectExpr);
		persistenceSpecification.put(LocalDataConstants.LOOKUP_UPDATE_OBJECT_EXPR, m_lookupUpdateObjectExpr);
		persistenceSpecification.put(LocalDataConstants.RELATION, relation);
		persistenceSpecification.put(LocalDataConstants.NO_UPDATE, m_noUpdate);
		info(this, "persistenceSpecification:" + persistenceSpecification);
		SessionContext sc = getSessionContext(exchange);
		Exception ex = null;
		List<Object> result = null;
		Object obj = ExchangeUtils.getSource(m_source, exchange, Object.class);
		info(this, "doMultiInsertUpdate:" + obj);
		try {
			result = sc.persistObjects(obj, persistenceSpecification);
		} catch (Exception e) {
			ex = e;
		}
		Message resultMessage = prepareResponseMessage(exchange, LocalDataOperation.multiInsertUpdate);
		processAndTransferResult(result, exchange, ex);
	}

	private void doDelete(Exchange exchange, LocalDataOperation operation) {
		String entityType = ExchangeUtils.getParameter(m_entityType, exchange, String.class, ENTITY_TYPE);
		SessionContext sc = getSessionContext(exchange);
		Exception ex = null;
		Map result = null;
		try {
			if( operation == LocalDataOperation.deleteById ){
				String objectId = ExchangeUtils.getParameter(m_objectId, exchange, String.class);
				info(this, "doDeleteById(" + entityType + "):" + objectId);
				if (isEmpty(objectId)) {
					throw new RuntimeException("LocalDataProducer.doDeleteById:no \"objectId\" given");
				}
				result = sc.deleteObjectById(entityType, objectId);
			}
			if( operation == LocalDataOperation.deleteByFilter ){
				String filterName = m_filterName;
				if (isEmpty(filterName)) {
					throw new RuntimeException("LocalDataProducer.doDeleteById:no filtername given");
				}
				Map filterParameter = buildFilterParameter(exchange);
				List<Object> idList = sc.getObjectIdsByNamedFilter(filterName, filterParameter);
				info(this, "doDeleteByFilter(" + filterName+","+filterParameter + "):"+idList);
				int i=0;
				for( Object id : idList){
					if( i++ >= m_max) break;
					result = sc.deleteObjectById(entityType, String.valueOf(id));
				}
			}
			if( operation == LocalDataOperation.deleteByWhere ){
				if (isEmpty(m_where)) {
					throw new RuntimeException("LocalDataProducer.doDeleteByWhere:no whereclause  given");
				}
				String where = ExchangeUtils.getParameter(m_where, exchange, String.class);
				info(this, "doDeleteByWhere(" + entityType+","+where + ")");
				List<Object> rmObjList = getObjectsByWhere( exchange, sc, entityType, where);
				int i=0;
				for( Object rmObj : rmObjList){
					if( i++ >= m_max) break;
					Object id = sc.getPM().getObjectId( rmObj);
					result = sc.deleteObjectById(entityType, String.valueOf(id));
				}
			}
			if( operation == LocalDataOperation.deleteByCriteria ){
				String where = buildWhere( exchange, sc, entityType );
				if( m_objectCriteria == null || isEmpty(where)){
					throw new RuntimeException("LocalDataProducer.doDeleteByCriteria:no criteria  given");
				}
				info(this, "doDeleteByCriteria(" + entityType+","+where + ")");
				List<Object> rmObjList = getObjectsByWhere( exchange, sc, entityType, where);
				int i =0;
				for( Object rmObj : rmObjList){
					if( i++ >= m_max) break;
					Object id = sc.getPM().getObjectId( rmObj);
					result = sc.deleteObjectById(entityType, String.valueOf(id));
				}
			}
		} catch (Exception e) {
			ex = e;
		}
		Message resultMessage = prepareResponseMessage(exchange, operation);
		processAndTransferResult(result, exchange, ex);
	}

	private void doFind(Exchange exchange, LocalDataOperation operation) {
		SessionContext sc = getSessionContext(exchange);
		Exception ex = null;
		try {
			if( operation == LocalDataOperation.findById ){
				String entityType = ExchangeUtils.getParameter(m_entityType, exchange, String.class, ENTITY_TYPE);
				String objectId = ExchangeUtils.getParameter(m_objectId, exchange, String.class);
				info(this, "doFindById(" + entityType + "):" + objectId);
				if (isEmpty(objectId)) {
					throw new RuntimeException("LocalDataProducer.doFindById:no \"objectId\" given");
				}
				info(this, "doFindById(" + objectId +")");
				Object res  = sc.getObjectMapById(entityType, objectId);
				Message resultMessage = prepareResponseMessage(exchange, operation);
				ExchangeUtils.setDestination(m_destination, res, exchange);
			}
			if( operation == LocalDataOperation.findByFilter ){
				String filterName = m_filterName;
				if (isEmpty(filterName)) {
					throw new RuntimeException("LocalDataProducer.doFindById:no filtername given");
				}
				Map filterParameter = buildFilterParameter(exchange);
				info(this, "doFindByFilter(" + filterName + "," + filterParameter+")");
				Map retMap = sc.executeNamedFilter(filterName, filterParameter);
				List result=null;
				if (retMap == null) {
					result = new ArrayList();
				} else {
					result = (List) retMap.get("rows");
				}
				Message resultMessage = prepareResponseMessage(exchange, LocalDataOperation.findByFilter);
				resultMessage.setHeader(LocalDataConstants.ROW_COUNT, result.size());
				ExchangeUtils.setDestination(m_destination, result, exchange);

			}
			if( operation == LocalDataOperation.findByWhere ){
				String entityType = ExchangeUtils.getParameter(m_entityType, exchange, String.class, ENTITY_TYPE);
				if (isEmpty(m_where)) {
					throw new RuntimeException("LocalDataProducer.doFindByWhere:no whereclause  given");
				}
				String where = ExchangeUtils.getParameter(m_where, exchange, String.class);
				info(this, "doFindByWhere(" + entityType+","+where + ")");
				List<Object> result = getObjectsByWhere( exchange, sc, entityType, where);
				Message resultMessage = prepareResponseMessage(exchange, LocalDataOperation.findByFilter);
				resultMessage.setHeader(LocalDataConstants.ROW_COUNT, result.size());
				ExchangeUtils.setDestination(m_destination, result, exchange);
			}
			if( operation == LocalDataOperation.findByCriteria ){
				String entityType = ExchangeUtils.getParameter(m_entityType, exchange, String.class, ENTITY_TYPE);
				String where = buildWhere( exchange, sc, entityType );
				if( m_objectCriteria == null || isEmpty(where)){
					throw new RuntimeException("LocalDataProducer.doFindByCriteria:no criteria  given");
				}
				info(this, "doFindByCriteria(" + entityType+","+where + ")");
				List<Object> result = getObjectsByWhere( exchange, sc, entityType, where);
				Message resultMessage = prepareResponseMessage(exchange, LocalDataOperation.findByFilter);
				resultMessage.setHeader(LocalDataConstants.ROW_COUNT, result.size());
				ExchangeUtils.setDestination(m_destination, result, exchange);
			}
		} catch (Exception e) {
			ex = e;
		}
	}

	private void doInsert(Exchange exchange) {
		String entityType = ExchangeUtils.getParameter(m_entityType, exchange, String.class, ENTITY_TYPE);
		Map insert = ExchangeUtils.getSource(m_source, exchange, Map.class);
		info(this, "doInsert(" + entityType + "):" + insert);
		SessionContext sc = getSessionContext(exchange);
		Exception ex = null;
		Map result = null;
		try {
			result = sc.insertObjectMap(insert, entityType);
		} catch (Exception e) {
			ex = e;
		}
		Message resultMessage = prepareResponseMessage(exchange, LocalDataOperation.insert);
		processAndTransferResult(result, exchange, ex);
		resultMessage.setBody(result);
	}

	private void doUpdateById(Exchange exchange, boolean isUpsert) {
		String objectId = ExchangeUtils.getParameter(m_objectId, exchange, String.class);
		if (isEmpty(objectId) && !isUpsert) {
			throw new RuntimeException("LocalDataProducer.doUpdateById:no \"objectId\" given");
		}
		String entityType = ExchangeUtils.getParameter(m_entityType, exchange, String.class, ENTITY_TYPE);
		Map update = ExchangeUtils.getSource(m_source, exchange, Map.class);
		SessionContext sc = getSessionContext(exchange);
		Exception ex = null;
		Map result = null;
		int max = m_max;
		int i = 0;
		try {
			if (isEmpty(objectId) && isUpsert) {
				result = sc.insertObjectMap(update, entityType);
			}else{
				info(this, "doUpdateById(" + entityType+","+objectId + "):" + update);
				result = sc.updateObjectMap(update, entityType, objectId);
			}
		} catch (Exception e) {
			ex = e;
		}
		Message resultMessage = prepareResponseMessage(exchange, LocalDataOperation.updateById);
		processAndTransferResult(result, exchange, ex);
	}

	private void doUpdateByFilter(Exchange exchange, boolean isUpsert) {
		String filterName = m_filterName;
		if (isEmpty(filterName) && !isUpsert) {
			throw new RuntimeException("LocalDataProducer.doUpdateByFilter:no filtername given");
		}
		String entityType = ExchangeUtils.getParameter(m_entityType, exchange, String.class, ENTITY_TYPE);
		Map update = ExchangeUtils.getSource(m_source, exchange, Map.class);
		SessionContext sc = getSessionContext(exchange);
		Exception ex = null;
		Map result = null;
		int max = m_max;
		int i = 0;
		try {
			info(this, "doUpdateByFilter(" + filterName+ "):" + update);
			Map filterParameter = buildFilterParameter(exchange);
			List<Object> toObjList = sc.getObjectsByNamedFilter(filterName, filterParameter);
			for( Object toObj : toObjList){
				if( i++ >= max) break;
				sc.populate( update, toObj);
			}
			if ( i==0  && isUpsert) {
				result = sc.insertObjectMap(update, entityType);
			}
		} catch (Exception e) {
			ex = e;
		}
		Message resultMessage = prepareResponseMessage(exchange, LocalDataOperation.updateByFilter);
		processAndTransferResult(result, exchange, ex);
	}
	private void doUpdateByWhere(Exchange exchange, boolean isUpsert) {
		if (isEmpty(m_where) && !isUpsert) {
			throw new RuntimeException("LocalDataProducer.doUpdateByWhere:\"where clause\" not given");
		}
		String where = ExchangeUtils.getParameter(m_where, exchange, String.class);
		String entityType = ExchangeUtils.getParameter(m_entityType, exchange, String.class, ENTITY_TYPE);
		Map update = ExchangeUtils.getSource(m_source, exchange, Map.class);
		SessionContext sc = getSessionContext(exchange);
		Exception ex = null;
		Map result = null;
		int max = m_max;
		int i = 0;
		try {
			info(this, "doUpdateByWhere(" + entityType+","+where + "):" + update);
			List<Object> toObjList = getObjectsByWhere( exchange, sc, entityType, where);
			for( Object toObj : toObjList){
				if( i++ >= max) break;
				sc.populate( update, toObj);
			}
			if ( i==0  && isUpsert) {
				result = sc.insertObjectMap(update, entityType);
			}
		} catch (Exception e) {
			ex = e;
		}
		Message resultMessage = prepareResponseMessage(exchange, LocalDataOperation.updateByWhere);
		processAndTransferResult(result, exchange, ex);
	}
	private void doUpdateByCriteria(Exchange exchange, boolean isUpsert) {
		if (m_objectCriteria == null && !isUpsert) {
			throw new RuntimeException("LocalDataProducer.doUpdate:\"objectCriteria\" not given");
		}
		String entityType = ExchangeUtils.getParameter(m_entityType, exchange, String.class, ENTITY_TYPE);
		Map update = ExchangeUtils.getSource(m_source, exchange, Map.class);
		SessionContext sc = getSessionContext(exchange);
		Exception ex = null;
		Map result = null;
		int max = m_max;
		int i = 0;
		try {
			String where = buildWhere( exchange, sc, entityType );
			info(this, "doUpdateByCriteria(" + entityType+","+where + "):" + update);
			List<Object> toObjList = getObjectsByWhere( exchange, sc, entityType, where);
			for( Object toObj : toObjList){
				if( i++ >= max) break;
				sc.populate( update, toObj);
			}
			if ( i==0  && isUpsert) {
				result = sc.insertObjectMap(update, entityType);
			}
		} catch (Exception e) {
			ex = e;
		}
		Message resultMessage = prepareResponseMessage(exchange, LocalDataOperation.updateByCriteria);
		processAndTransferResult(result, exchange, ex);
	}


	private void doUpsert(Exchange exchange) {
		String objectId = ExchangeUtils.getParameter(m_objectId, exchange, String.class);
		String entityType = ExchangeUtils.getParameter(m_entityType, exchange, String.class, ENTITY_TYPE);
		Map data = ExchangeUtils.getSource(m_source, exchange, Map.class);
		info(this, "doUpsert(" + entityType+","+objectId + "):" + data);
		SessionContext sc = getSessionContext(exchange);
		Exception ex = null;
		Map result = null;
		try {
			if( !isEmpty(objectId)){
				result = sc.updateObjectMap(data, entityType, objectId);
			}else{
				result = sc.insertObjectMap(data, entityType);
			}
		} catch (Exception e) {
			ex = e;
		}
		Message resultMessage = prepareResponseMessage(exchange, LocalDataOperation.upsertById);
		processAndTransferResult(result, exchange, ex);
	}


/*	private void doFindOneByFilter(Exchange exchange) {
		String filterName = getStringCheck(LocalDataConstants.FILTER_NAME, m_filterName);
		SessionContext sc = getSessionContext(exchange);
		Map result = null;
		Map exVars = ExchangeUtils.getVariablesFromHeaderFields(exchange, m_paramHeaders);
		Map retMap = sc.executeNamedFilter(filterName, exVars, m_options);
		if (retMap == null) {
		} else {
			List rows = (List) retMap.get("rows");
			if (rows.size() > 0) {
				result = (Map) rows.get(0);
			}
		}
		Message resultMessage = prepareResponseMessage(exchange, LocalDataOperation.findOneByFilter);
		resultMessage.setHeader(LocalDataConstants.ROW_COUNT, 1);
		ExchangeUtils.setDestination(m_destination, result, exchange);
	}*/
	private String getNamespace(Exchange exchange) {
		String namespace = m_namespace;
		if (namespace == null || "".equals(namespace) || "-".equals(namespace) ) {
			namespace = exchange.getProperty("_namespace",String.class);
		}else{
			namespace = ExchangeUtils.getParameter(m_namespace, exchange, String.class, NAMESPACE);
		}
		return namespace;
	}
	private Map<String,String> buildFilterParameter( Exchange exchange ){
		Map<String,String> ret = new HashMap<String,String>();
		if( m_filterParameter == null) return ret;
		for( Map<String,String> param : m_filterParameter){
			info(this,"param:"+param);
			String name = param.get("name");
			String value = param.get("value");
			String val = ExchangeUtils.getParameter(value, exchange, String.class);
			ret.put( name, val );
		}
		info(this,"filterParameter:"+ret);
		return ret;
	}

	private String buildWhere( Exchange exchange, SessionContext sc, String entityType ){
		Map<String,Map> fields = sc.getPermittedFields(entityType);
		StringBuilder b = new StringBuilder();
		String and = "";
		if( m_objectCriteria == null) return b.toString();
		for( Map<String,String> criteria : m_objectCriteria){
			String op = criteria.get("op");
			String name = criteria.get("name");
			String value = criteria.get("value");
			Map<String,String> field = fields.get(name);
			String  dt = field.get("datatype");
			Class clazz = getType(dt);
			Object val = ExchangeUtils.getParameter(value, exchange, clazz);
			boolean isAlpha = Character.isJavaLetter(op.charAt(0));
			Boolean isString = "string".equals( dt);
			b.append( and );
			b.append( "(" );
			b.append( name );
			b.append( isAlpha ? "." : " " );

			b.append(op);
			b.append( isAlpha ? "( " : " " );

			b.append( isString ? "\"" : "");
			b.append( val);
			b.append( isString ? "\"" : "");
			b.append( isAlpha ? " ))" : " )" );
			and = " && ";
		}
		return b.toString();
	}

	private List<Object> getObjectsByWhere( Exchange exchange, SessionContext sc, String entityType, String where){
		String pack = StoreDesc.getPackName( entityType );
		String namespace = getNamespace( exchange);
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace, pack);
		Class clazz = sc.getClass(sdesc, entityType);
		List<Object> toObjList = sc.getObjectsByFilter( clazz, where );
		if( toObjList == null || toObjList.size()==0){
			throw new RuntimeException("LocalDataProducer.doUpdate:object not found:where:"+where);
		}
		info(this, "getObjectsByWhere.toObj:" + toObjList);
		return toObjList;
	}

	private Class  getType(String dt) {
		Class ret = String.class;
		if (  "long".equals(dt)) {
			ret = Long.class;
		}else if (  "number".equals(dt)) {
			ret = Integer.class;
		}else if (  "decimal".equals(dt)) {
			ret = Double.class;
		}else if (  "double".equals(dt)) {
			ret = Double.class;
		}else if (  "date".equals(dt)) {
			ret = java.util.Date.class;
		}
		info(this,"getType.ret:"+ret);
		return ret;
	}
	private SessionContext getSessionContext(Exchange exchange) {
		String namespace = m_namespace;
		if (namespace == null || "".equals(namespace) || "-".equals(namespace) ) {
			namespace = exchange.getProperty("_namespace",String.class);
		}else{
			namespace = ExchangeUtils.getParameter(m_namespace, exchange, String.class, NAMESPACE);
		}
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
		if (sdesc == null) {
			throw new RuntimeException("LocalDataProducer.namespace:" + namespace + " not found");
		}
		SessionContext sc = m_dataLayer.getSessionContext(sdesc);
		return sc;
	}

	private void processAndTransferResult(Object result, Exchange exchange, Exception ex) {
		if (ex != null) {
			exchange.getIn().setHeader(LocalDataConstants.LAST_ERROR, ex.getMessage());
			exchange.setException(ex);
		}
		exchange.getIn().setHeader(LocalDataConstants.WRITE_RESULT, result);
	}

	private Message prepareResponseMessage(Exchange exchange, LocalDataOperation operation) {
		Message answer = exchange.getIn();
		return answer;
	}
}

