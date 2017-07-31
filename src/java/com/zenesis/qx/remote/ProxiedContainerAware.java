package com.zenesis.qx.remote;

/**
 * Classes which want to know what Proxied parent instance and property they are attached to
 * can implement this interface.  setProxiedContainer is called during serialisation
 *  
 * @author john
 *
 */
public interface ProxiedContainerAware {

    public void setProxiedContainer(Proxied container, ProxyProperty property);
}
