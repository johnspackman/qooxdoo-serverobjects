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
package com.zenesis.qx.remote;

import jakarta.servlet.ServletException;

/**
 * This exception is thrown to relay an exception to the client
 * 
 * @author "John Spackman <john.spackman@zenesis.com>"
 */
public class ProxyException extends ServletException {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private final Proxied serverObject;

  /**
   * @param message
   * @param rootCause
   */
  public ProxyException(Proxied serverObject, String message, Throwable rootCause) {
    super(message, rootCause);
    this.serverObject = serverObject;
  }

  /**
   * @param message
   */
  public ProxyException(Proxied serverObject, String message) {
    super(message);
    this.serverObject = serverObject;
  }

  /**
   * @return the serverObject
   */
  public Proxied getServerObject() {
    return serverObject;
  }

}
