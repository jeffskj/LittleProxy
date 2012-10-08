package com.rei.conman.proxy.filter;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.rei.conman.proxy.HttpRequestMatcher;

/**
 * Default implementation of a class for processing HTTP request rules.
 */
public class DefaultHttpFilter implements HttpFilter {

    /**
     * The request rules. Note that this should never change to avoid 
     * having to synchronize.
     */
    private final Collection<HttpRequestMatcher> requestMatchers;
    private final HttpResponseFilter responseFilter;
    
    /**
     * Creates a new set of HTTP request rules.
     * 
     * @param responseFilter The class that filters responses to matching
     * requests.
     * @param requestRules The request rules for this set.
     */
    public DefaultHttpFilter (final HttpResponseFilter responseFilter,
        final HttpRequestMatcher... requestRules) {
        this.responseFilter = responseFilter;
        this.requestMatchers = Arrays.asList(requestRules);
    }

    public boolean filterResponses(final HttpRequest httpRequest) {
        for (final HttpRequestMatcher rule : requestMatchers) {
            if (!rule.filterResponses(httpRequest)) {
                return false;
            }
        }
        return true;
    }

    public HttpResponse filterResponse(final HttpRequest request, 
        final HttpResponse response) {
        return responseFilter.filterResponse(request, response);
    }
    
    public int getMaxResponseSize() {
        return 1024 * 200;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Request Matchers: ");
        sb.append(requestMatchers);
        sb.append("\nResponse Filter: ");
        sb.append(responseFilter);
        return sb.toString();
    }
}
