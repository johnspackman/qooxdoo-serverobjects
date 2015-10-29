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
import com.zenesis.qx.remote.annotations.Method;

/**
 * Basic tests for scalar values
 * @author "John Spackman <john.spackman@zenesis.com>"
 *
 */
public interface ITestScalars extends Proxied {

	/**
	 * Returns 43
	 * @return
	 */
	@Method
	public int getFourtyThree();
	
	/**
	 * Returns 6.7
	 * @return
	 */
	@Method
	public float getSixPointSeven();
	
	/**
	 * Returns "Hello World"
	 * @return
	 */
	@Method
	public String getHelloWorld(); 
	
	/**
	 * Returns [ "Jack", "Jill", "Bill", "Ben" ]
	 * @return
	 */
	@Method
	public String[] getNames();
	
	/**
	 * Returns the sum of the values
	 * @param values
	 * @return
	 */
	@Method
	public int addUp(int[] values);
}
