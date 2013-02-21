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
import com.zenesis.qx.remote.annotations.Properties;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.test.AbstractRemoteTestCase;

/**
 * These tests check property serialisation
 * 
 * @author "John Spackman <john.spackman@zenesis.com>"
 *
 */
public class SerialisationTests8 extends AbstractRemoteTestCase {

	public void test1() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		pw.println(tracker.toJSON(new EightA()));
		pw.println(tracker.toJSON(new EightC()));
		assertFromFile(sw.toString(), "SerialisationTests8.test1");
	}

}

@Properties({@Property("eightB")})
interface IEightA extends Proxied {
	public IEightB[] getEightB();
}

class EightA implements IEightA {
	@Override
	public IEightB[] getEightB() {
		return null;
	}
}

interface IEightB extends Proxied {
	public void doSomething();
}

class EightB implements IEightB {
	@Override
	public void doSomething() { };
}

@Properties({@Property("eightB")})
interface IEightC extends Proxied {
	public IEightB[] getEightB();
}

class EightC implements IEightC {
	@Override
	public IEightB[] getEightB() {
		return null;
	}
}

