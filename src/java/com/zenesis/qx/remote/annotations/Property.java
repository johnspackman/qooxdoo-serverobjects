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
package com.zenesis.qx.remote.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a single property; if an non-empty eventName is given then an on-change
 * event is thrown with that name, where the event is deferred and asynchronous.
 * To customise the event, @see <code>PropertyEx</code> and <code>Event</code>.
 * 
 * For any given property use either <code>Property</code> or <code>PropertyEx</code>, 
 * not both.
 * 
 * @author John Spackman [john.spackman@zenesis.com]
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.METHOD })
public @interface Property {
	
	/**
	 * The name of the property
	 */
	public String value() default "";
	
	/**
	 * For array properties, these can be created on the client as native arrays or
	 * as qx.data.Array (which is required for binding).  The default is to wrap.  
	 * @return
	 */
	public Remote.Array array() default Remote.Array.DEFAULT;
	
	/**
	 * If a property array is an ArrayList instead of an actual array then we loose
	 * type information and that means we cannot deserialise from the client; in
	 * this case, we have to specify the class using the arrayType 
	 * @return
	 */
	public Class arrayType() default Object.class;
	
	/**
	 * Synchronisation policy for the property value
	 */
	public Remote.Sync sync() default Remote.Sync.QUEUE;
	
	/**
	 * Whether to only get the value on demand (if true, this means that Qooxdoo's 
	 * native property definition mechanism will not be used because it does not 
	 * support overriding the get method)
	 * @return
	 */
	public boolean onDemand() default false;
	
	/**
	 * Whether exceptions thrown by this property's accessors should be passed back 
	 * to the client
	 * @return
	 */
	public Remote.Toggle exceptions() default Remote.Toggle.DEFAULT;

	/**
	 * Whether this property is read-only (default is to auto detect)
	 * @return
	 */
	public Remote.Toggle readOnly() default Remote.Toggle.DEFAULT;

	/**
	 * The name of the event to be fired when the property is modified
	 */
	public String event() default "";
	
	/**
	 * Whether the property supports null values
	 * @return
	 */
	public Remote.Toggle nullable() default Remote.Toggle.DEFAULT;
	
	/**
	 * The name of the method to use to serialize the property value for
	 * sending to the server
	 * @return
	 */
	public String serialize() default "";
	
	/**
	 * The name of the method to call to deserialize the property value when 
	 * received from the client 
	 * @return
	 */
	public String deserialize() default "";
	
	/**
	 * The name of the method to call to expire the cached property value when 
	 * received from the client 
	 * @return
	 */
	public String expire() default "";
	
	/***
	 * The name of the method to call to get the value of the property
	 * @return
	 */
	public String get() default "";
	
	/**
	 * The name of the method to call to set the value of the property
	 * @return
	 */
	public String set() default "";
}
