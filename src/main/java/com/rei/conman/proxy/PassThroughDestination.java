package com.rei.conman.proxy;

import com.rei.conman.route.Destination;
import com.rei.conman.route.Protocol;
import com.rei.conman.route.RouteTargetType;
import com.rei.conman.route.TargetSystem;
import com.rei.conman.route.TargetSystemDefinition;

public class PassThroughDestination implements Destination {

    private String url;
    private TargetSystemDefinition targetSystemDef;

    public PassThroughDestination(TargetSystemDefinition targetSystemDef, String uri) {
        this.targetSystemDef = targetSystemDef;
        url = uri;
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
        return targetSystemDef.getTargetForProtocol(protocol);
    }
    
    @Override
    public String getResolvedUrl() {
        return url;
    }

}
