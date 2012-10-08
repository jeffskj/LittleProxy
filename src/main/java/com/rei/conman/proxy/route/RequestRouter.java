package com.rei.conman.proxy.route;

import org.jboss.netty.handler.codec.http.HttpRequest;

public interface RequestRouter {
    Route determineRoute(HttpRequest request);
}
