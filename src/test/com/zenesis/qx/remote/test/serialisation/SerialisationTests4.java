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

import java.io.PrintWriter;
import java.io.StringWriter;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.annotations.DoNotProxy;
import com.zenesis.qx.remote.test.AbstractRemoteTestCase;

/**
 * These tests check whether exceptions are thrown to trap various errors
 * 
 * @author "John Spackman <john.spackman@zenesis.com>"
 *
 */
public class SerialisationTests4 extends AbstractRemoteTestCase {

	public void test1() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		try {
			pw.println(tracker.toJSON(new TestMethodConflict()));
			assertTrue("Exception expected for conflicting methods", false);
		}catch(IllegalArgumentException e) {
			if (!e.getMessage().startsWith("org.codehaus.jackson.map.JsonMappingException: Cannot create a proxy for class") ||
					!e.getMessage().contains("because it has overloaded method"))
			assertTrue(false);
		}
	}

	public void test2() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		try {
			pw.println(tracker.toJSON(new TestDoNotProxyConflict()));
			assertTrue("Exception expected for DoNotProxy conflicting with proxy from another interface", false);
		}catch(IllegalArgumentException e) {
			if (!e.getMessage().startsWith("org.codehaus.jackson.map.JsonMappingException: Cannot create a proxy for class") ||
					!e.getMessage().contains("because it has conflicting DoNotProxy"))
			assertTrue(false);
		}
	}

}

/*
 * Tests that overloaded methods from different interfaces throw an exception
 */
interface ITestMethodConflict1 extends Proxied {
	public boolean conflictingMethod(String str);
}

interface ITestMethodConflict2 extends Proxied {
	public boolean conflictingMethod(int i);
}

class TestMethodConflict implements ITestMethodConflict1, ITestMethodConflict2 {
	@Override
	public boolean conflictingMethod(int arg0) {
		return false;
	}

	@Override
	public boolean conflictingMethod(String str) {
		return false;
	}
}

/*
 * Tests that methods which are proxied from one interface and marked DoNotProxy
 * from another raise an error
 */
interface ITestDoNotProxyConflict1 extends Proxied {
	public boolean identicalMethod();
}

interface ITestDoNotProxyConflict2 extends Proxied {
	@DoNotProxy
	public boolean identicalMethod();
}

class TestDoNotProxyConflict implements ITestDoNotProxyConflict1, ITestDoNotProxyConflict2 {

	@Override
	public boolean identicalMethod() {
		return false;
	}
	
}

