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
package org.ms123.common.system.jooq;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Collection;
import java.util.Set;
import javax.sql.DataSource;
import java.sql.Connection;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.jooq.util.GenerationTool;
import org.jooq.util.jaxb.Configuration;
import org.jooq.util.jaxb.Generator;
import org.jooq.util.jaxb.Target;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.rpc.PDefaultBool;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.system.compile.CompileService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.ms123.common.entity.api.EntityService;
import static com.jcabi.log.Logger.debug;
import static com.jcabi.log.Logger.error;
import static com.jcabi.log.Logger.info;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.moveDirectoryToDirectory;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** JooqServiceImpl implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=jooq" })
public class JooqServiceImpl implements JooqService {

	protected BundleContext m_bc;

	protected CompileService m_compileService;

	private PermissionService m_permissionService;

	private static String workspace = System.getProperty("workspace");
	private static String gitRepos = System.getProperty("git.repos");

	public JooqServiceImpl() {
		System.out.println("JooqServiceImpl construct");
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		m_bc = bundleContext;
		try {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void update(Map<String, Object> props) {
	}

	protected void deactivate() throws Exception {
		System.out.println("JooqServiceImpl.deactivate");
	}

	private void compileMetadata(Boolean toWorkspace, String namespace) throws Exception {
		List<File> classPath = new ArrayList<File>();
		File basedir = null;
		if (toWorkspace) {
			classPath.add(new File(workspace, "jooq/build"));
			basedir = new File(workspace, "jooq");
		} else {
			classPath.add(new File(gitRepos, namespace + "/.etc/jooq/build"));
			basedir = new File(gitRepos, namespace + "/.etc/jooq");
		}

		File destinationDirectory = new File(basedir, "build");
		File sourceDirectory = new File(basedir, "gen");
		m_compileService.compileJava(m_bc.getBundle(), destinationDirectory, sourceDirectory, classPath);
	}

	private Object getService(Class clazz, String vendor) throws Exception {
		Collection<ServiceReference> sr = m_bc.getServiceReferences(clazz, "(dataSourceName=" + vendor + ")");
		if (sr.size() > 0) {
			Object o = m_bc.getService((ServiceReference) sr.toArray()[0]);
			return o;
		}
		return null;
	}

	/*BEGIN JSON-RPC-API*/
	@RequiresRoles("admin")
	public void buildMetadata(@PName(StoreDesc.STORE_ID) String storeId,
													  @PName("configFile") @POptional String configFile,
													  @PName("toWorkspace") @POptional @PDefaultBool(false) Boolean toWorkspace) throws RpcException {
		Connection conn = null;
		try {
			StoreDesc sdesc = StoreDesc.get(storeId);
			String namespace = sdesc.getNamespace();
			File f = null;
			if (configFile != null) {
				if (configFile.indexOf("/") > 0) {
					f = new File(new File(gitRepos, sdesc.getNamespace()), configFile);
				} else {
					f = new File(new File(gitRepos, sdesc.getNamespace()), ".etc/" + configFile);
				}
			} else {
				f = new File(gitRepos, sdesc.getNamespace() + "/.etc/jooqConfig.xml");
			}
			if (!f.exists()) {
				throw new RuntimeException("JooqServiceImpl.readMetadata:(" + f + ") not exists.");
			}

			Configuration config = GenerationTool.load(new FileInputStream(f));
			String packageDir = config.getGenerator().getTarget().getPackageName().replace(".", "/");
			File basedir = null;
			if (toWorkspace) {
				basedir = new File(workspace, "jooq");
			} else {
				basedir = new File(gitRepos, namespace + "/.etc/jooq");
			}
			File pdir = new File(basedir, "gen/" + packageDir);
			deleteDirectory(pdir);

			File bdir = new File(basedir, "build/" + packageDir);
			bdir.mkdirs();

			File tdir = new File("/tmp/jooq", packageDir);
			deleteDirectory(tdir);

			String vendor = sdesc.getVendor();
			DataSource ds = (DataSource) getService(javax.sql.DataSource.class, vendor);
			System.out.println("generate.call:" + ds);
			GenerationTool gt = new GenerationTool();
			synchronized ( gt ){
				org.ms123.common.system.jooq.Simpl4Generator.setNamespace( namespace );
				gt.setConnection(conn = ds.getConnection());
				gt.run(config);
				File parent = new File(basedir, "gen/" + packageDir).getParentFile();
				moveDirectoryToDirectory(tdir, parent, true);
				compileMetadata(toWorkspace, sdesc.getNamespace());
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "JooqServiceImpl.buildMetadata:", e);
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (Exception ee) {
				ee.printStackTrace();
			}
		}
	}

	/*END JSON-RPC-API*/
	@Reference(dynamic = true)
	public void setPermissionService(PermissionService shiroService) {
		System.out.println("JooqServiceImpl:" + shiroService);
		this.m_permissionService = shiroService;
	}
	@Reference(dynamic = true)
	public void setEntityService(EntityService paramEntityService) {
		org.ms123.common.system.jooq.Simpl4Generator.setEntityService( paramEntityService);
		System.out.println("JooqServiceImpl.setEntityService:" + paramEntityService);
	}

	@Reference(dynamic = true, optional = true)
	public void setCompileService(CompileService paramService) {
		this.m_compileService = paramService;
		System.out.println("JooqServiceImpl.setCompileService:" + paramService);
	}
}

