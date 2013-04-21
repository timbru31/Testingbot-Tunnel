package com.testingbot.tunnel;

import com.testingbot.tunnel.proxy.CustomConnectHandler;
import com.testingbot.tunnel.proxy.TunnelProxyServlet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletHolder;

import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
/**
 *
 * @author TestingBot
 */
public class HttpProxy {
    
    public HttpProxy(App app) {
        try {
            Server server = new Server();
            
             // HTTP Configuration
            HttpConfiguration http_config = new HttpConfiguration();
            http_config.setSecureScheme("https");
            //http_config.setSecurePort(443);
            http_config.setOutputBufferSize(32768);
            
            // HTTP connector
            ServerConnector http = new ServerConnector(server,new HttpConnectionFactory(http_config));        
            http.setPort(8087);
            http.setIdleTimeout(30000);
            
//            // SSL Context Factory for HTTPS and SPDY
//            SslContextFactory sslContextFactory = new SslContextFactory();
//            
//            // HTTPS Configuration
//            HttpConfiguration https_config = new HttpConfiguration(http_config);
//            https_config.addCustomizer(new SecureRequestCustomizer());
//
//            // HTTPS connector
//            ServerConnector https = new ServerConnector(server,
//                new SslConnectionFactory(sslContextFactory,"http/1.1"),
//                new HttpConnectionFactory(https_config));
//            https.setPort(8443);
//            https.setIdleTimeout(500000);

            // Set the connectors
            server.setConnectors(new Connector[] { http });

            // Set a handler
            HandlerCollection handlers = new HandlerCollection();
            
            ServletContextHandler context = new ServletContextHandler(handlers, "/", ServletContextHandler.SESSIONS);
            ServletHolder proxyServlet = new ServletHolder(TunnelProxyServlet.class);
            if (app.getFastFail() != null && app.getFastFail().length > 0) {
                StringBuilder sb = new StringBuilder();
                for (String domain : app.getFastFail()) {
                    sb.append(domain).append(",");
                }
                proxyServlet.setInitParameter("blackList", sb.toString());
            }
            context.addServlet(proxyServlet, "/*");
            
            // Setup proxy handler to handle CONNECT methods
            ConnectHandler connectHandler = new CustomConnectHandler();
//            if (app.getFastFail() != null && app.getFastFail().length > 0) {
//                for (String domain : app.getFastFail()) {
//                   proxy.addBlack(domain);
//                }
//            }
            
            handlers.setHandlers(new Handler[]{context, connectHandler});
            server.setHandler(handlers);
            // Start the server
            server.start();
            server.join();
            
            Thread shutDownHook = new Thread(new ShutDownHook(server));

            Runtime.getRuntime().addShutdownHook(shutDownHook);
        } catch (Exception ex) {
            Logger.getLogger(HttpProxy.class.getName()).log(Level.INFO, "Could not set up local http proxy. Please make sure this program can open port 8087 on this computer.");
            Logger.getLogger(HttpProxy.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private class ShutDownHook implements Runnable {
        private final Server proxy;

        ShutDownHook(Server proxy) {
          this.proxy = proxy;
        }

        public void run() {
            try {
                proxy.stop();
            } catch (Exception ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
      }
}

