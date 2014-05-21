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

import com.zenesis.qx.event.EventManager;
import com.zenesis.qx.remote.ProxyManager;
import com.zenesis.qx.remote.annotations.Property;

public class TestProperties implements ITestProperties {
	
	private String immediate = "Server Immediate";
	private String queued = "Server Queued";
	private String onDemandString = "MyOnDemandString";
	private String changeLog = "";
	private String watchedString;
	private int triggers = 0;
	
	@Property(onDemand=true)
	private String onDemandPreload = "MyOnDemandPreload";
	
	@Override
	public String getImmediate() {
		return immediate;
	}
	
	@Override
	public String getQueued() {
		return queued;
	}
	
	@Override
	public void setImmediate(String newValue) {
		this.immediate = ProxyManager.changeProperty(this, "immediate", newValue, immediate);
		changeLog += "immediate=" + immediate + "; ";
	}
	
	@Override
	public void setQueued(String value) {
		this.queued = ProxyManager.changeProperty(this, "queued", value, queued);
		changeLog += "queued=" + value + "; ";
	}

	@Override
	public String getOnDemandString() {
		return onDemandString;
	}
	
	@Override
	public void setOnDemandString(String onDemandString) {
		this.onDemandString = ProxyManager.changeProperty(this, "onDemandString", onDemandString, onDemandString);
	}

	/**
	 * @return the onDemandPreload
	 */
	public String getOnDemandPreload() {
		return onDemandPreload;
	}

	/**
	 * @param onDemandPreload the onDemandPreload to set
	 */
	public void setOnDemandPreload(String onDemandPreload) {
		this.onDemandPreload = ProxyManager.changeProperty(this, "onDemandPreload", onDemandPreload, onDemandPreload);
	}

	@Override
	public String getReadOnlyString() {
		return "read-only";
	}

	@Override
	public String getChangeLog() {
		return changeLog;
	}

	@Override
	public String getWatchedString() {
		return watchedString;
	}

	@Override
	public void setWatchedString(String value) {
		this.watchedString = ProxyManager.changeProperty(this, "watchedString", value, watchedString);
		changeLog += "watchedString=" + value + "; ";
	}

	@Override
	public void triggerChangeWatchedString() {
		setWatchedString("Watched=" + (++triggers));
	}

	@Override
	public void triggerSomeEvent() {
		EventManager.fireEvent(this, "someEvent");
	}
	
}
