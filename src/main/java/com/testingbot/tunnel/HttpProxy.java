package com.testingbot.tunnel;

import com.testingbot.tunnel.proxy.CustomConnectHandler;
import com.testingbot.tunnel.proxy.TunnelProxyServlet;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
/**
 *
 * @author TestingBot
 */
public class HttpProxy {
    private App app;
    private int randomNumber = (int )(Math.random() * 50 + 1);
    
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
            
            Thread shutDownHook = new Thread(new ShutDownHook(server));

            Runtime.getRuntime().addShutdownHook(shutDownHook);
        } catch (Exception ex) {
            Logger.getLogger(HttpProxy.class.getName()).log(Level.INFO, "Could not set up local http proxy. Please make sure this program can open port 8087 on this computer.");
            Logger.getLogger(HttpProxy.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private ServerSocket _findAvailableSocket() {
        int[] ports = {80, 888, 2000, 2001, 2020, 2222, 3000, 3001, 3030, 3333, 4000, 4001, 4040, 4502, 4503, 5000, 5001, 5050, 5555, 6000, 6001, 6060, 6666, 7000, 7070, 7777, 8000, 8001, 8080, 8888, 9000, 9001, 9090, 9999};
        
        for (int port : ports) {
            try {
                return new ServerSocket(port);
            } catch (IOException ex) {
                continue; // try next port
            }
        }
        
        return null;
    }
    
    public boolean testProxy() {
        // find a free port, create a webserver, make a request to the proxy endpoint, expect it to arrive here.
        
        ServerSocket serverSocket;
        int port;
        try {
            serverSocket = _findAvailableSocket();
            if (serverSocket == null) {
                return true;
            }
            
            port = serverSocket.getLocalPort();
            serverSocket.close();
        } catch (Exception ex) {
            // no port available? assume everything is ok
            return true;
        }
        
        Server server = new Server(port);
        server.setHandler(new TestHandler());
        try {
            server.start();
        } catch (Exception e) {
            return true;
        }
        
        try {
            HttpClient httpClient = new HttpClient();
            httpClient.start();
            String url = "https://" + (app.getRegion().equalsIgnoreCase("US") ? "api.testingbot.com" : "api-eu.testingbot.com") + "/v1/tunnel/test";
            
            InputStreamResponseListener listener = new InputStreamResponseListener();
            
            ContentResponse response = httpClient
            .POST(url)
            .param("client_key", app.getClientKey())
            .param("client_secret", app.getClientSecret())
            .param("tunnel_id", Integer.toString(app.getTunnelID()))
            .param("test_port", Integer.toString(port))
            .send();
            
            String output = response.getContentAsString();
            
            try {
                server.stop();
            } catch (Exception ex) {
                
            }
            
            return ((response.getStatus() == 201) && (output.indexOf("test=" + this.randomNumber) > -1));
        } catch (Exception ex) {
            return true;
        }
    }
    
    private class TestHandler extends AbstractHandler {

        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            response.getWriter().println("test=" + Integer.toString(randomNumber));
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

