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

import com.zenesis.qx.remote.CommandId.CommandType;


/**
 * Controls the writing of events and properties to the queue to be dispatched to
 * the client.
 * 
 * When writing property values and events to the queue for asynchronous delivery,
 * the values and events may or may not need to be replayed in a specific order.  For
 * example, a simple and low-cost implementation could set all property values and then 
 * fire all events in the order they were received, amalgamating duplicate values and 
 * events into one.  A more complicated and expensive version would replay them in
 * the exact order, including duplicate values etc.
 * 
 * @author John Spackman [john.spackman@zenesis.com]
 */
public interface CommandQueue {

	public void queueCommand(CommandId.CommandType type, Object object, String propertyName, Object data);
	public void queueCommand(CommandId id, Object data);

	public Object getCommand(CommandType type, Object object, String propertyName);
	
	/**
	 * Detects whether there is anything to be sent to the client
	 * @return
	 */
	public boolean hasDataToFlush();
	
	/**
	 * Detects whether any property or event should be flushed ASAP
	 * @return
	 */
	public boolean needsFlush();
}
