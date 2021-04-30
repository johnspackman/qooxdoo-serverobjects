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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonSerializable;
import com.zenesis.qx.remote.annotations.Mixin;
import com.zenesis.qx.remote.annotations.Use;

/**
 * ProxyType is compiled by ProxyManager from a Java class definition via
 * reflection; it contains enough information to be serialised into JSON and
 * reconstructed as a JavaScript class on the client.
 * 
 * @author John Spackman
 *
 */
public interface ProxyType extends JsonSerializable {

  /**
   * Called after construction to resolve extra types
   * 
   * @param typeManager
   */
  public void resolve(ProxyTypeManager typeManager);

  /**
   * Returns extra types required by the class
   * 
   * @return
   */
  public Set<ProxyType> getExtraTypes();

  /**
   * Detects whether this type is for an interface
   * 
   * @return
   */
  public boolean isInterface();

  /**
   * @return the name of the class
   */
  public String getClassName();

  /**
   * Returns the class
   * 
   * @return
   */
  public Class getClazz();

  /**
   * Returns the ProxyType for the classes superclass, or null if there is no
   * superclass
   * 
   * @return
   */
  public ProxyType getSuperType();

  /**
   * Returns the base Qooxdoo class; if null, then qx.core.Object is used.
   * 
   * @return
   */
  public String getQooxdooExtend();

  /**
   * Method used to serialise constructor arguments
   * 
   * @return
   */
  public Method serializeConstructorArgs();

  /**
   * @return the methods
   */
  public ProxyMethod[] getMethods();

  /**
   * @return the interfaces
   */
  public Set<ProxyType> getInterfaces();

  /**
   * Returns the mixins for the class
   * 
   * @return
   */
  public Mixin[] getMixins();

  /**
   * Returns the explicit uses for the class
   * 
   * @return
   */
  public Use[] getUses();

  /**
   * Returns the properties; returns an empty set if no properties defined, i.e.
   * never null
   * 
   * @return
   */
  public Map<String, ProxyProperty> getProperties();

  /**
   * Returns the property with a given name, or null if the property does not
   * exist
   * 
   * @param propertyName
   * @return
   */
  // public ProxyProperty getProperty(String propertyName);

  /**
   * Detects whether there is a property with a given name, in this class or
   * inherited from interfaces
   * 
   * @param name
   * @return
   */
  public boolean isProperty(String name);

  /**
   * Returns the events; returns an empty map if no properties defined
   * 
   * @return
   */
  public Map<String, ProxyEvent> getEvents();

  /**
   * Returns the ProxyEvent for a given event name, or null if the type cannot
   * raise that event
   * 
   * @param eventName
   * @return
   */
  public ProxyEvent getEvent(String eventName);

  /**
   * Detects whether the named event can be thrown by instances of this class
   * 
   * @param eventName
   * @return
   */
  public boolean supportsEvent(String eventName);

  /**
   * Creates a new instance of the type
   * 
   * @param clazz
   * @return
   */
  public Proxied newInstance(Class<? extends Proxied> clazz)
      throws InstantiationException, IllegalAccessException, InvocationTargetException;
}
