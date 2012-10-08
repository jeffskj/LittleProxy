package com.rei.conman.proxy;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP proxy server.
 */
public class DefaultHttpProxyServer implements HttpProxyServer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ChannelGroup allChannels = new DefaultChannelGroup("HTTP-Proxy-Server");

    private final ServerBootstrap serverBootstrap;

    private final ProxyAuthorizationManager authenticationManager = new DefaultProxyAuthorizationManager();
    private final ProxyConfig config;

    /**
     * Creates a new proxy server.
     * 
     * @param port The port the server should run on.
     */
    public DefaultHttpProxyServer(final int port) {
        this(ProxyConfig.builder().port(port).build());
    }

    public DefaultHttpProxyServer(ProxyConfig config) {
        this.config = config;

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            public void uncaughtException(final Thread t, final Throwable e) {
                log.error("Uncaught throwable", e);
            }
        });

        serverBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()));
    }

    public void start() {
        start(false, true);
    }

    public void start(final boolean localOnly, final boolean anyAddress) {
        log.info("Starting proxy on port: " + config.port());
        final HttpServerPipelineFactory factory = new HttpServerPipelineFactory(authenticationManager, allChannels, config, 
               new DefaultRelayPipelineFactoryFactory(config, allChannels));
        serverBootstrap.setPipelineFactory(factory);

        // Binding only to localhost can significantly improve the security of
        // the proxy.
        InetSocketAddress isa;
        if (localOnly) {
            isa = new InetSocketAddress("127.0.0.1", config.port());
        } else if (anyAddress) {
            isa = new InetSocketAddress(config.port());
        } else {
            try {
                isa = new InetSocketAddress(NetworkUtils.getLocalHost(), config.port());
            } catch (final UnknownHostException e) {
                log.error("Could not get local host?", e);
                isa = new InetSocketAddress(config.port());
            }
        }
        final Channel channel = serverBootstrap.bind(isa);
        allChannels.add(channel);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                stop();
            }
        }));

        /*
         * final ServerBootstrap sslBootstrap = new ServerBootstrap( new
         * NioServerSocketChannelFactory( Executors.newCachedThreadPool(),
         * Executors.newCachedThreadPool())); sslBootstrap.setPipelineFactory(new
         * HttpsServerPipelineFactory()); sslBootstrap.bind(new InetSocketAddress("127.0.0.1",
         * 8443));
         */
    }

    public void stop() {
        log.info("Shutting down proxy");
        final ChannelGroupFuture future = allChannels.close();
        future.awaitUninterruptibly(6 * 1000);
        serverBootstrap.releaseExternalResources();
        log.info("Done shutting down proxy");
    }

    public void addProxyAuthenticationHandler(final ProxyAuthorizationHandler pah) {
        authenticationManager.addHandler(pah);
    }
}
