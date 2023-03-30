/* ************************************************************************

   server-objects - a contrib to the Qooxdoo project (http://qooxdoo.org/)

   http://qooxdoo.org

   Copyright:
     2010-2020 Zenesis Limited, http://www.zenesis.com

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php

     This software is provided under the same licensing terms as Qooxdoo,
     please see the LICENSE file in the Qooxdoo project's top-level directory 
     for details.

   Authors:
 * John Spackman (john.spackman@zenesis.com)

 ************************************************************************ */

/**
 * IProxyIO
 *
 * IO provider for ProxyManager
 *
 */
qx.Interface.define("com.zenesis.qx.remote.IProxyIO", {
  members: {
    /**
     * @param options {Map} containing:
     *  headers {Map} of headers to send
     *  body {String} body to send
     *  async {Boolean} true if async (default: true)
     *  proxyData {Object} proxyData to return to handler
     *  handler {Function} callback
     */
    send(options) {}
  }
});
