package org.littleshoot.proxy;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.http.HttpRequest;

public class DefaultRelayPipelineFactoryFactory 
    implements RelayPipelineFactoryFactory {
    
    private final ChainProxyManager chainProxyManager;
    private final ChannelGroup channelGroup;
    private final HttpRequestFilter requestFilter;
    private final HttpResponseFilters responseFilters;
    private final RequestRewriter rewriter;

    public DefaultRelayPipelineFactoryFactory(
        final ChainProxyManager chainProxyManager, 
        final HttpResponseFilters responseFilters, 
        final HttpRequestFilter requestFilter,
        final RequestRewriter rewriter,
        final ChannelGroup channelGroup) {
        this.chainProxyManager = chainProxyManager;
        this.responseFilters = responseFilters;
        this.rewriter = rewriter;
        this.channelGroup = channelGroup;
        this.requestFilter = requestFilter;
    }
    
    public ChannelPipelineFactory getRelayPipelineFactory(final Route route, 
        final HttpRequest httpRequest, final Channel browserToProxyChannel,
        final RelayListener relayListener) {
	
        // TODO: fix this to work with route
        String hostAndPort = chainProxyManager == null
            ? null : chainProxyManager.getChainProxy(httpRequest);
        if (hostAndPort == null) {
            hostAndPort = ProxyUtils.parseHostAndPort(httpRequest);
        }
        
        return new DefaultRelayPipelineFactory(route, httpRequest, 
            relayListener, browserToProxyChannel, channelGroup, rewriter, responseFilters, 
            requestFilter, chainProxyManager);
    }
    
}