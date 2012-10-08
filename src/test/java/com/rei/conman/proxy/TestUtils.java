package com.rei.conman.proxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.rei.conman.proxy.route.RequestRewriter;
import com.rei.conman.proxy.route.RequestRouter;

public class TestUtils {

    private TestUtils() {}

    /**
     *  Creates and starts a proxy server that listens on given port.
     * @param port The port
     * @return The instance of proxy server
     */
    public static HttpProxyServer startProxyServer(int port) {
        final DefaultHttpProxyServer proxyServer = new DefaultHttpProxyServer(port);
        proxyServer.start(true, true);
        return proxyServer;
    }
    
    public static HttpProxyServer startProxyServer(int port, RequestRouter router, RequestRewriter rewriter) {
        ProxyConfig config = ProxyConfig.builder().port(port).requestRouter(router).requestRewriter(rewriter).build();
        
        final DefaultHttpProxyServer proxyServer = new DefaultHttpProxyServer(config);
        proxyServer.start(true, true);
        return proxyServer;
    }

    /**
     *  Creates and starts a proxy server that listens on given port.
     * @param port The port
     * @param chainProxyHostAndPort Proxy relay
     * @return The instance of proxy server
     */
    public static HttpProxyServer startProxyServer(int port, final String chainProxyHostAndPort) {
        final DefaultHttpProxyServer proxyServer = new DefaultHttpProxyServer(port);
        proxyServer.start(true, true);
        return proxyServer;
    }

    /**
     * Creates and starts embedded web server that is running on given port.
     * Each response has empty body  with HTTP OK status.
     *
     * @param port The port
     * @return Instance of Server
     * @throws Exception if failed to start
     */
    public static Server startWebServer(int port) throws Exception {
        final Server httpServer = new Server(port);
        httpServer.setAttribute("requests", new ArrayList<String>());
        httpServer.setHandler(new AbstractHandler() {
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                response.setStatus(HttpServletResponse.SC_OK);
                System.out.println("received request on URI: " + request.getRequestURI());
                getRequests(httpServer).add(request.getRequestURI());
                baseRequest.setHandled(true);
            }
        });
        httpServer.start();
        return httpServer;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getRequests(Server server) {
        return ((List<String>) server.getAttribute("requests"));
    }
    
    /**
     * Creates instance HttpClient that is configured to use proxy server.
     * The proxy server should run on localhost and given port
     * @param port the proxy port
     * @return instance of HttpClient
     */
    public static HttpClient createProxiedHttpClient(int port) {
        final HttpClient httpclient = new DefaultHttpClient();
        final HttpHost proxy = new HttpHost("localhost", port, "http");
        httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        return httpclient;
    }

}
