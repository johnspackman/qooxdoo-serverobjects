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

/**
 * Contains constants and enums shared between annotations
 * 
 * @author John Spackman [john.spackman@zenesis.com]
 */
public final class Remote {

	private Remote() {
		super();
	}
	
	/*
	 * How to handle array properties - the default is to wrap 
	 */
	public enum Array {
		WRAP,		// Wrap the array with qx.data.Array 
		NATIVE,		// Pass the array as a native array
		DEFAULT		// Sensible default for the context
	}
	
	/*
	 * Because annotation values can never be null there is no easy way to specify
	 * "take default value"; this Toggle enum is semantically equivalent to Boolean  
	 */
	public enum Toggle {
		TRUE(true), FALSE(false), DEFAULT(null);
		
		public Boolean booleanValue;

		private Toggle(Boolean booleanValue) {
			this.booleanValue = booleanValue;
		}
	}
	
	/*
	 * How to relay the event or property value to the other side
	 */
	public enum Sync {
		QUEUE("queue"), IMMEDIATE("immediate");
		
		public final String remoteId;
		Sync(String remoteId) {
			this.remoteId = remoteId;
		}
	}
}
