package com.zenesis.qx.remote;

/**
 * Proxied objects which implement this interface are able to provide their own
 * ProxyType instance.
 * 
 * @author john
 */
public interface DynamicTypeProvider extends Proxied {

  /**
   * Returns the ProxyType to use for this object
   * 
   * @return the PrpoxyType - if null, then default behaviour is used to create a
   *         ProxyType
   */
  public ProxyType getProxyType();
}
