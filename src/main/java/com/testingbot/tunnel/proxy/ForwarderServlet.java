package com.testingbot.tunnel.proxy;


import com.testingbot.tunnel.App;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.proxy.ProxyServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.client.api.Request;

public class ForwarderServlet extends ProxyServlet {
    private App app;
    
    public ForwarderServlet(App app) {
        this.app = app;
    }
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }
    
    @Override
    protected void customizeProxyRequest(Request proxyRequest, HttpServletRequest request) {
        proxyRequest.header("TB-Tunnel", this.app.getServerIP());
        proxyRequest.header("TB-Credentials", this.app.getClientKey() + "_" + this.app.getClientSecret());
        
        for (String key : app.getCustomHeaders().keySet()) {
            proxyRequest.header(key, app.getCustomHeaders().get(key));
        }
    }
    
    @Override
    protected URI rewriteURI(HttpServletRequest request) {
        if (!validateDestination(request.getServerName(), request.getServerPort()))
            return null;

        StringBuffer uri = request.getRequestURL();
        try {
            return new URI(request.getScheme(), "", "127.0.0.1", 4444, uri.toString(), request.getQueryString(), "");
        } catch (URISyntaxException ex) {
            Logger.getLogger(ForwarderServlet.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
