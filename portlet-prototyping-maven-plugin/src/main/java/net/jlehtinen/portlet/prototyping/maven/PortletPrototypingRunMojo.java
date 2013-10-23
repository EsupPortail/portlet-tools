/*
 * Copyright 2010 Johannes Lehtinen 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.jlehtinen.portlet.prototyping.maven;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import net.jlehtinen.portlet.util.PortletXml;
import net.jlehtinen.portlet.util.ReflectionWrapper;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.pluto.util.assemble.AssemblerConfig;
import org.apache.pluto.util.assemble.AssemblerFactory;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.plugin.Jetty6RunMojo;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Runs the portlet or portlets being developed directly from the Maven source project
 * using the Jetty servlet container and the Apache Pluto portlet container. Extends the
 * <a href="http://docs.codehaus.org/display/JETTY/Maven+Jetty+Plugin"><em>maven-jetty-plugin</em></a>.
 * Notice that in addition to parameters defined here, the parameters defined for the extended
 * <a href="http://jetty.codehaus.org/jetty/maven-plugin/run-mojo.html"><em>jetty:run</em></a>
 * goal are also available.
 * 
 * @extendsPlugin jetty
 * @goal run
 * @description Runs the portlet or portlets being developed from the Maven source project
 */
public class PortletPrototypingRunMojo extends Jetty6RunMojo {

	/** Jetty-Pluto group identifier */
	protected static final String PORTLET_PROTOTYPING_GROUP_ID = "net.jlehtinen.portlet";
	
	/** Jetty-Pluto portal identifier */
	protected static final String PORTLET_PROTOTYPING_PORTAL_ID = "portlet-prototyping-portal";
	
	/** Pluto group identifier */
	protected static final String PLUTO_GROUP_ID = "org.apache.portals.pluto";
	
	/** System property for the portlet names */
	protected static final String PORTLET_NAMES_PROPERTY = "portletNames";
	
	/** System property for the portlet context */
	protected static final String PORTLET_CONTEXT_PATH_PROPERTY = "portletContextPath";
	
	/** System property for custom CSS URLs */
	protected static final String CSS_URLS_PROPERTY = "cssUrls";
	
	/** System property for custom Javascript URLs */
	protected static final String JS_URLS_PROPERTY = "jsUrls";
	
	/** Path to properties file containing version information */
	protected static final String VERSION_PROPERTIES_PATH = "/net/jlehtinen/portlet/prototyping/maven/version.properties";
	
	/** Minimum supported Java version */
	protected static final int MIN_JAVA_VERSION = 6;

	/** System property for skipping Java version check */
	protected static final String SKIP_JAVA_VERSION_CHECK_PROPERTY = "net.jlehtinen.portlet.prototyping.maven.skipJavaVersionCheck";
	
	/**
	 * The portlet.xml file to be used. The default location is in ${basedir}/src/main/webapp/WEB-INF.
	 * The file can also be specified at runtime using the <em>maven.war.portletxml</em>
	 * property.
	 * 
	 * @parameter expression="${maven.war.portletxml}"
	 */
	protected File portletXml;
	
	/**
	 * The destination file into which a modified version of the <em>portlet.xml</em> is written.
	 * This will be used only if the descriptor needs to be modified.
	 * 
	 * @parameter expression="${project.build.directory}/pluto-resources/portlet.xml"
	 * @readonly
	 * @required
	 */
	protected File portletXmlDestination;
	
    /**
     * The destination file into which an assembled version of the <em>web.xml</em> is written.
     * 
     * @parameter expression="${project.build.directory}/pluto-resources/web.xml"
     * @readonly
     * @required
     */
    private File webXmlDestination;

	/**
	 * <p>Specifies the names of the portlets to be prototyped under Pluto as a comma separated list.
	 * The names must match exactly the name provided in the <em>portlet-name</em> element
	 * of the portlet descriptor. The default is to include all portlets found in the portlet.xml.</p>
	 * 
	 * <p>Example: <code>&lt;portletNames>MyPortlet,Another Portlet&lt;/portletNames></code></p>
	 * 
	 * <p>The portlets can also be specified at runtime using the <em>portletNames</em> property.</p>
	 * 
	 * @parameter expression="${portletNames}"
	 */
	protected String portletNames;
	
