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
package com.zenesis.qx.remote.test.properties;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.annotations.Event;
import com.zenesis.qx.remote.annotations.Events;
import com.zenesis.qx.remote.annotations.Method;
import com.zenesis.qx.remote.annotations.Properties;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.annotations.Remote;

/**
 * Tests property configurations:
 * 
 * 1. Test setting "sync" to immediate and queued
 * 2. Test read-only properties
 * 3. Test writable properties
 * 4. Test property change events
 * 5. Test normal events (i.e. events not associated with a property)
 * 6. Test client instantiation (this class can be instantiated on the client)
 * 
 * @author "John Spackman <john.spackman@zenesis.com>"
 */
@Properties({
	@Property(value="immediate", sync=Remote.Sync.IMMEDIATE),
	@Property(value="queued", sync=Remote.Sync.QUEUE),
	@Property(value="onDemandString", onDemand=true, event="changeDemandString"),
	@Property("readOnlyString"),
	@Property(value="watchedString", event="changeWatchedString")
	})
@Events({
	@Event("someEvent")
	})
public interface ITestProperties extends Proxied {

	/**
	 * immediate is a property whose property changes are immediately flushed to the server
	 * @return
	 */
	public String getImmediate();
	public void setImmediate(String value);
	
	/**
	 * queued is a property whose property changes are queued to go to the server
	 * @return
	 */
	public String getQueued();
	public void setQueued(String value);
	
	/**
	 * onDemandString is a string which is not sent with the object initially, but is
	 * got on demand and then subsequently cached
	 * @return
	 */
	public String getOnDemandString();
	public void setOnDemandString(String value);

	/**
	 * Not editable
	 * @return
	 */
	public String getReadOnlyString();
	
	/**
	 * Log of changes to properties
	 * @return
	 */
	@Method
	public String getChangeLog();
	
	/**
	 * watchedString is modified by triggerChangeWatchedString
	 * @return
	 */
	public String getWatchedString();
	public void setWatchedString(String value);
	public void triggerChangeWatchedString();
	
	/**
	 * Triggers an event not connected with a property
	 */
	@Method
	public void triggerSomeEvent();
}

