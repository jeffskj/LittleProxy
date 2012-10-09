package com.rei.conman.proxy;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rei.conman.route.Destination;

/**
 * Request encoder for the proxy. This is necessary because we need to have access to the most
 * recent request message on this connection to determine caching rules.
 */
public class ProxyHttpRequestEncoder extends HttpRequestEncoder {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyHttpRequestEncoder.class);
    private final HttpRelayingHandler relayingHandler;
    private final Destination destination;
    private final ProxyConfig config;

    /**
     * Creates a new request encoder.
     * 
     * @param handler The class that handles relaying all data along this connection. We need this
     *            to synchronize caching rules for each request and response pair.
     * @param requestFilter The filter for requests.
     */
    public ProxyHttpRequestEncoder(final HttpRelayingHandler handler, ProxyConfig config, final Destination destination) {
        relayingHandler = handler;
        this.config = config;
        this.destination = destination;
    }

    @Override
    protected Object encode(final ChannelHandlerContext ctx, final Channel channel, final Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            // The relaying handler needs to know all the headers, including
            // hop-by-hop headers, of the original request, particularly
            // for determining whether or not to close the connection to the
            // browser, so we give it the original and copy the original
            // to modify it just before writing it on the wire.
            final HttpRequest request = (HttpRequest) msg;
            relayingHandler.requestEncoded(request);

            HttpRequest toSend = ProxyUtils.copyHttpRequest(request, false);

            if (config.requestFilter() != null) {
                config.requestFilter().filter(toSend);
            }

            if (config.requestRewriter() != null) {
                toSend = config.requestRewriter().rewrite(toSend, destination);
            }
            // LOG.info("Writing modified request: {}", httpRequestCopy);
            return super.encode(ctx, channel, toSend);
        }
        return super.encode(ctx, channel, msg);
    }
}