	/**
	 * <p>Whether to disable portlets other than the ones specified in <i>portletNames</i>. If this is set
	 * to true, the other portlets are disabled by filtering them away from the <em>portlet.xml</em>
	 * before the portlet project is deployed. The default is not to disable other portlets.</p>
	 * 
	 * <p>This parameter can be used if some of the other portlets depend on portal implementation
	 * specific features that are not available in the prototyping environment.</p>
	 * 
	 * @parameter expression="${disableOtherPortlets}" default-value="false"
	 */
	protected boolean disableOtherPortlets = false;
	
    /**
     * <p>Portal implementation to use. By default a slightly modified Pluto portal implementation is
     * used but this parameter can be used to override it. For example, it is possible to create a
     * customized portal style by creating a WAR overlay on the default portal and then specify
     * the custom portal version using this parameter.</p>
     * 
     * <p>You can either specify a Maven artifact using <em>groupId</em>,
     * <em>artifactId</em> and <em>version</em>, or a direct path to the WAR
     * using <em>file</em>.</p>
     * 
     * <p>The following example demonstrates how to use Maven artifact resolution:</p>
     * 
     * <pre>
     * &lt;portal>
     *   &lt;groupId>net.jlehtinen.portlet&lt;/groupId>
     *   &lt;artifactId>portlet-prototyping-portal&lt;/artifactId>
     *   &lt;version>0.5&lt;/version>
     *   &lt;packaging>war&lt;/packaging>
     * &lt;/portal>
     * </pre>
     * 
     * @parameter
     */
    protected ArtifactIdentity portal;
    
	/**
	 * <p>Portal libraries to be installed into the shared class path.
	 * By default the libraries needed by the Pluto portal implementation
	 * are included but this parameter can be used to override the default
	 * set of libraries.</p>
	 * 
	 * <p>Each library can be specified either as a Maven artifact using <em>groupId</em>,
	 * <em>artifactId</em> and <em>version</em>, or as a direct path to the JAR
	 * using <em>file</em>.</p>
	 * 
	 * <p>The following example demonstrates both kinds of library referrals:</p>
	 * 
	 * <pre>
	 * &lt;portalLibraries>
	 *   &lt;library>
	 *     &lt;groupId>org.apache.portals.pluto&lt;/groupId>
	 *     &lt;artifactId>pluto-container-api&lt;/artifactId>
	 *     &lt;version>2.1.0-M3&lt;/version>
	 *     &lt;packaging>jar&lt;/packaging>
	 *   &lt;/library>
	 *   &lt;library>
	 *     &lt;file>/opt/j2ee/libs/portlet-api-2.0.jar&lt;/file>
	 *   &lt;/library>
	 * &lt;/portalLibraries>
	 * </pre>
	 * 
	 * @parameter
	 */
	protected List<Library> portalLibraries;
	
	/**
	 * Version of the Pluto portal implementation to be used. This only affects the
	 * default portal libraries if they have not been explicitly specified using
	 * <em>portalLibraries</em>. To use a different version of the Pluto portal,
	 * you have to specify a portal implementation using the <em>portal</em> parameter.
	 * 
	 * @parameter default-value="2.1.0-M3"
	 */
	protected String plutoVersion;
	
	/**
	 * Context path for the portal implementation.
	 * 
	 * @parameter default-value="/pluto"
	 */
	protected String plutoContextPath;
	
	/**
	 * <p>List of URLs for CSS files to be included in the portal. CSS files can
	 * also be specified at runtime by setting the <em>cssUrls</em> property
	 * to a semicolon (";") separated list of URLs.</p>
	 * 
	 * <p>By default some Pluto-specific CSS files are included by the portal.
	 * Any CSS URLs specified override the default CSS files.</p>
	 * 
	 * <p>The following example shows how to use custom CSS files.</p>
	 * 
	 * <pre>
	 * &lt;cssUrls>
	 *   &lt;url>http://my.server/custom.css&lt;/url>
	 *   &lt;url>http://my.server/another.css&lt;/url>
	 * &lt;/cssUrls>
	 * </pre>
	 * 
	 * @parameter
	 */
	protected List<String> cssUrls;
	
	/**
	 * <p>List of URLs for Javascript files to be included in the portal. Javascript files can
	 * also be specified at runtime by setting the <em>jsUrls</em> property
	 * to a semicolon (";") separated list of URLs.</p> 
	 * 
	 * <p>By default some Pluto-specific Javascript is included by the portal.
	 * Any Javascript URLs specified override the default Javascript.</p>
	 * 
	 * <p>The following example shows how to use custom Javascript files.</p>
	 * 
	 * <pre>
	 * &lt;jsUrls>
	 *   &lt;url>http://my.server/custom.js&lt;/url>
	 *   &lt;url>http://my.server/another.js&lt;/url>
	 * &lt;/jsUrls>
	 * </pre>
	 * 
	 * @parameter
	 */
	protected List<String> jsUrls;

