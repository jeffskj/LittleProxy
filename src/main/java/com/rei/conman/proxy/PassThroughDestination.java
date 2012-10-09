package com.rei.conman.proxy;

import com.rei.conman.route.Destination;
import com.rei.conman.route.Protocol;
import com.rei.conman.route.RouteTargetType;
import com.rei.conman.route.TargetSystem;

public class PassThroughDestination implements Destination {

    private String url;
    private TargetSystem targetSystem = new TargetSystem();

    public PassThroughDestination(String hostAndPort, String uri) {
        url = uri;
        String[] parts = hostAndPort.split(":");
        targetSystem.setHost(parts[0]);
        targetSystem.setPort(Integer.parseInt(parts[1]));
    }
    
    @Override
    public Protocol getForcedProtocol() {
        return null;
    }

    @Override
    public RouteTargetType getType() {
        return RouteTargetType.FORWARD;
    }

    @Override
    public TargetSystem getTargetSystem(Protocol protocol) {
        return targetSystem;
    }
    
    @Override
    public String getResolvedUrl() {
        return url;
    }

}
