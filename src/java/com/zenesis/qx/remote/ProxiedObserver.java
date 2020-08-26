package com.zenesis.qx.remote;

public interface ProxiedObserver {

    public enum ObservationType {
        SET, EDIT_ARRAY
    }
    public void observeSetProperty(Proxied proxied, ProxyProperty property, Object value, Object oldValue);
    public void observeEditArray(Proxied proxied, ProxyProperty property, Object value);
}
