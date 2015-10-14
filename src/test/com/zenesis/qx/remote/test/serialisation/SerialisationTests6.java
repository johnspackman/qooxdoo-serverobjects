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
import com.zenesis.qx.remote.test.simple.MainTests;

/**
 * These tests check things that cropped up but were not covered by other serialisation tests
 * 
 * @author "John Spackman <john.spackman@zenesis.com>"
 *
 */
public class SerialisationTests6 extends AbstractRemoteTestCase {

	public void test1() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		pw.println(tracker.toJSON(new MainTests()));
		assertFromFile(sw.toString(), "SerialisationTests6.test1");
	}

}
