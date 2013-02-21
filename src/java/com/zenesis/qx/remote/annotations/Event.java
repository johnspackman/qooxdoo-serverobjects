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
 * Defines an event that can be thrown, either by a property or by an object
 * 
 * @author John Spackman [john.spackman@zenesis.com]
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface Event {
	
	/**
	 * The name of the event
	 */
	public String value();
	
	/**
	 * Synchronisation policy for the event
	 */
	public Remote.Sync sync() default Remote.Sync.QUEUE;

	/**
	 * If the event has data associated with it, this gives the class of the
	 * data.  Because annotation properties cannot have null and must be literal
	 * values, the default class is <code>Remote</code> which cannot be instantiated
	 * and therefore is a suitable placeholder for "no data".
	 */
	public Class data() default Remote.class;
}
