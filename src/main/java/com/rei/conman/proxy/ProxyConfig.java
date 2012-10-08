package com.rei.conman.proxy;

import java.util.concurrent.Future;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.rei.conman.proxy.cache.ProxyCacheManager;
import com.rei.conman.proxy.filter.HttpRequestFilter;
import com.rei.conman.proxy.filter.HttpResponseFilters;
import com.rei.conman.proxy.route.RequestRewriter;
import com.rei.conman.proxy.route.RequestRouter;
import com.rei.conman.proxy.ssl.KeyStoreManager;

/**
 * Simple class for storing configuration. 
 */
public class ProxyConfig {
    private boolean useJmx = false;
    private int port;
    private HttpResponseFilters responseFilters;
    private KeyStoreManager ksm;
    private HttpRequestFilter requestFilter;
    private RequestRouter router;
    private RequestRewriter rewriter;
    
    private ProxyCacheManager cacheManager = new ProxyCacheManager() {

        public boolean returnCacheHit(final HttpRequest request, final Channel channel) {
            return false;
        }

        public Future<String> cache(final HttpRequest originalRequest, final HttpResponse httpResponse,
                final Object response, final ChannelBuffer encoded) {
            return null;
        }
    };
    
    private ProxyConfig(){}

    /**
     * Returns whether or not JMX is turned on.
     * 
     * @return <code>true</code> if JMX is turned on, otherwise 
     * <code>false</code>.
     */
    public boolean useJmx() {
        return useJmx;
    }
    
    public int port() {
        return port;
    }
    
    public HttpResponseFilters responseFilters() {
        return responseFilters;
    }

    public KeyStoreManager keyStoreManager() {
        return ksm;
    }

    public HttpRequestFilter requestFilter() {
        return requestFilter;
    }

    public RequestRouter requestRouter() {
        return router;
    }

    public RequestRewriter requestRewriter() {
        return rewriter;
    }

    public ProxyCacheManager cacheManager() {
        return cacheManager;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        ProxyConfig config = new ProxyConfig();
        
        private Builder() {}
        
        /**
         * Whether or not to use JMX -- defaults to false.
         * 
         * @param useJmx Whether or not to use JMX.
         */
        public Builder useJmx(boolean useJmx) {
            config.useJmx = useJmx;
            return this;
        }
        
        public Builder port(int port) {
            config.port = port;
            return this;
        }
        
        public Builder responseFilters(HttpResponseFilters filters) {
            config.responseFilters = filters;
            return this;
        }
        
        public Builder keyStoreManager(KeyStoreManager ksm) {
            config.ksm = ksm;
            return this;
        }
        
        public Builder requestFilter(HttpRequestFilter requestFilter) {
            config.requestFilter = requestFilter;
            return this;
        }
        
        public Builder requestRouter(RequestRouter router) {
            config.router = router;
            return this;
        }
        
        public Builder requestRewriter(RequestRewriter rewriter) {
            config.rewriter = rewriter;
            return this;
        }
        
        public ProxyConfig build() {
            return config;
        }
    }
}
