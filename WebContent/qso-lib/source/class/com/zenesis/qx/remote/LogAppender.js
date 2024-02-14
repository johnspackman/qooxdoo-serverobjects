/* ************************************************************************

   server-objects - a contrib to the Qooxdoo project (http://qooxdoo.org/)

   http://qooxdoo.org

   Copyright:
     2010 Zenesis Limited, http://www.zenesis.com

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
 * Processes the incoming log entry and sends it to the client
 *
 * Unlike other appenders, this will not function until initialise() has been explicitly called
 * @ignore(com.zenesis.qx.remote.LogEntrySink)
 */
qx.Class.define("com.zenesis.qx.remote.LogAppender", {
  /*
   * ****************************************************************************
   * STATICS
   * ****************************************************************************
   */

  statics: {
    __buffer: new qx.util.RingBuffer(50),

    /**
     * Called to install the appender
     */
    install() {
      qx.log.Logger.register(com.zenesis.qx.remote.LogAppender);
      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      var t = this;
      function init() {
        var boot = PM.getBootstrapObject();
        var ifc = qx.Interface.getByName("com.zenesis.qx.remote.LogEntrySink");
        if (!ifc || !qx.Interface.classImplements(boot.constructor, ifc)) {
          this.error("Cannot send logs to " + boot.classname + " because it does not implement com.zenesis.qx.remote.LogEntrySink");
        } else PM.addListener("queuePending", t.__queuePending, t);
      }
      if (PM.hasConnected()) {
        init();
      } else {
        PM.addListenerOnce("connected", init);
      }
    },

    /**
     * Processes a single log entry
     *
     * @param entry
     *          {Map} The entry to process
     */
    process(entry) {
      this.__buffer.addEntry(entry);
    },

    /**
     * Event handler to queue requests just before ProxyManager connects to the server
     */
    __queuePending() {
      // Get the entries
      var entries = this.__buffer.getAllEntries();
      if (!entries.length) {
        return;
      }
      this.__buffer.clear();
      var output = [];
      entries.forEach(function (entry) {
        var arr = qx.log.appender.Util.toTextArray(entry);
        arr.shift();
        var le = {
          time: entry.time.getTime(),
          offset: entry.offset,
          level: entry.level,
          loggerName: entry.loggerName,
          message: arr.join(" ")
        };

        output.push(le);
      });

      // Send it
      var boot = com.zenesis.qx.remote.ProxyManager.getInstance().getBootstrapObject();
      boot.addLogEntries(output, function () {});
    }
  }
});
