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
package com.zenesis.qx.remote.explorer;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.zenesis.qx.remote.ProxyManager;

public class FileExplorerServlet extends HttpServlet {
	
	// This is a singleton instance
	private static FileExplorerServlet s_instance;

	// Root for the FileExplorer to explore
	private File root;

	/**
	 * Constructor
	 */
	public FileExplorerServlet() {
		super();
		assert(s_instance == null);
		s_instance = this;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		root = new File(getServletContext().getRealPath("explorer"));
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String uri = request.getPathInfo();
		
		if ("/ajax".equals(uri)) {
			ProxyManager.handleRequest(request, response, FileExplorer.class, "fileExplorer", false);
		} else
			throw new ServletException("Unrecognised URL " + uri);
	}

	/**
	 * @return the root
	 */
	public File getRoot() {
		return root;
	}

	public static FileExplorerServlet getInstance() {
		return s_instance;
	}
}
