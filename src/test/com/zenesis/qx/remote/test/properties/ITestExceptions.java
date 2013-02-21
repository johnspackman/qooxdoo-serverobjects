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
import com.zenesis.qx.remote.annotations.Properties;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.annotations.Remote;

/**
 * Tests exception handling on the server
 * 
 * @author "John Spackman <john.spackman@zenesis.com>"
 */
@Properties({
	@Property(value="string", sync=Remote.Sync.IMMEDIATE)
})
public interface ITestExceptions extends Proxied {

	/**
	 * Throws an exception that should be handled on the server
	 */
	public void throwException() throws DummyException;
	
	/**
	 * A property that always throws an exception when set 
	 * @return
	 */
	public String getString();
	public void setString(String value) throws DummyException;
}
