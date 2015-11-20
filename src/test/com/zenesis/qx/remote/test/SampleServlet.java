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

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.ProxyManager;
import com.zenesis.qx.remote.ProxySessionTracker;
import com.zenesis.qx.remote.RequestHandler;
import com.zenesis.qx.remote.test.simple.MainTests;
import com.zenesis.qx.remote.test.simple.TestBootstrap;

@SuppressWarnings("serial")
public class SampleServlet extends HttpServlet {
	
	@Override
	public void init() throws ServletException {
		super.init();
		String str = getServletConfig().getInitParameter("logging-dir");
		if (str != null) {
			File dir = new File(str);
			dir.mkdirs();
			RequestHandler.s_traceLogDir = dir;
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String uri = request.getPathInfo();
		
		if ("/ajax".equals(uri)) {
			ProxyManager.handleRequest(request, response, TestBootstrap.class, "sampleServlet", true);
		} else
			throw new ServletException("Unrecognised URL " + uri);
	}
	
}
