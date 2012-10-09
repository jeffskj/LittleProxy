package com.rei.conman.proxy.route;

import java.net.URI;
import java.net.URISyntaxException;

import org.jboss.netty.handler.codec.http.HttpRequest;

import com.rei.conman.route.Destination;
import com.rei.conman.route.internal.MatchedRoute;
import com.rei.conman.route.internal.Routes;

public class ConmanRequestRewritingRouter implements RequestRouter, RequestRewriter {

    @Override
    public Destination determineDestination(HttpRequest request) {
        try {
            MatchedRoute route = Routes.match(new URI(request.getUri()).getPath());
            return route.getDestination(); 
        } catch (URISyntaxException e) {
        }
        return null;
    }
    
    @Override
    public HttpRequest rewrite(HttpRequest request, Destination destination) {
        request.setUri(destination.getResolvedUrl());
        return request;
    }
}