package com.testingbot.tunnel;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.testingbot.tunnel.proxy.ForwarderServlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
/**
 *
 * @author TestingBot
 */
public class HttpForwarder {
    
    public HttpForwarder(App app) {
        try {
            Server server = new Server();
            HttpConfiguration http_config = new HttpConfiguration();
            http_config.setSecureScheme("https");
          //  http_config.setSecurePort(443);
            http_config.setOutputBufferSize(32768);

            // HTTP connector
            ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));        
            http.setPort(Integer.parseInt(app.getSeleniumPort()));
            http.setIdleTimeout(30000);
            
//            HttpConfiguration https_config = new HttpConfiguration(http_config);
//            https_config.addCustomizer(new SecureRequestCustomizer());
//            
//            SslContextFactory sslContextFactory = new SslContextFactory();
//            ServerConnector https = new ServerConnector(server,
//                new SslConnectionFactory(sslContextFactory,"http/1.1"),
//                new HttpConnectionFactory(https_config));
//            https.setPort(8443);
//            https.setIdleTimeout(500000);

            // Set the connectors
            server.setConnectors(new Connector[] { http });
            
            ServletHandler servletHandler = new ServletHandler();
            servletHandler.addServletWithMapping(new ServletHolder(new ForwarderServlet(app)), "/*");
            server.setHandler(servletHandler);
            
            server.start();
            server.join();
        } catch (Exception ex) {
            Logger.getLogger(HttpForwarder.class.getName()).log(Level.INFO, "Could not set up local forwarder. Please make sure this program can open port 4445 on this computer.");
            Logger.getLogger(HttpForwarder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