	/**
	 * <p>List of users to be added in the user realm. By default a single user <em>pluto</em>
	 * with password <em>pluto</em> and role <em>pluto</em> is included in the
	 * user realm but this parameter can be used to override user realm data.</p>
	 * 
	 * <p>The following example demonstrates the use of this parameter:</p>
	 * 
	 * <pre>
	 * &lt;users>
	 *   &lt;user>
	 *     &lt;name>myuser&lt;/name>
	 *     &lt;password>mypass&lt;/passsword>
	 *     &lt;roles>myrole,admin&lt;/roles>
	 *   &lt;/user>
	 * &lt/users>
	 * </pre>
	 * 
	 * @parameter
	 */
	protected List<User> users;
	
	/**
	 * Name of the user realm passed on to Pluto.
	 * 
	 * @parameter default-value="Pluto Realm"
	 */
	protected String plutoRealmName;
	
	/**
	 * Artifact resolver
	 * 
	 * @component
	 */
	protected ArtifactResolver artifactResolver;

	/**
	 * Artifact factory
	 * 
	 * @component
	 */
	protected ArtifactFactory artifactFactory;
	
    /**
     * Local repository
     *
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    protected ArtifactRepository localRepository;

    /**
     * Remote repositories being used
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    protected List<ArtifactRepository> remoteRepositories;

	/** Context handler for the Pluto portal. */
	protected ContextHandler plutoHandler;
	
	/** The original web.xml file of the web application */
	protected File originalWebXml;
	
	/** The parsed portlet.xml or null if not loaded yet */
	protected PortletXml parsedPortletXml;
	
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Check Java version
		checkJavaVersion();
		
		// Configure this mojo
		configureJettyPlutoRunMojo();

		// Assemble portlets for Pluto
		assemblePortlets();
		
		// Configure required portal libraries into the Jetty class path
		configureClassPath();
		
		// Create a context handler for Pluto portal
		plutoHandler = createPlutoContextHandler();
		
