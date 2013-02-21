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
package com.zenesis.qx.remote.test.simple;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.annotations.Properties;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.test.properties.ITestArrays;
import com.zenesis.qx.remote.test.properties.ITestExceptions;
import com.zenesis.qx.remote.test.properties.ITestProperties;
import com.zenesis.qx.remote.test.properties.TestProperties;

/**
 * This is the Bootstrap - it is passed to the client during initialisation and is the entry
 * point for the client application.  In our case, this is the entry point for all of the
 * tests
 * 
 * @author "John Spackman <john.spackman@zenesis.com>"
 */
@Properties({
	@Property("testProperties"),
	@Property("clientTestProperties")
})
public interface ITestBootstrap extends Proxied {

	/**
	 * Returns the test for scalar values; this always returns the same instance so
	 * that verifyTestScalars can compare values 
	 * @return
	 */
	public ITestScalars getTestScalars();
	
	/**
	 * Tests that testScalars is the same one returned previously by getTestScalars()
	 * @param testScalars
	 * @return
	 */
	public boolean verifyTestScalars(ITestScalars testScalars);
	
	/**
	 * Tests the "cache" attribute of the properties
	 * @return
	 */
	public ITestProperties getTestProperties();
	
	/**
	 * Property to be set with an instance of TestProperties created on the client
	 * @return
	 */
	public TestProperties getClientTestProperties();
	public void setClientTestProperties(TestProperties props);
	
	/**
	 * Checks that the clientTestProperties property is not the one created by this class
	 * and is not null (i.e. it was created on the client) and that the "watchedString" 
	 * property has had it's value set to "setByClientProperty"
	 * @return
	 */
	public boolean checkClientTestProperties();
	
	/**
	 * Checks that the ITestProperties is _not_ the one created by this class, i.e.
	 * that it was created on the client and that the "watchedString" property has
	 * had it's value set to "setByClientMethod" 
	 * @param props
	 * @return
	 */
	public boolean checkNewTestProperties(ITestProperties props);
	
	/**
	 * Tests exception handling
	 * @return
	 */
	public ITestExceptions getTestExceptions();
	
	/**
	 * Tests array handling
	 * @return
	 */
	public ITestArrays getTestArrays();
}
