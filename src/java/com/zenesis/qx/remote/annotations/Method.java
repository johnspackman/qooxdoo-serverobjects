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
 * Allows for decoration of remotely invoked methods
 * 
 * @author "John Spackman <john.spackman@zenesis.com>"
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Method {

	/**
	 * If the function returns an array, whether the array should be wrapped on 
	 * the client as a qx.data.Array
	 * @return
	 */
	public Remote.Array array() default Remote.Array.DEFAULT;
	
	/**
	 * If a function returns an ArrayList instead of an actual array then we loose
	 * type information and that means we cannot deserialise from the client; in
	 * this case, we have to specify the class using the arrayType 
	 * @return
	 */
	public Class arrayType() default Object.class;
	
	/**
	 * If a function returns a Map this specifies the class of the key; in practice
	 * this is only useful for enum keys because keys must be translated into
	 * strings on the client 
	 * @return
	 */
	public Class keyType() default Object.class;
	
	/**
	 * Whether the method's return value should be sent with the object property
	 * values
	 * @return
	 */
	public boolean prefetchResult() default false;
	
	/**
	 * Whether the method's return value can be cached on the client
	 * @return
	 */
	public boolean cacheResult() default false;
}
