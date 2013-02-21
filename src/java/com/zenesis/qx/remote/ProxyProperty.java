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

import com.fasterxml.jackson.databind.JsonSerializable;
import com.zenesis.qx.remote.annotations.Remote;

/**
 * Represents a property that a ProxyType instance has
 * 
 * @author John Spackman [john.spackman@zenesis.com]
 */
public interface ProxyProperty extends JsonSerializable {
	
	public String getName();
	
	/**
	 * Returns true if this property is readonly
	 * @return
	 */
	public boolean isReadOnly();
	
	/**
	 * @return the sync
	 */
	public Remote.Sync getSync();

	/**
	 * @return the event
	 */
	public ProxyEvent getEvent();

	/**
	 * @return the nullable
	 */
	public boolean isNullable();

	/**
	 * @return the onDemand
	 */
	public boolean isOnDemand();

	/**
	 * @return the sendExceptions
	 */
	public boolean isSendExceptions();
	
	/**
	 * @return the propertyClass
	 */
	public MetaClass getPropertyClass();
	
	/**
	 * Returns the value currently in the property of an object 
	 * @param proxied
	 * @return
	 */
	public Object getValue(Proxied proxied);
	
	/**
	 * Sets the value of a property in an object
	 * @param proxied
	 * @param value
	 */
	public void setValue(Proxied proxied, Object value);

	/**
	 * Expires the cached value, in response to the same event on the client
	 * @param proxied
	 */
	public void expire(Proxied proxied);
	
	/**
	 * Called internally to serialize a value to the client
	 * @param proxied
	 * @param value
	 * @return
	 */
	public Object serialize(Proxied proxied, Object value);

	/**
	 * Called internally to deserialize a value from the client
	 * @param proxied
	 * @param value
	 * @return
	 */
	public Object deserialize(Proxied proxied, Object value);
}
