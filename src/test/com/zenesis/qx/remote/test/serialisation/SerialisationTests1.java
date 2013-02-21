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

import com.zenesis.qx.remote.test.AbstractRemoteTestCase;

/**
 * Tests com.zenesis.qx.remote.
 * 
 * @author John Spackman
 *
 */
public class SerialisationTests1 extends AbstractRemoteTestCase {
	
	public void test1() throws Exception {
		A a = new A();
		B b1 = new B();
		B b2 = new B();
		C c = new C();
		D d = new D();
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		// Simple test
		pw.println(tracker.toJSON(a));
		
		// b1 test inheritance
		pw.println(tracker.toJSON(b1));
		
		// b2 should send the class name and not a full definition
		pw.println(tracker.toJSON(b2));
		
		// c is an ordinary object
		pw.println(tracker.toJSON(c));
		
		// d is a composite class with multiple interface inheritance
		pw.println(tracker.toJSON(d));
		
		// Repeat it all again - only server ids should be sent because the client
		//	already has a proxy
		pw.println(tracker.toJSON(a));
		pw.println(tracker.toJSON(b1));
		pw.println(tracker.toJSON(b2));
		pw.println(tracker.toJSON(c));
		pw.println(tracker.toJSON(d));
		
		pw.flush();
		
		String actual = sw.toString();
		assertFromFile(actual, "SerialisationTests1.test1");
		
		assertEquals(a, tracker.getProxied(0));
		assertEquals(b1, tracker.getProxied(1));
		assertEquals(b2, tracker.getProxied(2));
		assertEquals(d, tracker.getProxied(3));
	}
}
