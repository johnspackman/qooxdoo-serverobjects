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

import com.zenesis.qx.remote.annotations.Method;

public class TestScalars implements ITestScalars {

	@Override
	public int getFourtyThree() {
		return 43;
	}

	@Override
	public String getHelloWorld() {
		return "Hello World";
	}

	@Override
	public float getSixPointSeven() {
		return 6.7f;
	}
	
	@Method
	public int getZero() {
		return 0;
	}
	
	@Method
	public boolean getTrue() {
		return true;
	}
	
	@Method
	public boolean getFalse() {
		return false;
	}

	@Override
	public String[] getNames() {
		return new String[] { "Jack", "Jill", "Bill", "Ben" };
	}

	/* (non-Javadoc)
	 * @see com.zenesis.qx.remote.test.simple.ITestScalars#addUp(int[])
	 */
	@Override
	public int addUp(int[] values) {
		int result = 0;
		for (int i : values)
			result += i;
		return result;
	}

}
