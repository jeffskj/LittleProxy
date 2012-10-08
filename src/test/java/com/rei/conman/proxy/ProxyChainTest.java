package com.rei.conman.proxy;

import static com.rei.conman.proxy.TestUtils.createProxiedHttpClient;
import static com.rei.conman.proxy.TestUtils.getRequests;
import static com.rei.conman.proxy.TestUtils.startProxyServer;
import static com.rei.conman.proxy.TestUtils.startWebServer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.eclipse.jetty.server.Server;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.junit.Test;

import com.rei.conman.proxy.HttpProxyServer;
import com.rei.conman.proxy.route.RequestRewriter;
import com.rei.conman.proxy.route.RequestRouter;
import com.rei.conman.proxy.route.Route;

public class ProxyChainTest {
    private static final int WEB_SERVER_PORT = 1080;
    private static final HttpHost WEB_SERVER_HOST = new HttpHost("localhost", WEB_SERVER_PORT);
    private static final int PROXY_PORT = 8081;
    private static final String PROXY_HOST_AND_PORT = "localhost:8081";
    private static final int ANOTHER_PROXY_PORT = 8082;

    private Server webServer;
    private Server webServer2;
    private HttpProxyServer proxyServer;
    private HttpProxyServer anotherProxyServer;
    private HttpClient httpclient;

    @Test public void testSingleProxy() throws Exception {
        // Given
        webServer = startWebServer(WEB_SERVER_PORT);
        proxyServer = startProxyServer(PROXY_PORT);
        httpclient = createProxiedHttpClient(PROXY_PORT);

        // When
        final HttpResponse response = httpclient.execute(WEB_SERVER_HOST, new HttpGet("/"));

        // Then
        assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
        assertNotNull(response.getFirstHeader("Via"));

        webServer.stop();
        proxyServer.stop();
    }
    
    @Test public void testReverseProxy() throws Exception {
        // Given
        webServer = startWebServer(WEB_SERVER_PORT);
        webServer2 = startWebServer(WEB_SERVER_PORT + 1);
        proxyServer = startProxyServer(PROXY_PORT, new TestRequestRouter(), new TestRequestRewriter());
        httpclient = new DefaultHttpClient(new ThreadSafeClientConnManager());

        // When
        final HttpResponse response = httpclient.execute(new HttpGet("http://" + PROXY_HOST_AND_PORT + "/1"));
        final HttpResponse response2 = httpclient.execute(new HttpGet("http://" + PROXY_HOST_AND_PORT + "/2"));
        
        // Then
        assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(HttpServletResponse.SC_OK, response2.getStatusLine().getStatusCode());
//        assertNotNull(response.getFirstHeader("Via"));

        assertEquals("/one", getRequests(webServer).get(0));
        assertEquals("/two", getRequests(webServer2).get(0));
        
        webServer.stop();
        webServer2.stop();
        proxyServer.stop();
    }

    @Test public void testChainedProxy() throws Exception {
        // Given
        webServer = startWebServer(WEB_SERVER_PORT);
        proxyServer = startProxyServer(PROXY_PORT);
        anotherProxyServer = startProxyServer(ANOTHER_PROXY_PORT, PROXY_HOST_AND_PORT);
        httpclient = createProxiedHttpClient(ANOTHER_PROXY_PORT);

        // When
        final HttpResponse response = httpclient.execute(WEB_SERVER_HOST, new HttpGet("/"));

        // Then
        assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
        assertNotNull(response.getFirstHeader("Via"));

        webServer.stop();
        anotherProxyServer.stop();
        proxyServer.stop();
    }

    public class TestRequestRouter implements RequestRouter {
        
        public Route determineRoute(HttpRequest request) {
            if (request.getUri().contains("/1")) {
                return new Route("localhost", WEB_SERVER_PORT, "/one");
           }
            if (request.getUri().contains("/2")) {
                 return new Route("localhost", WEB_SERVER_PORT + 1, "/two");
            }
            return null;
        }
        
    }
    
    public class TestRequestRewriter implements RequestRewriter {

        public HttpRequest rewrite(HttpRequest request, Route route) {
            request.setUri(route.getContext().toString());
            return request;
        }

    }

    
}
