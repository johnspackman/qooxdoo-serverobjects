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

import com.zenesis.qx.remote.annotations.Event;
import com.zenesis.qx.remote.annotations.Remote;
import com.zenesis.qx.remote.annotations.Remote.Sync;

/**
 * Represents an event which can be raised against a ProxyType instance
 * 
 * @author John Spackman [john.spackman@zenesis.com]
 */
public class ProxyEvent {

  private final String name;
  private final Remote.Sync sync;
  private final Class dataClass;

  /**
   * @param name
   * @param sync
   * @param dataClass
   */
  public ProxyEvent(String name, Sync sync, Class dataClass) {
    super();
    this.name = name;
    this.sync = sync;
    this.dataClass = dataClass;
  }

  /**
   * @param name
   */
  public ProxyEvent(String name) {
    this(name, Remote.Sync.QUEUE, null);
  }

  /**
   * Creates a ProxyEvent from an Event annotation
   * 
   * @param anno
   */
  public ProxyEvent(Event anno) {
    this(anno.value(), anno.sync(), anno.data());
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the sync
   */
  public Remote.Sync getSync() {
    return sync;
  }

  /**
   * @return the dataClass
   */
  public Class getDataClass() {
    return dataClass;
  }

}
