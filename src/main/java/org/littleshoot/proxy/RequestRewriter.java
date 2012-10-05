package org.littleshoot.proxy;

import org.jboss.netty.handler.codec.http.HttpRequest;

public interface RequestRewriter {
    HttpRequest rewrite(HttpRequest request, Route route);
}
