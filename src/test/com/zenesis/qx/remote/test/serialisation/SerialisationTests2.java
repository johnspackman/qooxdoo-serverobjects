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
import com.zenesis.qx.remote.annotations.AlwaysProxy;
import com.zenesis.qx.remote.annotations.DoNotProxy;
import com.zenesis.qx.remote.test.AbstractRemoteTestCase;

/**
 * These tests check whether methods get exported correctly according to where they are defined
 * and what annotations are used.
 * @author "John Spackman <john.spackman@zenesis.com>"
 *
 */
public class SerialisationTests2 extends AbstractRemoteTestCase {

	public void test1() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		pw.println(tracker.toJSON(new TestDoNotProxy()));
		
		assertFromFile(sw.toString(), "SerialisationTests2.test1");
	}

	public void test2() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		pw.println(tracker.toJSON(new TestTwoDeep()));
		
		assertFromFile(sw.toString(), "SerialisationTests2.test2");
	}

	public void test3() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		pw.println(tracker.toJSON(new TestAlwaysProxy()));
		
		assertFromFile(sw.toString(), "SerialisationTests2.test3");
	}
}

/*
 * Test that only methods defined in an interface which is extended from Proxied 
 * are proxied, and also that the DoNotProxy annotation is respected
 */
interface ITestDoNotProxy extends Proxied {
	public boolean exportedMethod();
	
	@DoNotProxy
	public boolean suppressedMethod();
}

class TestDoNotProxy implements ITestDoNotProxy {
	@Override
	public boolean exportedMethod() {
		return false;
	}

	@Override
	public boolean suppressedMethod() {
		return false;
	}
	
	public boolean invisibleMethod() {
		return false;
	}
}

/*
 * Test that methods defined in interfaces not directly extended from Proxied are
 * also exported
 */
interface ITestTwoDeep extends ITestDoNotProxy {
	public boolean exportedByInheritanceMethod();
}

class TestTwoDeep extends TestDoNotProxy implements ITestTwoDeep {

	@Override
	public boolean exportedByInheritanceMethod() {
		return false;
	}
}

/*
 * Test that methods in classes marked AlwaysProxy are proxied even though they
 * normally would not be
 */
class TestAlwaysProxy implements ITestDoNotProxy {
	@Override
	public boolean exportedMethod() {
		return false;
	}

	@Override
	public boolean suppressedMethod() {
		return false;
	}
	
	@AlwaysProxy
	public boolean additionalMethod() {
		return false;
	}
}