		super.execute();
	}

	/**
	 * Checks for a supported Java version.
	 */
	protected void checkJavaVersion() {
		String skip = System.getProperty(SKIP_JAVA_VERSION_CHECK_PROPERTY);
		String versionStr = System.getProperty("java.version");
		if (skip == null && versionStr != null) {
			try {
				int stop1 = versionStr.indexOf('.');
				int major = Integer.valueOf(versionStr.substring(0, stop1)).intValue();
				int stop2 = versionStr.indexOf('.', stop1 + 1);
				int minor = Integer.valueOf(versionStr.substring(stop1 + 1, stop2 != -1 ? stop2 : versionStr.length())).intValue();
				if (major < 1 || (major == 1 && minor < MIN_JAVA_VERSION)) {
					getLog().error(MessageFormat.format(
							"Portlet Prototyping Maven Plugin requires Java {0} or greater but current version {1} < 1.{0}.",
							new Object[] {
									new Integer(MIN_JAVA_VERSION),
									versionStr
							}
					));
					getLog().info(MessageFormat.format(
							"Set system property -D{0} to skip version check.",
							new Object[] { SKIP_JAVA_VERSION_CHECK_PROPERTY }
					));
					System.exit(1);
				}
			} catch (Throwable t) {
				getLog().warn(MessageFormat.format("Exception parsing Java version string {0}", new Object[] { versionStr }), t);
			}
		}
	}
	
	/**
	 * Overrides the Jetty plugin method to add the portal context handler
	 * into the list of handlers.
	 * 
	 * @return configured context handlers and the portal context handler
	 * @see org.mortbay.jetty.plugin.Jetty6RunMojo#getConfiguredContextHandlers()
	 */
	public ContextHandler[] getConfiguredContextHandlers() {
		ContextHandler[] configuredHandlers = super.getConfiguredContextHandlers();
		ContextHandler[] handlers;
		if (configuredHandlers != null) {
			handlers = (ContextHandler[]) Arrays.copyOf(configuredHandlers, configuredHandlers.length + 1);
		} else {
			handlers = new ContextHandler[1];
		}
		handlers[handlers.length - 1] = plutoHandler;
		return handlers;
	}

	/**
	 * Completes the configuration of this mojo.
	 * 
	 * @throws MojoExecutionException on error
	 */
	protected void configureJettyPlutoRunMojo() throws MojoExecutionException {
	
		// Check which original web.xml should be used
		originalWebXml = getWebXml();
		if (originalWebXml == null) {
			originalWebXml = getDefaultWebXml();
		}
		getLog().info(MessageFormat.format("Original web.xml = {0}", new Object[] { originalWebXml }));
		
		// Make Jetty use the assembled web.xml
		configureJettyWebXml(webXmlDestination);
		getLog().info(MessageFormat.format("Assembled web.xml = {0}", new Object[] { webXmlDestination }));
		
		// Initialize default portlet.xml location, if necessary
		if (portletXml == null) {
			portletXml = getDefaultPortletXml();
		}
		getLog().info(MessageFormat.format("Original portlet.xml = {0}", new Object[] { portletXml }));

		// Check that web.xml exists
		if (!originalWebXml.exists()) {
			throw new MojoExecutionException(MessageFormat.format("Web application descriptor {0} does not exist", new Object[] { originalWebXml }));
		}

		// Check that portlet.xml exists
		if (!portletXml.exists()) {
			throw new MojoExecutionException(MessageFormat.format("Portlet descriptor {0} does not exist", new Object[] { portletXml }));
		}
		
		// Validate the user-specified portal implementation
		if (portal != null) {
			try {
				portal.validate();
			} catch (MojoExecutionException e) {
				throw new MojoExecutionException(MessageFormat.format("Invalid <portal> entry in configuration: {0}", new Object[] { e.getMessage() }));				
			}
		}
		
		// Validate the user-specified libraries
		if (portalLibraries != null) {
			Iterator<Library> iter = portalLibraries.iterator();
			while (iter.hasNext()) {
				try {
					iter.next().validate();
				} catch (MojoExecutionException e) {
					throw new MojoExecutionException(MessageFormat.format("Invalid <library> entry in configuration: {0}", new Object[] { e.getMessage() }));
				}
			}
		}
		
		// Validate the user-specified realm data
		if (users != null) {
			Iterator<User> iter = users.iterator();
			while (iter.hasNext()) {
				try {
					iter.next().validate();
				} catch (MojoExecutionException e) {
					throw new MojoExecutionException(MessageFormat.format("Invalid <user> entry in configuration: {0}",	new Object[] { e.getMessage() }));
				}				
			}
		}
		
		// Initialize the default portal implementation, if necessary
		if (portal == null) {
			portal = createDefaultPortal();
		}
		
		// Initialize the default set of portal libraries, if necessary
		if (portalLibraries == null) {
			portalLibraries = new ArrayList<Library>();
			addDefaultPortalLibraries(portalLibraries);
		}

		// Initialize the default set of users, if necessary
		if (users == null) {
			users = new ArrayList<User>();
			users.add(new User("pluto", "pluto", "pluto"));
		}
		
		// Resolve portal implementation WAR if necessary
		if (portal.getFile() == null) {
			portal.setFile(resolveArtifact(createArtifact(portal)));
		}
		
		// Resolve the libraries
		Iterator<Library> iter = portalLibraries.iterator();
		while (iter.hasNext()) {
			Library l = (Library) iter.next();
			if (l.getFile() == null) {
				l.setFile(resolveArtifact(createArtifact(l)));
			}
		}
		
		// Pass the context path onwards in a system parameter
		System.setProperty(PORTLET_CONTEXT_PATH_PROPERTY, getContextPath());
		
		// Pass the portlet identifiers to the portal in a system property
		if (System.getProperty(PORTLET_NAMES_PROPERTY) == null) {
			
			// Use portlet names from portlet.xml if not specified
			if (portletNames == null) {
				portletNames = getDefaultPortletNames();
				disableOtherPortlets = false;
			}
			
			// Set the system property
			System.setProperty(PORTLET_NAMES_PROPERTY, portletNames);
		}
		
		// Pass any CSS URLs to the portal in a system property
		urlsToProperty(cssUrls, CSS_URLS_PROPERTY);

		// Pass any Javascript URLs to the portal in a system property
		urlsToProperty(jsUrls, JS_URLS_PROPERTY);
		
	}
	
	/**
	 * Returns the default web.xml file.
	 * 
	 * @return default web.xml file
	 */
	protected File getDefaultWebXml() {
		return new File(new File(getWebAppSourceDirectory(), "WEB-INF"), "web.xml");
	}
	
	/**
	 * Returns the default portlet.xml file.
	 * 
	 * @return default portlet.xml file
	 */
	protected File getDefaultPortletXml() {
		return new File(new File(getWebAppSourceDirectory(), "WEB-INF"), "portlet.xml");
	}
	
	/**
	 * Returns the default string of portlet names, loaded from the portlet.xml.
	 * 
	 * @return default portlet names
	 * @throws MojoExecutionException if an error occurs
	 */
	protected String getDefaultPortletNames() throws MojoExecutionException {
		PortletXml doc = getParsedPortletXml();
		Set<String> namesSet = doc.getPortletNames();
		StringBuilder namesBuf = new StringBuilder();
		Iterator<String> iter = namesSet.iterator();
		while (iter.hasNext()) {
			String name = iter.next();
			if (namesBuf.length() > 0) {
				namesBuf.append(',');
			}
			namesBuf.append(name);
		}
		return namesBuf.toString();
	}

	/**
	 * Converts a list of URLs to a semicolon separated property string value.
	 * 
	 * @param urls list of urls
	 * @param property property name
	 */
	protected void urlsToProperty(List<String> urls, String property) {
		if (System.getProperty(property) == null && urls != null) {
			StringBuilder sb = new StringBuilder();
			for (String url : urls) {
				if (sb.length() > 0) {
					sb.append(';');
				}
				sb.append(url.toString());
			}
			System.setProperty(property, sb.toString());
		}		
	}
	
    /**
     * Configures the Jetty to use the assembled web.xml file.
     */
    protected void configureJettyWebXml(File jettyWebXml) {
    	
    	// Use reflection to set webXml parameter
    	ReflectionWrapper thisRef = new ReflectionWrapper(this);
    	thisRef.setFieldValue("webXml", jettyWebXml);
    }
	
	/**
	 * Creates the artifact identity for the default portal implementation.
	 * 
	 * @return artifact identity for the default portal implementation
	 */
	protected ArtifactIdentity createDefaultPortal() throws MojoExecutionException {
		
		// Check version of this plugin
		String version;
		try {
			Properties prop = new Properties();
			prop.load(getClass().getResource(VERSION_PROPERTIES_PATH).openStream());
			version = prop.getProperty("version");
		} catch (Exception e) {
			throw new MojoExecutionException("Failed to read version information", e);
		}
		if (version == null) {
			throw new MojoExecutionException("Version property not found");
		}
		
		// Return the default portal
		return new ArtifactIdentity(PORTLET_PROTOTYPING_GROUP_ID, PORTLET_PROTOTYPING_PORTAL_ID, version, "war");
	}
	
	/**
	 * Adds the default set of portal libraries to the specified list.
	 * 
	 * @param portalLibraries list of portal libraries
	 */
	protected void addDefaultPortalLibraries(List<Library> portalLibraries) {
		portalLibraries.add(new Library("org.apache.portals", "portlet-api_2.0_spec", "1.0"));
		portalLibraries.add(new Library(PLUTO_GROUP_ID, "pluto-container-api", plutoVersion));
		portalLibraries.add(new Library(PLUTO_GROUP_ID, "pluto-container-driver-api", plutoVersion));
		portalLibraries.add(new Library(PLUTO_GROUP_ID, "pluto-taglib", plutoVersion));
		portalLibraries.add(new Library("javax.ccpp", "ccpp", "1.0"));
	}
	
	/**
	 * Assembles portlets so that they can be deployed to Pluto.
	 */
	protected void assemblePortlets() throws MojoExecutionException {
		
		// Filter portlet.xml if so configured
		File portletXmlUsed;
		if (disableOtherPortlets) {
			String[] pna = portletNames.split(",");
			Set<String> portletNamesSet = new HashSet<String>(pna.length);
			for (int i = 0; i < pna.length; i++) {
				portletNamesSet.add(pna[i]);
			}
			PortletXml doc = getParsedPortletXml();
			doc.filterPortlets(portletNamesSet);
			try {
				doc.save(portletXmlDestination);
			} catch (IOException e) {
				throw new MojoExecutionException("Failed to save filtered portlet.xml", e);
			}
			portletXmlUsed = portletXmlDestination;
			getLog().info(MessageFormat.format("Filtered portlet.xml = {0}", new Object[] { portletXmlDestination }));			
		} else {
			portletXmlUsed = portletXml;
		}
		
		// Create assembler configuration
		AssemblerConfig assemblerConfig = new AssemblerConfig();
		assemblerConfig.setWebappDescriptor(originalWebXml);
		assemblerConfig.setPortletDescriptor(portletXmlUsed);
		assemblerConfig.setDestination(webXmlDestination);
		
		// Assembler portlets
		try {
			AssemblerFactory.getFactory().createAssembler(assemblerConfig).assemble(assemblerConfig);
		} catch (Exception e) {
			throw new MojoExecutionException("Failed to assemble web application for Pluto", e);
		}
		getLog().info("Assembled web application for Pluto");
	}
	
	/**
	 * Adds the portal libraries to the shared Jetty class path.
	 */
	protected void configureClassPath() throws MojoExecutionException {
		
		// Resolve each library and add it to the class path
		Iterator<Library> iter = portalLibraries.iterator();
		while (iter.hasNext()) {

			// Add library to the mojo class loader
			String jar = ((Library) iter.next()).getFile();
			ClassLoader cl = getClass().getClassLoader();
			if (cl != null && cl instanceof URLClassLoader) {
				ReflectionWrapper wcl = new ReflectionWrapper(cl);
				try {
					wcl.invokeMethod("addURL", new Class[] { URL.class }, new Object[] { new File(jar).toURI().toURL() });
					getLog().info(MessageFormat.format("Added to shared class path: {0}", new Object[] { jar }));
				} catch (Throwable t) {
					throw new MojoExecutionException(MessageFormat.format("Failed to add jar {0} to the path of the class loader using reflection", new Object[] { jar }));
				}
			} else {
				throw new MojoExecutionException("Can not modify class loader, even via reflection");
			}
			
		}
	}

	/**
	 * Creates a context handler for the Pluto portal.
	 * 
	 * @return context handler for Pluto
	 * @throws Exception on error
	 */
	protected ContextHandler createPlutoContextHandler() {

		// Log some basic configuration
		getLog().info(MessageFormat.format("Portal context path = {0}", new Object[] { plutoContextPath }));
		getLog().info(MessageFormat.format("Portal WAR = {0}", new Object[] { portal.getFile() }));

		// Create context handler
		WebAppContext plutoHandler = new WebAppContext();
		plutoHandler.setContextPath(plutoContextPath);
		plutoHandler.setWar(portal.getFile());
		plutoHandler.setExtractWAR(false);
		Realm realm = new Realm(plutoRealmName, users);
		plutoHandler.getSecurityHandler().setUserRealm(realm);
		return plutoHandler;
	}

	/**
     * Creates a new runtime artifact identification record from the specified identifiers.
     * 
     * @param groupId group identifier
     * @param artifactId artifact identifier
     * @param version artifact version
     * @param type artifact type
     * @return artifact identification record
     */
    protected Artifact createArtifact(String groupId, String artifactId, String version, String type) {
    	return artifactFactory.createArtifact(groupId, artifactId, version, "runtime", type);
    }
    
    /**
     * Creates a new runtime artifact record from the specified artifact identity.
     * 
     * @param aid artifact identity
     * @return artifact record
     */
    protected Artifact createArtifact(ArtifactIdentity aid) {
    	return artifactFactory.createArtifact(
    			aid.getGroupId(),
    			aid.getArtifactId(),
    			aid.getVersion(),
    			"runtime",
    			aid.getPackaging()
    	);
    }
    
    /**
     * Resolves the specified artifact and returns the path to the artifact in the local repository.
     * 
     * @param artifact artifact identification record
     * @return path to the artifact file in the local repository
     * @throws ArtifactNotFoundException if no such artifact is found
     * @throws ArtifactResolutionException if artifact can not be resolved
     */
    protected String resolveArtifact(Artifact artifact) throws MojoExecutionException {
    	try {
    		artifactResolver.resolve(artifact, remoteRepositories, localRepository);
    	} catch (Exception e) {
    		throw new MojoExecutionException(MessageFormat.format("Could not resolve artifact {0}", new Object[] { artifact }), e);
    	}
    	return new File(localRepository.getBasedir(), localRepository.pathOf(artifact)).getPath();
    }
    
    /**
     * Returns the parsed portlet.xml, loading it if necessary.
     * 
     * @return portlet.xml as a DOM document
     * @throws MojoExecutionException if portlet.xml can not be loaded or parsed
     */
    protected PortletXml getParsedPortletXml() throws MojoExecutionException {
    	if (parsedPortletXml == null) {
    		try {
    			parsedPortletXml = PortletXml.load(portletXml);
    		} catch (IOException e) {
    			throw new MojoExecutionException("Failed to load or parse portlet.xml", e);
    		}
    	}
    	return parsedPortletXml;
    }
}
