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

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonSerializable;

/**
 * ProxyType is compiled by ProxyManager from a Java class definition via reflection;
 * it contains enough information to be serialised into JSON and reconstructed as
 * a JavaScript class on the client.
 * 
 * @author John Spackman
 *
 */
public interface ProxyType extends JsonSerializable {
	
	/**
	 * Detects whether this type is for an interface
	 * @return
	 */
	public boolean isInterface();
	
	/**
	 * @return the name of the class
	 */
	public String getClassName();
	
	/**
	 * Returns the ProxyType for the classes superclass, or null if there is no superclass 
	 * @return
	 */
	public ProxyType getSuperType();

	/**
	 * @return the methods
	 */
	public ProxyMethod[] getMethods();

	/**
	 * @return the interfaces
	 */
	public Set<ProxyType> getInterfaces();
	
	/**
	 * Returns the properties; returns an empty set if no properties defined, i.e. never null
	 * @return
	 */
	public Map<String, ProxyProperty> getProperties();
	
	/**
	 * Returns the property with a given name, or null if the property does not exist
	 * @param propertyName
	 * @return
	 */
	//public ProxyProperty getProperty(String propertyName);

	/**
	 * Detects whether there is a property with a given name, in this class or
	 * inherited from interfaces
	 * @param name
	 * @return
	 */
	public boolean isProperty(String name);
	
	/**
	 * Returns the events; returns an empty map if no properties defined
	 * @return
	 */
	public Map<String, ProxyEvent> getEvents();
	
	/**
	 * Returns the ProxyEvent for a given event name, or null if the type
	 * cannot raise that event
	 * @param eventName
	 * @return
	 */
	public ProxyEvent getEvent(String eventName);
	
	/**
	 * Detects whether the named event can be thrown by instances of this class
	 * @param eventName
	 * @return
	 */
	public boolean supportsEvent(String eventName);
	
}
