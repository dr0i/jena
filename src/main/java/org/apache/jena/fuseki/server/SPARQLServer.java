/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.fuseki.server ;

import static java.lang.String.format ;
import static org.apache.jena.fuseki.Fuseki.serverLog ;

import java.io.FileInputStream ;
import java.util.Arrays ;
import java.util.EnumSet ;
import java.util.List ;

import javax.servlet.DispatcherType ;

import org.apache.jena.atlas.lib.NotImplemented ;
import org.apache.jena.fuseki.Fuseki ;
import org.apache.jena.fuseki.FusekiException ;
import org.apache.jena.fuseki.servlets.FusekiFilter ;
import org.eclipse.jetty.security.* ;
import org.eclipse.jetty.security.authentication.BasicAuthenticator ;
import org.eclipse.jetty.server.Connector ;
import org.eclipse.jetty.server.Server ;
import org.eclipse.jetty.server.nio.BlockingChannelConnector ;
import org.eclipse.jetty.servlet.FilterHolder ;
import org.eclipse.jetty.servlet.ServletContextHandler ;
import org.eclipse.jetty.util.security.Constraint ;
import org.eclipse.jetty.webapp.WebAppContext ;
import org.eclipse.jetty.xml.XmlConfiguration ;

import com.hp.hpl.jena.sparql.util.Utils ;

/**
 * SPARQLServer is the Jena server instance which wraps/utilizes 
 * {@link org.eclipse.jetty.server.Server}. This class provides
 * immediate access to the {@link org.eclipse.jetty.server.Server#start()} and 
 * {@link org.eclipse.jetty.server.Server#stop()} commands as well as obtaining
 * instances of the server and server configuration. Finally we can obtain 
 * instances of {@link org.apache.jena.fuseki.server.ServerConfig}.
 */
public class SPARQLServer {
    static {
        Fuseki.init() ;
    }

    // Temporarary
    public static SPARQLServer  instance = null ;

    private Connector serverConnector = null ;
    private Connector mgtConnector = null ;
    
    private ServerConfig        serverConfig ;

    private Server              server         = null ;
    private static List<String> epDataset      = Arrays.asList("*") ;

    /**
     * Default constructor which requires a {@link org.apache.jena.fuseki.server.ServerConfig}
     * object as input. We use this config to specify (verbose) logging, enable compression
     * etc. 
     * @param config
     */
    public SPARQLServer(ServerConfig config) {
        this.serverConfig = config ;
        // Currently server-wide.
        Fuseki.verboseLogging = config.verboseLogging ;
        boolean webappBuild = true ;
        
        if ( webappBuild ) 
            buildServerWebapp(serverConfig.jettyConfigFile, config.enableCompression) ;
        else {
            ServletContextHandler context = buildServer(serverConfig.jettyConfigFile, config.enableCompression) ;
            // Filter to grab all request for dynamic dispatching.
            FilterHolder f = new FilterHolder(new FusekiFilter()) ;
            EnumSet<DispatcherType> es = EnumSet.allOf(DispatcherType.class) ; 
            context.addFilter(f, "/*", es);
            context.addEventListener(new FusekiServletContextListener());
            // No security.
        }
        
        instance = this ;
        mgtConnector = serverConnector ;
        // Datasets not initialized yet.
    }

    /**
     * Initialize the {@link SPARQLServer} instance.
     */
    public void start() {
        String now = Utils.nowAsString() ;
        serverLog.info(format("%s %s %s", Fuseki.NAME, Fuseki.VERSION, Fuseki.BUILD_DATE)) ;
        // This does not get set usefully for Jetty as we use it.
        // String jettyVersion = org.eclipse.jetty.server.Server.getVersion() ;
        // serverLog.info(format("Jetty %s",jettyVersion)) ;
        String host = server.getConnectors()[0].getHost() ;
        if ( host != null )
            serverLog.info("Incoming connections limited to " + host) ;
        serverLog.info(format("Started %s on port %d", now, server.getConnectors()[0].getPort())) ;

        try {
            server.start() ;
        } catch (java.net.BindException ex) {
            serverLog.error("SPARQLServer: Failed to start server: " + ex.getMessage()) ;
            System.exit(1) ;
        } catch (Exception ex) {
            serverLog.error("SPARQLServer: Failed to start server: " + ex.getMessage(), ex) ;
            System.exit(1) ;
        }
    }

    /**
     * Stop the {@link SPARQLServer} instance.
     */
    public void stop() {
        String now = Utils.nowAsString() ;
        serverLog.info(format("Stopped %s on port %d", now, server.getConnectors()[0].getPort())) ;
        try {
            server.stop() ;
        } catch (Exception ex) {
            Fuseki.serverLog.warn("SPARQLServer: Exception while stopping server: " + ex.getMessage(), ex) ;
        }
        FusekiConfig.removeJMX() ;
    }

