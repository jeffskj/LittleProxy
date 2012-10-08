package com.rei.conman.proxy;

import static org.jboss.netty.channel.Channels.pipeline;

import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.net.ssl.SSLEngine;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rei.conman.proxy.ssl.SslContextFactory;

/**
 * Factory for creating pipelines for incoming requests to our listening socket.
 */
public class HttpServerPipelineFactory implements ChannelPipelineFactory, AllConnectionData {

    private static final Logger log = LoggerFactory.getLogger(HttpServerPipelineFactory.class);

    private static final boolean CACHE_ENABLED = false;

    private final ProxyAuthorizationManager authenticationManager;
    private final ChannelGroup channelGroup;

    private final ClientSocketChannelFactory clientSocketChannelFactory = new NioClientSocketChannelFactory(
            Executors.newCachedThreadPool(), Executors.newCachedThreadPool());

    private int numHandlers;
    private final RelayPipelineFactoryFactory relayPipelineFactoryFactory;

    private static final Timer TIMER = new HashedWheelTimer();

    private final ProxyConfig config;

    /**
     * Creates a new pipeline factory with the specified class for processing proxy authentication.
     * 
     * @param authorizationManager The manager for proxy authentication.
     * @param channelGroup The group that keeps track of open channels.
     * @param chainProxyManager upstream proxy server host and port or <code>null</code> if none
     *            used.
     * @param ksm The KeyStore manager.
     * @param relayPipelineFactoryFactory The relay pipeline factory factory.
     */
    public HttpServerPipelineFactory(final ProxyAuthorizationManager authorizationManager,
            final ChannelGroup channelGroup, ProxyConfig config,
            final RelayPipelineFactoryFactory relayPipelineFactoryFactory) {

        this.config = config;
        this.relayPipelineFactoryFactory = relayPipelineFactoryFactory;

        log.info("Creating server with keystore manager: {}", config.keyStoreManager());
        authenticationManager = authorizationManager;
        this.channelGroup = channelGroup;

        if (config.useJmx()) {
            setupJmx();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                clientSocketChannelFactory.releaseExternalResources();
            }
        }));
    }

    private void setupJmx() {
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            final Class<? extends AllConnectionData> clazz = getClass();
            final String pack = clazz.getPackage().getName();
            final String oName = pack + ":type=" + clazz.getSimpleName() + "-" + clazz.getSimpleName() + hashCode();
            log.info("Registering MBean with name: {}", oName);
            final ObjectName mxBeanName = new ObjectName(oName);
            if (!mbs.isRegistered(mxBeanName)) {
                mbs.registerMBean(this, mxBeanName);
            }
        } catch (final MalformedObjectNameException e) {
            log.error("Could not set up JMX", e);
        } catch (final InstanceAlreadyExistsException e) {
            log.error("Could not set up JMX", e);
        } catch (final MBeanRegistrationException e) {
            log.error("Could not set up JMX", e);
        } catch (final NotCompliantMBeanException e) {
            log.error("Could not set up JMX", e);
        }
    }

    public ChannelPipeline getPipeline() throws Exception {
        final ChannelPipeline pipeline = pipeline();

        log.info("Accessing pipeline");
        if (config.keyStoreManager() != null) {
            log.info("Adding SSL handler");
            final SslContextFactory scf = new SslContextFactory(config.keyStoreManager());
            final SSLEngine engine = scf.getServerContext().createSSLEngine();
            engine.setUseClientMode(false);
            pipeline.addLast("ssl", new SslHandler(engine));
        }

        // We want to allow longer request lines, headers, and chunks
        // respectively.
        pipeline.addLast("decoder", new HttpRequestDecoder(8192, 8192 * 2, 8192 * 2));
        pipeline.addLast("encoder", new ProxyHttpResponseEncoder(config.cacheManager()));

        final HttpRequestHandler httpRequestHandler = new HttpRequestHandler(config, authenticationManager,
                channelGroup, clientSocketChannelFactory, relayPipelineFactoryFactory);

        pipeline.addLast("idle", new IdleStateHandler(TIMER, 0, 0, 70));
        // pipeline.addLast("idleAware", new IdleAwareHandler("Client-Pipeline"));
        pipeline.addLast("idleAware", new IdleRequestHandler(httpRequestHandler));
        pipeline.addLast("handler", httpRequestHandler);
        numHandlers++;
        return pipeline;
    }

    public int getNumRequestHandlers() {
        return numHandlers;
    }
}
