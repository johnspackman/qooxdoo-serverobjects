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
public class SerialisationTests9 extends AbstractRemoteTestCase {
	
	public void test1() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		F f = new F();
		G g = new G();
		
		// Class implements Proxied
		pw.println(tracker.toJSON(f));
		
		// Class which extends a class that implements Proxied
		pw.println(tracker.toJSON(g));
		
		pw.flush();
		
		String actual = sw.toString();
		assertFromFile(actual, "SerialisationTests9.test1");
		
		assertEquals(f, tracker.getProxied(0));
	}
}
