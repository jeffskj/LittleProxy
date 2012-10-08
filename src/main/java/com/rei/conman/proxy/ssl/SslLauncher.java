package com.rei.conman.proxy.ssl;

import com.rei.conman.proxy.DefaultHttpProxyServer;
import com.rei.conman.proxy.HttpProxyServer;
import com.rei.conman.proxy.ProxyConfig;
import com.rei.conman.proxy.filter.HttpFilter;
import com.rei.conman.proxy.filter.HttpResponseFilters;

/**
 * Launches a new HTTP proxy using SSL and a self-signed certificate.
 */
public class SslLauncher {

    /**
     * Starts the proxy from the command line.
     * 
     * @param args Any command line arguments.
     */
    public static void main(final String... args) {
        final int defaultPort = 8080;
        int port;
        if (args.length > 0) {
            final String arg = args[0];
            try {
                port = Integer.parseInt(arg);
            } catch (final NumberFormatException e) {
                port = defaultPort;
            }
        } else {
            port = defaultPort;
        }

        System.out.println("About to start SSL server on port: " + port);
        final HttpResponseFilters responseFilters = new HttpResponseFilters() {
            public HttpFilter getFilter(final String hostAndPort) {
                return null;
            }
        };
        
        ProxyConfig config = ProxyConfig.builder().port(port)
                .responseFilters(responseFilters)
                .keyStoreManager(new SelfSignedKeyStoreManager()).build();
        
        final HttpProxyServer server = new DefaultHttpProxyServer(config);
        System.out.println("About to start...");
        server.start();
    }
}
