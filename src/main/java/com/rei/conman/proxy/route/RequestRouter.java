package com.rei.conman.proxy.route;

import org.jboss.netty.handler.codec.http.HttpRequest;

import com.rei.conman.route.Destination;

public interface RequestRouter {
    Destination determineDestination(HttpRequest request);
}
