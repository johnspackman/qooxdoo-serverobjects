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
package com.zenesis.qx.event;

/**
 * Contains details of the event which was thrown
 * @author John Spackman [john.spackman@zenesis.com]
 */
public class Event {

	private final String eventName;
	private final Object originalTarget;
	private final Object currentTarget;
	private final Object data;
	
	/**
	 * @param eventName
	 * @param originalTarget
	 * @param currentTarget
	 * @param data
	 */
	public Event(Object originalTarget, Object currentTarget, String eventName, Object data) {
		super();
		this.eventName = eventName;
		this.originalTarget = originalTarget;
		this.currentTarget = currentTarget;
		this.data = data;
	}

	/**
	 * @return the eventName
	 */
	public String getEventName() {
		return eventName;
	}

	/**
	 * @return the originalTarget
	 */
	public Object getOriginalTarget() {
		return originalTarget;
	}

	/**
	 * @return the currentTarget
	 */
	public Object getCurrentTarget() {
		return currentTarget;
	}

	/**
	 * @return the data
	 */
	public Object getData() {
		return data;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return eventName + " on " + currentTarget + ", data=" + data;
	}
}
