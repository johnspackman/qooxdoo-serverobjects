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
package com.zenesis.qx.remote.test.serialisation;

public class B extends A implements IB {

	
	/* (non-Javadoc)
	 * @see com.zenesis.test.junit.remote.A#getValueA(int)
	 */
	@Override
	public String getValueA(int value) {
		return "B:valueA=" + super.getValueA(value);
	}

	/* (non-Javadoc)
	 * @see com.zenesis.test.junit.remote.IB#getValueB(int)
	 */
	@Override
	public String getValueB(int value, A a) {
		return "B:valueB=" + value + ", a.valueA=" + a.getValueA(value);
	}

}
