package org.littleshoot.proxy;

import org.jboss.netty.handler.codec.http.HttpRequest;

public interface RequestRouter {
    Route determineRoute(HttpRequest request);
}
