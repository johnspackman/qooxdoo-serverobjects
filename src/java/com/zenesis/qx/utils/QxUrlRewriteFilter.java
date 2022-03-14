/* ************************************************************************
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
 *
 */
package com.zenesis.qx.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rewrites incoming URLs on the fly to speed up development.
 * 
 * When developing in Eclipse, the IDE watches the changes you make and then
 * publishes them to Tomcat. This is fine (and essential) for normal Java
 * development but if you're editing JS files you have to wait for the IDE to
 * see you finish editing and then publish the changes. If you refresh your
 * browser too fast, it won't see your changes and you get to spend time
 * debugging the problem that you just fixed.
 * 
 * Also, Qooxdoo has lots and lots of files and if you drop the release into
 * your WebContent folder then Eclipse will publish it to Tomcat - but this
 * takes *ages*, and if you ever have to refresh etc then you can go have a nap,
 * make love to your wife, and still have time to make a coffee before it's
 * done. During development, put Qooxdoo outside the WebContent folder and use
 * this filter to map it into the URL-space.
 * 
 * This filter takes the incoming URL and if it matches a given path(s) it will
 * get the file directly from your development directory and not from the Tomcat
 * webapp directory.
 * 
 * @author "John Spackman <john.spackman@zenesis.com>"
 *
 */
public class QxUrlRewriteFilter implements Filter {

  private ServletContext context;
  private final HashMap<String, File> rewrites = new HashMap<String, File>();

  @Override
  public void init(FilterConfig config) throws ServletException {
    context = config.getServletContext();
    File root = new File(context.getRealPath("."));
    String str = config.getInitParameter("rewrite-root");
    if (str != null)
      root = new File(str).getAbsoluteFile();

    for (Enumeration<String> e = config.getInitParameterNames(); e.hasMoreElements();) {
      String name = e.nextElement();
      if (name.startsWith("rewrite-") && !name.equals("rewrite-root")) {
        String path = config.getInitParameter(name);
        int pos = path.indexOf('=');
        if (pos > 0) {
          String mapTo = path.substring(pos + 1);
          path = path.substring(0, pos);
          File file;
          if (mapTo.length() == 0 || mapTo.charAt(0) != '/')
            file = new File(root, mapTo);
          else
            file = new File(mapTo);
          rewrites.put(path, file);
        }
      }
    }
  }

  @Override
  public void destroy() {
  }

  @Override
  public void doFilter(ServletRequest _request, ServletResponse _response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) _request;
    HttpServletResponse response = (HttpServletResponse) _response;

    // Look for files to process from here
    String uri = request.getRequestURI().substring(request.getContextPath().length());
    for (String path : rewrites.keySet()) {
      if (uri.startsWith(path)) {
        // It's got to match a path (i.e. not part of a path)
        if (path.length() == uri.length() || uri.charAt(path.length()) != '/')
          continue;

        // Find the file
        File file = new File(rewrites.get(path), uri.substring(path.length()));
        if (!file.exists())
          continue;

        // Check timestamps
        response.setDateHeader("Last-Modified", file.lastModified());
        long lastMod = request.getDateHeader("If-Modified-Since");
        if (lastMod > 0 && Math.abs(file.lastModified() - lastMod) < 1000) {
          response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
          return;
        }

        // Send the file
        String mimeType = context.getMimeType(file.getName());
        response.setContentType(mimeType);
        response.setContentLength((int) file.length());

        // Output
        OutputStream os = response.getOutputStream();
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[8 * 1024];
        int len;
        while ((len = fis.read(buffer)) > -1)
          os.write(buffer, 0, len);
        os.flush();
        os.close();
        fis.close();

        // Done
        return;
      }
    }

    chain.doFilter(request, response);
    return;
  }

}
