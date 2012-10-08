package com.rei.conman.proxy;

import org.junit.Test;

import static com.rei.conman.proxy.ProxyUtils.parseHost;
import static com.rei.conman.proxy.ProxyUtils.parseHostAndPort;
import static org.junit.Assert.assertEquals;

/**
 * Test for proxy utilities.
 */
public class ProxyUtilsTest {

    @Test
    public void testParseHost() throws Exception {
        assertEquals("www.test.com", parseHost("http://www.test.com"));
        assertEquals("www.test.com", parseHost("http://www.test.com:80/test"));
        assertEquals("www.test.com", parseHost("https://www.test.com:80/test"));
        assertEquals("www.test.com", parseHost("www.test.com:80/test"));
        assertEquals("www.test.com", parseHost("www.test.com"));
    }

    @Test
    public void testParseHostAndPort() throws Exception {
        assertEquals("www.test.com:80", parseHostAndPort("http://www.test.com:80/test"));
        assertEquals("www.test.com:80", parseHostAndPort("https://www.test.com:80/test"));
        assertEquals("www.test.com:80", parseHostAndPort("www.test.com:80/test"));
        assertEquals("www.test.com", parseHostAndPort("http://www.test.com"));
        assertEquals("www.test.com", parseHostAndPort("www.test.com"));
    }
}
