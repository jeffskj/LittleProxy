package com.rei.conman.proxy;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.rei.conman.proxy.route.Route;

public class DefaultRelayPipelineFactoryFactory implements RelayPipelineFactoryFactory {

    private final ChannelGroup channelGroup;
    private final ProxyConfig config;

    public DefaultRelayPipelineFactoryFactory(ProxyConfig config, final ChannelGroup channelGroup) {
        this.config = config;
        this.channelGroup = channelGroup;
    }

    public ChannelPipelineFactory getRelayPipelineFactory(final Route route, final HttpRequest httpRequest,
            final Channel browserToProxyChannel, final RelayListener relayListener) {

        return new DefaultRelayPipelineFactory(route, httpRequest, relayListener, browserToProxyChannel, channelGroup,
                config);
    }

}