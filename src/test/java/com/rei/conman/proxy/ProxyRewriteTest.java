package com.rei.conman.proxy;

import static com.rei.conman.proxy.TestUtils.getRequests;
import static com.rei.conman.proxy.TestUtils.startProxyServer;
import static com.rei.conman.proxy.TestUtils.startWebServer;
import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.eclipse.jetty.server.Server;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.rei.conman.proxy.route.ConmanRequestRewritingRouter;
import com.rei.conman.proxy.route.RequestRewriter;
import com.rei.conman.proxy.route.RequestRouter;
import com.rei.conman.route.Destination;
import com.rei.conman.route.TargetSystemDefinition;
import com.rei.conman.route.internal.Route;
import com.rei.conman.route.internal.Routes;

public class ProxyRewriteTest {
    private static final int WEB_SERVER_PORT = 1080;
    private static final int PROXY_PORT = 8081;
    private static final String PROXY_HOST_AND_PORT = "localhost:8081";

    private Server webServer;
    private Server webServer2;
    private HttpProxyServer proxyServer;
    private HttpClient httpclient = new DefaultHttpClient(new PoolingClientConnectionManager());

    @Test
    public void testReverseProxy() throws Exception {
        // Given
        webServer = startWebServer(WEB_SERVER_PORT);
        webServer2 = startWebServer(WEB_SERVER_PORT + 1);
        proxyServer = startProxyServer(PROXY_PORT, new TestRequestRewriter(), new TestRequestRewriter());

        // When
        HttpResponse response = httpclient.execute(new HttpGet("http://" + PROXY_HOST_AND_PORT + "/1"));
        HttpResponse response2 = httpclient.execute(new HttpGet("http://" + PROXY_HOST_AND_PORT + "/2"));

        // Then
        assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(HttpServletResponse.SC_OK, response2.getStatusLine().getStatusCode());

        assertEquals("/one", getRequests(webServer).get(0));
        assertEquals("/two", getRequests(webServer2).get(0));

        webServer.stop();
        webServer2.stop();
        proxyServer.stop();
    }

    @Test
    public void testProxyConmanRewrite() throws Exception {
        // Given
        ConmanRequestRewritingRouter conmanRewriter = new ConmanRequestRewritingRouter();
        
        webServer = startWebServer(WEB_SERVER_PORT);
        proxyServer = startProxyServer(PROXY_PORT, conmanRewriter, conmanRewriter);
        Route route = Route.parse("/1", "/one");
        
        TargetSystemDefinition targetSystemDefinition = new TargetSystemDefinition();
        targetSystemDefinition.setHost("localhost");
        targetSystemDefinition.setPort(WEB_SERVER_PORT);
        
        route.setTargetSystemDefinition(targetSystemDefinition);
        Routes.update(ImmutableSet.of(route));
        
        // When
        HttpResponse response = httpclient.execute(new HttpGet("http://" + PROXY_HOST_AND_PORT + "/1"));
        
        // Then
        assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals("/one", getRequests(webServer).get(0));

        webServer.stop();
        proxyServer.stop();
    }

    public class TestRequestRewriter implements RequestRouter, RequestRewriter {
        public Destination determineDestination(HttpRequest request) {
            if (request.getUri().contains("/1")) {
                return new PassThroughDestination("localhost:" + WEB_SERVER_PORT, "/one");
            }
            if (request.getUri().contains("/2")) {
                return new PassThroughDestination("localhost:" + (WEB_SERVER_PORT + 1), "/two");
            }
            return null;
        }

        public HttpRequest rewrite(HttpRequest request, Destination destination) {
            request.setUri(destination.getResolvedUrl());
            return request;
        }

    }

}
