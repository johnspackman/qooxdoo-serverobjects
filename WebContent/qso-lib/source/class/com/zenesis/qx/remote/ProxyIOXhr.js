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
 * ProxyIOXhr
 * 
 * Implementation of IProxyIO for XHR
 * 
 */
qx.Class.define("com.zenesis.qx.remote.ProxyIOXhr", {
  extend: qx.core.Object,
  
  implement: [ com.zenesis.qx.remote.IProxyIO ],
  
  construct(url) {
    this.base(arguments);
    this.setUrl(url);
  },
  
  properties: {
    /** URL to communicate with */
    url: {
      check: "String"
    },
    
    /** Timeout to allow on XHR */
    timeout: {
      init: null,
      nullable: true,
      check: "Integer",
      apply: "_applyTimeout"
    }
  },
  
  members: {
    /**
     * Called to apply the timeout
     */
    _applyTimeout: function(value) {
      if (typeof qx.io.remote.transport.XmlHttp.setTimeout == "function") {
        qx.io.remote.transport.XmlHttp.setTimeout(value);
      } else {
        this.warn("Cannot set the timeout of the underlying qx.io.remote.transport.XmlHttp transport because it does not support " +
        "setTimeout.  Please see pull request #???");
      }
    },

    /**
     * @Override
     */
    send(options) {
      let { headers, body, async, proxyData, handler } = options;
      headers = qx.lang.Object.clone(headers);
      
      let req = null;
      
      const onFailure = evt => {
        req.dispose();
        handler({
          content: evt.getContent(),
          statusCode: evt.getStatusCode(),
          type: evt.getType(),
          proxyData: proxyData
        });
      };
      
      const onSuccess = evt => {
        let responseHeaders = evt.getResponseHeaders();
        let content = evt.getContent();
        
        req.dispose();
        
        if (qx.core.Environment.get("qx.debug")) {
          var sha = responseHeaders["x-proxymanager-sha1"];
          if (sha != null) {
            var digest = com.zenesis.qx.remote.Sha1.digest(content);
            if (sha != digest) {
              throw new Error("Invalid SHA received from server, expected=" + sha + ", found=" + digest);
            }
          }
        }
        
        handler({
          content: content,
          statusCode: evt.getStatusCode(),
          proxyData: proxyData,
          responseHeaders: responseHeaders
        });
      }
      
      const createRequest = () => {
        var req = new qx.io.remote.Request(this.getUrl(), "POST", "text/plain");
        req.setAsynchronous(async);
        req.setTimeout(this.getTimeout());
        
        // You must set the character encoding explicitly; even if the page is
        // served as UTF8 and everything else is
        // UTF8, not specifying will lead to the server screwing up decoding
        // (presumably the default charset for the
        // JVM).
        var charset = document.characterSet || document.charset || "UTF-8";
        headers["Content-Type"] = "application/x-www-form-urlencoded; charset=" + charset;
  
        // Send it
        if (qx.core.Environment.get("com.zenesis.qx.remote.trace")) {
          // Use console.log because LogAppender would cause recursive logging
          console.log && console.log("Sending to server: " + body); 
        }
        
        if (qx.core.Environment.get("qx.debug"))
          headers["X-ProxyManager-SHA1"] = com.zenesis.qx.remote.Sha1.digest(body);
          
        headers["X-ProxyManager-ClientTime"] = new Date().getTime();
        Object.keys(headers).forEach(key => req.setRequestHeader(key, headers[key]));
        
        req.addListener("completed", onSuccess);
        req.addListener("failed", onFailure);
        req.addListener("timeout", onFailure);
        
        req.setData(body);
        return req;
      };
      
      req = createRequest();
      
      req.send();      
    }
  }
});
