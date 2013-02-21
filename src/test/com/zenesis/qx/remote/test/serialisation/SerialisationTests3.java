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
import com.zenesis.qx.remote.test.AbstractRemoteTestCase;

/**
 * These tests check whether methods parameters are exported correctly
 * @author "John Spackman <john.spackman@zenesis.com>"
 *
 */
public class SerialisationTests3 extends AbstractRemoteTestCase {

	public void test1() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		pw.println(tracker.toJSON(new TestScalars()));
		
		assertFromFile(sw.toString(), "SerialisationTests3.test1");
	}

	public void test2() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		pw.println(tracker.toJSON(new TestComplexAlpha()));
		
		assertFromFile(sw.toString(), "SerialisationTests3.test2");
	}
}

/*
 * Test for basic types
 */
interface ITestScalars extends Proxied {
	
	public boolean getBoolean(boolean value);
	public float getFloat(float value);
	public double getDouble(double value);
	public int getInt(int value);
	public String getString(String value);
	public String[] getStringArray(String[] value);
}

class TestScalars implements ITestScalars {
	@Override
	public boolean getBoolean(boolean value) {
		return false;
	}

	@Override
	public double getDouble(double value) {
		return 0;
	}

	@Override
	public float getFloat(float value) {
		return 0;
	}

	@Override
	public int getInt(int value) {
		return 0;
	}

	@Override
	public String getString(String value) {
		return null;
	}
	
	@Override
	public String[] getStringArray(String[] value) {
		return new String[] { "hello", "world" };
	}
}

/*
 * Test for complex (i.e. Proxied) types, where classes are interdependent and/or
 * self referencing
 */

interface ITestComplexAlpha extends Proxied {
	
	public ITestComplexBravo getBravo();
	
	public ITestComplexAlpha getRecursive();
	
	public void setBravo(ITestComplexBravo bravo);
}

interface ITestComplexBravo extends Proxied {
	
	public void setAlpha(ITestComplexAlpha alpha);
}

class TestComplexAlpha implements ITestComplexAlpha {

	@Override
	public ITestComplexBravo getBravo() {
		return null;
	}

	@Override
	public ITestComplexAlpha getRecursive() {
		return null;
	}

	@Override
	public void setBravo(ITestComplexBravo bravo) {
	}
}

class TestComplexBravo implements ITestComplexBravo {

	@Override
	public void setAlpha(ITestComplexAlpha alpha) {
	}
	
}
