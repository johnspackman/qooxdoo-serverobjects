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
package com.zenesis.qx.remote.test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.servlet.ServletException;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.ProxyManager;
import com.zenesis.qx.remote.ProxySessionTracker;
import com.zenesis.qx.remote.RequestHandler;
import com.zenesis.qx.remote.test.simple.MainTests;
import com.zenesis.qx.test.AbstractTestCase;

public abstract class AbstractRemoteTestCase extends AbstractTestCase {

	protected Class<? extends Proxied> bootstrapClass = MainTests.class;
	protected ProxySessionTracker tracker;
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tracker = new ProxySessionTracker(bootstrapClass);
		ProxyManager.selectTracker(tracker);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		ProxyManager.deselectTracker(tracker);
		tracker = null;
		super.tearDown();
	}

	/**
	 * Posts a request to the tracker and returns the result as a string
	 */
	protected String postRequest(Reader reader) throws IOException, ServletException {
		StringWriter sw = new StringWriter();
		new RequestHandler(tracker).processRequest(reader, sw);
		return sw.toString();
	}
	
	/**
	 * Posts a request and returns the output as a string
	 * @param src
	 * @return
	 */
	protected String postRequest(String src) throws IOException, ServletException {
		return postRequest(new StringReader(src));
	}
}
