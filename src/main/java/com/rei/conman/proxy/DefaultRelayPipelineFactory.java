package com.rei.conman.proxy;

import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rei.conman.proxy.filter.HttpFilter;
import com.rei.conman.route.Destination;
import com.rei.conman.route.TargetSystem;

public class DefaultRelayPipelineFactory implements ChannelPipelineFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultRelayPipelineFactory.class);
    private static final Timer TIMER = new HashedWheelTimer();

    private final Destination destination;
    private final HttpRequest httpRequest;
    private final RelayListener relayListener;
    private final Channel browserToProxyChannel;

    private final ChannelGroup channelGroup;
    private final boolean filtersOff;
    private final ProxyConfig config;

    public DefaultRelayPipelineFactory(final Destination destination, final HttpRequest httpRequest,
            final RelayListener relayListener, final Channel browserToProxyChannel, final ChannelGroup channelGroup,
            ProxyConfig config) {
        this.destination = destination;
        this.httpRequest = httpRequest;
        this.relayListener = relayListener;
        this.browserToProxyChannel = browserToProxyChannel;

        this.channelGroup = channelGroup;
        this.config = config;

        filtersOff = config.responseFilters() == null;
    }

    public ChannelPipeline getPipeline() throws Exception {
        // Create a default pipeline implementation.
        final ChannelPipeline pipeline = pipeline();

        // We always include the request and response decoders
        // regardless of whether or not this is a URL we're
        // filtering responses for. The reason is that we need to
        // follow connection closing rules based on the response
        // headers and HTTP version.
        //
        // We also importantly need to follow the cache directives
        // in the HTTP response.
        final HttpResponseDecoder decoder;
        if (httpRequest.getMethod() == HttpMethod.HEAD) {
            decoder = new HttpResponseDecoder(8192, 8192 * 2, 8192 * 2) {
                @Override
                protected boolean isContentAlwaysEmpty(final HttpMessage msg) {
                    return true;
                }
            };
        } else {
            decoder = new HttpResponseDecoder(8192, 8192 * 2, 8192 * 2);
        }
        pipeline.addLast("decoder", decoder);

        TargetSystem targetSystem = destination.getTargetSystem(ProxyUtils.getProtocol(httpRequest));
        LOG.debug("Querying for host and port: {}", targetSystem.getHostAndPort());
        final boolean shouldFilter;
        final HttpFilter filter;
        if (filtersOff) {
            shouldFilter = false;
            filter = null;
        } else {
            filter = config.responseFilters().getFilter(targetSystem.getHostAndPort());
            if (filter == null) {
                LOG.info("No filter found");
                shouldFilter = false;
            } else {
                LOG.debug("Using filter: {}", filter);
                shouldFilter = filter.filterResponses(httpRequest);
                // We decompress and aggregate chunks for responses from
                // sites we're applying rules to.
                if (shouldFilter) {
                    pipeline.addLast("inflater", new HttpContentDecompressor());
                    pipeline.addLast("aggregator", new HttpChunkAggregator(filter.getMaxResponseSize()));// 2048576));
                }
            }
            LOG.debug("Filtering: " + shouldFilter);
        }

        // The trick here is we need to determine whether or not
        // to cache responses based on the full URI of the request.
        // This request encoder will only get the URI without the
        // host, so we just have to be aware of that and construct
        // the original.
        final HttpRelayingHandler handler;
        if (shouldFilter) {
            LOG.info("Creating relay handler with filter");
            handler = new HttpRelayingHandler(browserToProxyChannel, channelGroup, filter, relayListener,
                    targetSystem.getHostAndPort());
        } else {
            LOG.info("Creating non-filtering relay handler");
            handler = new HttpRelayingHandler(browserToProxyChannel, channelGroup, relayListener,
                    targetSystem.getHostAndPort());
        }

        final ProxyHttpRequestEncoder encoder = new ProxyHttpRequestEncoder(handler, config, destination);
        pipeline.addLast("encoder", encoder);

        // We close idle connections to remote servers after the
        // specified timeouts in seconds. If we're sending data, the
        // write timeout should be reasonably low. If we're reading
        // data, however, the read timeout is more relevant.
        final HttpMethod method = httpRequest.getMethod();

        // Could be any protocol if it's connect, so hard to say what the
        // timeout should be, if any.
        if (!method.equals(HttpMethod.CONNECT)) {
            final int readTimeoutSeconds;
            final int writeTimeoutSeconds;
            if (method.equals(HttpMethod.POST) || method.equals(HttpMethod.PUT)) {
                readTimeoutSeconds = 0;
                writeTimeoutSeconds = 70;
            } else {
                readTimeoutSeconds = 70;
                writeTimeoutSeconds = 0;
            }
            pipeline.addLast("idle", new IdleStateHandler(TIMER, readTimeoutSeconds, writeTimeoutSeconds, 0));
            pipeline.addLast("idleAware", new IdleAwareHandler("Relay-Handler"));
        }
        pipeline.addLast("handler", handler);
        return pipeline;
    }
}