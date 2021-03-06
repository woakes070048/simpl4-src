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
package org.ms123.common.management;

import flexjson.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import org.ms123.common.libhelper.BundleDelegatingClassLoader;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.domainobjects.api.DomainObjectsService;
import org.ms123.common.system.compile.CompileService;
import org.ms123.common.system.dbmeta.DbMetaService;
import org.ms123.common.workflow.api.WorkflowService;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.camel.api.CamelService;
import org.ms123.common.system.thread.ThreadContext;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.git.GitService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceRegistration;
import org.apache.commons.io.FileUtils;
import static com.jcabi.log.Logger.info;
import static com.jcabi.log.Logger.error;

/**
 *
 */
@SuppressWarnings("unchecked")
@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
abstract class BaseManagementServiceImpl implements EventHandler, FrameworkListener{
	protected BundleContext m_bundleContext;
	protected DomainObjectsService m_domainobjectsService;
	protected CompileService m_compileService;
	protected DbMetaService m_dbmetaService;
	protected WorkflowService m_workflowService;
	protected CamelService m_camelService;
	protected GitService m_gitService;
	protected PermissionService m_permissionService;
	protected  ServiceRegistration m_serviceRegistration;
	protected JSONDeserializer m_ds = new JSONDeserializer();
	protected JSONSerializer m_js = new JSONSerializer();
	private Bundle m_camelBundle=null;
	final static String[] topics = ["namespace/installed", "namespace/postUpdate", "namespace/deleted"];

	protected void registerEventHandler() {
		try {
			Dictionary d = new Hashtable();
			d.put(EventConstants.EVENT_TOPIC, topics);
			m_serviceRegistration = m_bundleContext.registerService(EventHandler.class.getName(), this, d);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void handleEvent(Event event) {
		info(this, "BaseManagementServiceImpl.Event: " + event.getProperty("namespace")+":"+event.getTopic());
		if( "namespace/installed".equals(event.getTopic())){
			String namespace= (String)event.getProperty("namespace");
			doAllInNamespace(namespace);
		}
		if( "namespace/postUpdate".equals(event.getTopic())){
			String namespace= (String)event.getProperty("namespace");
			doAllInNamespace(namespace);
		}
	}

	public void frameworkEvent(FrameworkEvent event) {
		info(this, "BaseManagementServiceImpl.frameworkEvent:"+event);
		if( event.getType() != FrameworkEvent.STARTED){
			return;
		}
		String _ns = "global";
		StoreDesc sdesc = StoreDesc.getNamespaceData(_ns);
		m_permissionService.loginInternal(_ns);
		ThreadContext.loadThreadContext(_ns, "admin");
		for( Bundle b : event.getBundle().getBundleContext().getBundles()){
			if( b.getSymbolicName().equals("org.ms123.common.camel")){
				m_camelBundle = b;
				break;
			}
		}
		ClassLoader cclSave = Thread.currentThread().getContextClassLoader();
		BundleDelegatingClassLoader ccl = new BundleDelegatingClassLoader(m_camelBundle, cclSave);
		Thread.currentThread().setContextClassLoader(ccl);
		try{
			List<String> createdNamespaces = new ArrayList<String>();
			info(this, "BaseManagementServiceImpl.createdNamespaces:"+createdNamespaces);
			boolean firstRun = isFirstRun();
			Setup.doSetup(createdNamespaces,firstRun);
			if( firstRun){
				createdNamespaces = getAllNamespaces();
				for( String ns : createdNamespaces){
					doAllInNamespace(ns);
				}
			}else{
				for( String ns : createdNamespaces){
					doAllInNamespace(ns);
				}
				List<String> allNamespaces = getAllNamespaces();
				for( String ns : allNamespaces){
					if( createdNamespaces.indexOf(ns) < 0){
						createRoutesFromJson(ns);
						installBundles(ns);
					}
				}
			}
		}finally{
			Thread.currentThread().setContextClassLoader(cclSave);
			ThreadContext.getThreadContext().finalize(null);
		}
	}

	private void doAllInNamespace(String ns){
		installBundles(ns);
		compileGroovy(ns);
		compileJava(ns);
		createDatasources(ns);
		createClasses(ns);
		createRoutesFromJson(ns);
		deployWorkflows(ns);
	}

	private void installBundles(String ns){
		try{
			String fileInstallDir = (String) System.getProperty("felix.fileinstall.dir");
			String gitRepos = (String) System.getProperty("git.repos");
			File src = new File(gitRepos,ns+"/.etc/bundles");
			File dest = new File(fileInstallDir);
			if( src.exists()){
				info(this,"installBundles:"+src+" -> "+ dest);
				FileUtils.copyDirectory(src, dest);
			}
		}catch(Exception e){
			error(this, "installBundlesNamespace.error:%[exception]s",e);
			e.printStackTrace();
		}
	}

	private void compileGroovy(String ns){
		try{
			List<Map> result = m_compileService.compileGroovyNamespace( ns );
			info(this, "compileGroovyNamespace:"+ns+"/result:"+result);
		}catch(Exception e){
			error(this, "compileGroovyNamespace.error:%[exception]s",e);
			e.printStackTrace();
		}
	}

	private void compileJava(String ns){
		try{
			List<Map> result = m_compileService.compileJavaNamespace( ns,m_camelBundle );
			info(this, "compileJavaNamespace:"+ns+"/result:"+result);
		}catch(Exception e){
			error(this, "compileJavaNamespace.error:%[exception]s",e);
			e.printStackTrace();
		}
	}

	private void createClasses(String ns){
		try{
			StoreDesc sdesc = StoreDesc.getNamespaceData(ns);
			info(this, "createClasses:"+ns+"/sdesc:"+sdesc);
			m_domainobjectsService.createClasses(sdesc);
		}catch(Exception e){
			error(this, "createDomainClasses.error:%[exception]s",e);
			e.printStackTrace();
		}
	}

	private void createRoutesFromJson(String ns){
		try{
			info(this, "createRoutesFromJson:"+ns);
			m_camelService.createRoutesFromJson(ns);
		}catch(Exception e){
			error(this, "createRoutesFromJson.error:%[exception]s",e);
			e.printStackTrace();
		}
	}
	private void createDatasources(String ns){
		try{
			info(this, "createDatasources:"+ns);
			m_dbmetaService.deployNamespace(ns);
		}catch(Exception e){
			error(this, "deployDatasources.error:%[exception]s",e);
			e.printStackTrace();
		}
	}
	private void deployWorkflows(String ns){
		try{
			info(this, "deployWorkflows:"+ns);
			m_workflowService.deployNamespace(ns);
		}catch(Exception e){
			error(this, "deployWorkflows.error:%[exception]s",e);
			e.printStackTrace();
		}
	}
	private List<String> getAllNamespaces(){
		List<String> allNamespaces = new ArrayList<String>();
		List<Map> repos = m_gitService.getRepositories(new ArrayList(),false);
		for(Map<String,String> repo : repos){
			allNamespaces.add(repo.get("name"));
		}
		return allNamespaces;
	}

	private boolean isFirstRun(){
		String simpl4Dir = (String) System.getProperty("simpl4.dir");
		File initFile = new File(simpl4Dir, "etc/initialized");
		boolean firstRun = false;
		if( !initFile.exists()){
			firstRun = true;
			initFile.createNewFile();
		}
		return firstRun;
	}
}

