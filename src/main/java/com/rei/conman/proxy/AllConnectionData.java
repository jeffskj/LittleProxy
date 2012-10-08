package com.rei.conman.proxy;

import javax.management.MXBean;

@MXBean(true)
public interface AllConnectionData {

    int getNumRequestHandlers();
}
