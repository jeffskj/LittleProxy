package com.rei.conman.proxy.route;

import org.jboss.netty.handler.codec.http.HttpRequest;

public interface RequestRewriter {
    HttpRequest rewrite(HttpRequest request, Route route);
}