    /**
     * Get the Jetty instance.
     * @return Server
     */
    public Server getServer() {
        return server ;
    }

    public int getServerPort() {
        return serverConnector.getPort() ;
    }

    public int getMgtPort() {
        return mgtConnector.getPort() ;
    }

    public void setMgtConnector(Connector connector) {
        mgtConnector = connector ;
    }

    /**
     * Obtain the {@link org.apache.jena.fuseki.server.ServerConfig}
     * @return ServerConfig
     */
    public ServerConfig getServerConfig() {
        return serverConfig ;
    }

    private ServletContextHandler buildServerWebapp(String jettyConfig, boolean enableCompression) {
        if ( jettyConfig != null )
            // --jetty-config=jetty-fuseki.xml
            // for detailed configuration of the server using Jetty features.
            configServer(jettyConfig) ;
        else
            defaultServerConfig(serverConfig.port, serverConfig.loopback) ;
        
        WebAppContext context = new WebAppContext();
        context.getServletContext().getContextHandler().setMaxFormContentSize(10 * 1000 * 1000) ;
        context.setDescriptor("war-web.xml");
        context.setResourceBase("pages");
        context.setContextPath("/");
        context.setParentLoaderPriority(true);  // Normal Java classloader behaviour.
        context.setErrorHandler(new FusekiErrorHandler()) ;
        server.setHandler(context) ;

        // XXX Security
        if ( jettyConfig == null && serverConfig.authConfigFile != null )
            security(context, serverConfig.authConfigFile) ;
        
        return context ;
    }
    
    private ServletContextHandler buildServer(String jettyConfig, boolean enableCompression) {
        throw new NotImplemented("Use the webapps setup") ;
    }
    
    private static void security(ServletContextHandler context, String authfile) {
        Constraint constraint = new Constraint() ;
        constraint.setName(Constraint.__BASIC_AUTH) ;
        constraint.setRoles(new String[]{"fuseki"}) ;
        constraint.setAuthenticate(true) ;

        ConstraintMapping mapping = new ConstraintMapping() ;
        mapping.setConstraint(constraint) ;
        mapping.setPathSpec("/*") ;

        IdentityService identService = new DefaultIdentityService() ;

        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler() ;
        securityHandler.addConstraintMapping(mapping) ;
        securityHandler.setIdentityService(identService) ;

        HashLoginService loginService = new HashLoginService("Fuseki Authentication", authfile) ;
        loginService.setIdentityService(identService) ;

        securityHandler.setLoginService(loginService) ;
        securityHandler.setAuthenticator(new BasicAuthenticator()) ;

        context.setSecurityHandler(securityHandler) ;

        serverLog.debug("Basic Auth Configuration = " + authfile) ;

    }

    private void configServer(String jettyConfig) {
        try {
            serverLog.info("Jetty server config file = " + jettyConfig) ;
            server = new Server() ;
            XmlConfiguration configuration = new XmlConfiguration(new FileInputStream(jettyConfig)) ;
            configuration.configure(server) ;
            serverConnector = server.getConnectors()[0] ;
        } catch (Exception ex) {
            serverLog.error("SPARQLServer: Failed to configure server: " + ex.getMessage(), ex) ;
            throw new FusekiException("Failed to configure a server using configuration file '" + jettyConfig + "'") ;
        }
    }

    private void defaultServerConfig(int port, boolean loopback) {
        // Server, with one NIO-based connector, large input buffer size (for
        // long URLs, POSTed forms (queries, updates)).
        server = new Server() ;

        // BlockingChannelConnector is better for pumping large responses back
        // but there have been observed problems with DirectMemory allocation
        // (-XX:MaxDirectMemorySize=1G does not help)
        // Connector connector = new SelectChannelConnector() ;

        // Connector and specific settings.
        BlockingChannelConnector bcConnector = new BlockingChannelConnector() ;
        // bcConnector.setUseDirectBuffers(false) ;

        Connector connector = bcConnector ;
        // Ignore. If set, then if this goes off, it keeps going off
        // and you get a lot of log messages.
        connector.setMaxIdleTime(0) ; // Jetty outputs a lot of messages if this
                                      // goes off.
        if ( loopback )
            connector.setHost("localhost");
        connector.setPort(port) ;
        // Some people do try very large operations ...
        connector.setRequestHeaderSize(64 * 1024) ;
        connector.setRequestBufferSize(5 * 1024 * 1024) ;
        connector.setResponseBufferSize(5 * 1024 * 1024) ;
        server.addConnector(connector) ;
        serverConnector = connector ;
    }
}