/**
 * ************************************************************************
 * 
 *    server-objects - a contrib to the Qooxdoo project that makes server 
 *    and client objects operate seamlessly; like Qooxdoo, server objects 
 *    have properties, events, and methods all of which can be access from
 *    either server or client, regardless of where the original object was
 *    created.
 * 
 *    http://qooxdoo.org
 * 
 *    Copyright:
 *      2010 Zenesis Limited, http://www.zenesis.com
 * 
 *    License:
 *      LGPL: http://www.gnu.org/licenses/lgpl.html
 *      EPL: http://www.eclipse.org/org/documents/epl-v10.php
 *      
 *      This software is provided under the same licensing terms as Qooxdoo,
 *      please see the LICENSE file in the Qooxdoo project's top-level directory 
 *      for details.
 * 
 *    Authors:
 *      * John Spackman (john.spackman@zenesis.com)
 * 
 * ************************************************************************
 */
package com.zenesis.qx.remote;

import com.zenesis.qx.event.EventManager;

/**
 * Specialisation of EventManager for better event detection
 * 
 * @author John Spackman [john.spackman@zenesis.com]
 */
public class ProxyEventManager extends EventManager {

  private final ProxyTypeManager typeManager;

  public ProxyEventManager(ProxyTypeManager typeManager) {
    super();
    this.typeManager = typeManager;
  }

  public ProxyEventManager(ProxyTypeManager typeManager, boolean setGlobal) {
    super(setGlobal);
    this.typeManager = typeManager;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.zenesis.qx.event.EventManager#supportsEvent(java.lang.Object,
   * java.lang.String)
   */
  @Override
  public boolean _supportsEvent(Object obj, String eventName) {
    if (obj instanceof Proxied) {
      Proxied proxied = (Proxied) obj;
      ProxyType type = typeManager.getProxyType(proxied.getClass());
      return type.supportsEvent(eventName);
    }
    return super._supportsEvent(obj, eventName);
  }

}
