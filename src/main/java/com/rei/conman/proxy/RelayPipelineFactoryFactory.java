package com.rei.conman.proxy;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.rei.conman.route.Destination;

public interface RelayPipelineFactoryFactory {

    ChannelPipelineFactory getRelayPipelineFactory(Destination destination, HttpRequest httpRequest, 
        Channel browserToProxyChannel, RelayListener relayListener);

}
