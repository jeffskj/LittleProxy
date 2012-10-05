package org.littleshoot.proxy;

public class Route {
    private final String host;
    private final int port;
    private final Object context;
    
    public Route(String hostAndPort) {
        String[] parts = hostAndPort.split(":");
        host = parts[0];
        port = Integer.parseInt(parts[1]);
        context = null;
    }
    
    public Route(String host, int port, Object context) {
        this.host = host;
        this.port = port;
        this.context = context;
    }

    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }

    public String getHostAndPort() {
        return host + ":" + port;
    }
    
    public Object getContext() {
        return context;
    }
}
