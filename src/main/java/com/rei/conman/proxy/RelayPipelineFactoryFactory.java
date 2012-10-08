package com.rei.conman.proxy;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.rei.conman.proxy.route.Route;

public interface RelayPipelineFactoryFactory {

    ChannelPipelineFactory getRelayPipelineFactory(Route route, HttpRequest httpRequest, 
        Channel browserToProxyChannel, RelayListener relayListener);

}
