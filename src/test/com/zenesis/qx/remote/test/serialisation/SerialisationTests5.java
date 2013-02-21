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
import com.zenesis.qx.remote.ProxyTypeManager;
import com.zenesis.qx.remote.annotations.AlwaysProxy;
import com.zenesis.qx.remote.annotations.ExplicitProxyOnly;
import com.zenesis.qx.remote.test.AbstractRemoteTestCase;

/**
 * These tests check that manually marking classes for export work as expected
 * 
 * @author "John Spackman <john.spackman@zenesis.com>"
 *
 */
public class SerialisationTests5 extends AbstractRemoteTestCase {

	public void test1() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		ProxyTypeManager.INSTANCE.getProxyType(TestManualExport1.class);
		pw.println(tracker.toJSON(new TestManualExport1()));
		assertFromFile(sw.toString(), "SerialisationTests5.test1");
	}

	public void test2() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		ProxyTypeManager.INSTANCE.getProxyType(TestManualExport2.class);
		pw.println(tracker.toJSON(new TestManualExport2()));
		assertFromFile(sw.toString(), "SerialisationTests5.test2");
	}

}

/*
 * Tests that classes can be manually exported
 */
@ExplicitProxyOnly
class TestManualExport1 implements Proxied {

	@AlwaysProxy
	public boolean manualExportMethod() {
		return false;
	}

	public boolean ignoredMethod() {
		return false;
	}
}

/*
 * Tests that classes can be manually exported
 */
class TestManualExport2 implements Proxied {

	public boolean manualExportMethod() {
		return false;
	}

	public boolean anotherMethod() {
		return false;
	}
}

