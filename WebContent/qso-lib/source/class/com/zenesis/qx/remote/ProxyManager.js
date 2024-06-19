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
 * ProxyManager
 *
 * The client must provide an implementation of this class so that the proxy
 * classes have an implementation-neutral means to send data to the server
 *
 * Matches the server-side c.z.g.af.remote.ProxyManager.
 *
 * @author John Spackman [john.spackman@zenesis.com]
 * @ignore(com.zenesis.qx.remote.LogEntrySink)
 * @ignore(BigNumber)
 */
/*
 * @require(qx.core.Aspect
 * @ignore(auto-require)
 */
qx.Class.define("com.zenesis.qx.remote.ProxyManager", {
  extend: qx.core.Object,

  /**
   * Constructor
   *
   * @param proxyIo {IProxyIO} the instance for communicating with the server
   * @ignore(qx.util.Json)
   */
  construct(proxyIo) {
    super();
    this.__unprocessedResponses = [];
    if (!com.zenesis.qx.remote.ProxyManager.__initialised) {
      com.zenesis.qx.remote.ProxyManager.__initialised = true;
    }
    if (this.constructor.__instance) {
      this.warn("Not setting ProxyManager instance because one is already defined");
    } else com.zenesis.qx.remote.ProxyManager.setInstance(this);

    this.__onPollTimeoutBinding = qx.lang.Function.bind(this.__onPollTimeout, this);

    this.__serverObjects = [];
    this.__proxyIo = proxyIo;
  },

  properties: {
    /**
     * Whether to poll the server periodically for updates, even if there is
     * nothing to send
     */
    pollServer: {
      init: false,
      check: "Boolean",
      nullable: false,
      event: "changePollServer",
      apply: "_applyPollServer"
    },

    /**
     * How often to poll the server in milliseconds if pollServer is true
     */
    pollFrequency: {
      init: 5000,
      check: "Integer",
      nullable: false,
      event: "changePollFrequency",
      apply: "_applyPollFrequency"
    },

    /** Timeout */
    timeout: {
      init: null,
      check: "Integer",
      nullable: true,
      apply: "_applyTimeout"
    }
  },

  events: {
    /** Fired when an exception is raised on the server; data is the exception */
    exception: "qx.event.type.Data",

    /**
     * Fired when there is an I/O error communicating to the server; if this
     * event is not preventDefault()'d, an exception is thrown. Data is the
     * event that was returned by qx.io.Request
     */
    ioError: "qx.event.type.Data",

    /**
     * Fired when a file upload is completed, data is the FileInfo form the
     * server
     */
    uploadComplete: "qx.event.type.Data",

    /** Fired to queue any pending requests */
    queuePending: "qx.event.type.Event",

    /** Fired when connected, data is the bootstrap object */
    connected: "qx.event.type.Data",

    /** Fired when all outstanding requests are complete */
    requestsComplete: "qx.event.type.Event",

    /** Fired when shutdown is called */
    shutdown: "qx.event.type.Event"
  },

  members: {
    // Server object array and hash lookup
    __serverObjects: null,
    __sessionId: null,

    // Current value returned by getCurrentNewServerId
    __currentNewServerId: null,

    // Client-created server objects and hash lookup
    __clientObjects: null,
    __clientObjectsLastId: 0,

    // Dirty arrays
    __dirtyArrays: null,

    // Objects disposed on the client that need to be removed from the server
    __disposedServerObjects: null,

    // Classes currently being defined
    __classesBeingDefined: {},

    // Queue of commands to send to the server at the next flush
    __queue: null,

    // Callbacks for asynchronous methods
    __asyncId: 0,
    __asyncCallback: {},
    __numActiveRequests: 0,
    __queuedServerMethods: null,

    // Polling timer
    __onPollTimeoutBinding: null,
    __pollTimerId: null,

    // The property currently being set, if any (used to prevent recursive sets)
    __setPropertyObject: null,
    __setPropertyName: null,

    // The number of call backs to the server
    __numberOfCalls: 0,
    __inProcessData: 0,
    __inUploadReceived: 0,

    // Exception returned from the server, to be thrown at end of current
    // function call
    __exception: null,

    __preRequestCallback: null,
    __shutdownPromise: false,

    /**
     * The Servlet at the other end is configured to return an initial object
     * for this session; it can be any arbitrary object because QSO will
     * instantiate it correctly at this end. That becomes the entry point for
     * the application from here on in.
     *
     * Can be called multiple times, the first object is always returned.
     */
    getBootstrapObject(callbackOrPromise) {
      if (this.__serverObjects.length) {
        let value = this.__serverObjects[0];
        if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.traceNullBoot")) {
          console.log("debug: getBootstrapObject: returning existing object: " + value.toHashCode());
        }
        if (qx.lang.Type.isPromise(callbackOrPromise)) {
          callbackOrPromise.resolve(value);
        } else {
          callbackOrPromise?.(value);
        }
        return value;
      }
      if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.traceNullBoot")) {
        console.log("debug: getBootstrapObject: this.__serverObjects.length before: " + this.__serverObjects.length);
        console.log("debug: getBootstrapObject: sending bootstrap command");
      }
      var result = null;
      var msg = {
        cmd: "bootstrap",
        asyncId: ++this.__asyncId
      };

      // if (callbackOrPromise) {
      //   this.__asyncCallback[msg.asyncId] = function (result) {
      //     if (qx.lang.Type.isPromise(callbackOrPromise)) {
      //       callbackOrPromise.resolve(result);
      //     } else {
      //       callbackOrPromise?.(result);
      //     }
      //   };
      // }

      if (callbackOrPromise) {
        let serverResponseCallback = qx.lang.Type.isPromise(callbackOrPromise) ? callbackOrPromise.resolve.bind(callbackOrPromise) : callbackOrPromise;
        this.__asyncCallback[msg.asyncId] = serverResponseCallback;
      }

      this._sendCommandToServer(msg, !!callbackOrPromise);
      if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.traceNullBoot")) {
        console.log("debug: getBootstrapObject: this.__serverObjects.length after: " + this.__serverObjects.length);
      }
      var ex = this.clearException();
      if (ex) {
        throw ex;
      }
      this.fireDataEvent("connected", this.__serverObjects[0]);
      return this.__serverObjects[0];
    },

    async getBootstrapObjectAsync() {
      let promise = new qx.Promise();
      this.getBootstrapObject(promise);
      return await promise;
    },

    /**
     * Returns the unique session id for this client, null until first call to
     * getBootstrap()
     *
     * @return {String}
     */
    getSessionId() {
      return this.__sessionId;
    },

    /**
     * Detects whether the connection has ever been made, i.e. whether
     * getBootstrapObject() has already been called.
     *
     * @return {Boolean} true if connected
     */
    hasConnected() {
      return this.__serverObjects.length > 0;
    },

    /**
     * Returns the IProxyIo instance
     *
     * @return {IProxyIo}
     */
    getProxyIo() {
      return this.__proxyIo;
    },

    /**
     * Shutsdown the connection, and cannot be undone.  Returns a promise which
     * completes when all currently active connections are complete
     */
    shutdown() {
      this.setPollServer(false);
      if (!this.__shutdownPromise) {
        this.__shutdownPromise = new qx.Promise();
        this.fireEvent("shutdown");
        if (this.__numActiveRequests === 0) {
          this.__shutdownPromise.resolve();
        }
      }
      return this.__shutdownPromise;
    },

    /**
     * Registers a client object
     *
     * @return {Integer} the new ID for this object
     */
    registerClientObject(obj) {
      qx.core.Assert.assertTrue(!obj.hasServerId());
      if (!this.__clientObjects) {
        this.__clientObjects = {};
      }
      var index = ++this.__clientObjectsLastId;
      this.__clientObjects[index] = obj;
      /*
      this.debug("Registering client object " + obj.toHashCode() + " " + obj.classname + " client id=" + index);
      obj.addListenerOnce("changeServerId", function(evt) {
        this.debug("Changing ServerId for " + evt.getTarget().toHashCode() + " from " + evt.getOldData() + " to " + evt.getData());
      });
       */
      return 0 - index;
    },

    /**
     * Unregisters a client object
     */
    unregisterClientObject(obj) {
      var id = obj.getServerId() * -1;
      qx.core.Assert.assertTrue(id > 0, "Object does not have a client ID, id=" + id);
      qx.core.Assert.assertTrue(this.__clientObjects[id] === obj, "Object is not a pending client object");
      //this.debug("Unregistering client object " + obj.toHashCode() + " client id=" + id);
      delete this.__clientObjects[id];
    },

    /**
     * Handles the "completed" event from the Request
     *
     * @param evt
     *          {Response} the response event
     * @lint ignoreDeprecated(eval)
     */
    __expectedRequestIndex: 0,
    __unprocessedResponses: null,
    __onResponseReceived(ioData) {
      if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.traceNullBoot")) {
        console.log("debug: __onResponseReceived: start");
      }
      this.__numActiveRequests--;
      var txt = ioData.content;
      var statusCode = ioData.statusCode;
      var proxyData = ioData.proxyData;

      if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.perfTrace")) {
        proxyData.receivedTime = new Date().getTime();
      }
      proxyData.txt = txt;

      var result = null;
      var reqIndex = null;

      this.__inProcessData++;
      try {
        if (statusCode == 200) {
          var sessionId = ioData.responseHeaders["x-proxymanager-sessionid"];
          if (sessionId) {
            if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.traceNullBoot")) {
              console.log("debug: __onResponseReceived: setting sessionId=" + sessionId);
            }
            if (qx.core.Environment.get("qx.debug")) {
              if (this.__sessionId && this.__sessionId != sessionId) {
                this.warn("Changing session ID from " + this._sessionId + " to " + sessionId);
              }
            }
            this.__sessionId = sessionId;
          }
          reqIndex = parseInt(ioData.responseHeaders["x-proxymanager-requestindex"], 10);
          if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.traceOverlaps")) {
            console.log && console.log("__onResponseReceived 1: request index=" + reqIndex + ", __expectedRequestIndex=" + this.__expectedRequestIndex);
          }

          qx.core.Assert.assertTrue(isNaN(reqIndex) || reqIndex === proxyData.reqIndex);
          result = this.__processResponse(proxyData);
        } else {
          this._handleIoError(ioData);
          if (typeof proxyData.async == "function") {
            proxyData.async(ioData);
          }
        }

        return result;
      } finally {
        if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.traceOverlaps")) {
          console.log && console.log("__onResponseReceived 2: request index=" + reqIndex + ", __expectedRequestIndex=" + this.__expectedRequestIndex);
        }
        this.__inProcessData--;
        if (this.__shutdownPromise && this.__numActiveRequests === 0) {
          this.__shutdownPromise.resolve();
        }
        if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.traceNullBoot")) {
          console.log("debug: __onResponseReceived: finished");
        }
      }
    },

    __processResponse(proxyData) {
      function process(proxyData, outOfSequence) {
        if (!proxyData.processed) {
          proxyData.processed = true;

          var txt = proxyData.txt.trim();
          var result;
          if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.traceOverlaps")) {
            console.log("process: start: " + proxyData.reqIndex + ", expected=" + t.__expectedRequestIndex + ", outOfSequence=" + outOfSequence);
          }
          try {
            if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.trace")) {
              console.log && console.log("received: txt=" + txt); // Use console.log because
              // LogAppender would cause
              // recursive logging
            }
            if (txt.length) {
              var data = eval("(" + txt + ")");
              result = t._processData(data, proxyData.asyncIds);
            }
            if (typeof proxyData.async == "function") {
              proxyData.async();
            }
          } catch (e) {
            t.error("Exception during receive: " + t.__describeException(e), e);
            t._setException(e);
            if (typeof proxyData.async == "function") {
              proxyData.async();
            }
          } finally {
            if (t.getPollServer()) {
              t._killPollTimer();
              t._startPollTimer();
            }
          }

          proxyData.endTime = new Date().getTime();
          var stats = proxyData.async ? t.__stats.async : t.__stats.sync;
          stats.count++;
          var time = proxyData.endTime - proxyData.startTime;
          stats.totalTime += time;
          stats.peakTime = Math.max(stats.peakTime, time);
        }

        if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.traceOverlaps")) {
          console.log("process: loaded: " + proxyData.reqIndex + ", expected=" + t.__expectedRequestIndex + ", outOfSequence=" + outOfSequence);
        }
        if (!outOfSequence) {
          t.__expectedRequestIndex = proxyData.reqIndex + 1;
          if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.traceOverlaps")) {
            console.log("process: updated expected=" + t.__expectedRequestIndex);
          }

          for (var i = 0; i < t.__unprocessedResponses.length; i++) {
            if (t.__unprocessedResponses[i].reqIndex == t.__expectedRequestIndex) {
              var next = qx.lang.Array.removeAt(t.__unprocessedResponses, i--);
              if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.traceOverlaps")) {
                console.log("process: shifting, next=" + next.reqIndex + ", next.processed=" + next.processed);
              }
              if (!next.processed) {
                process(next);
                break;
              } else {
                t.__expectedRequestIndex = next.reqIndex + 1;
              }
            }
          }
        }

        if (t.__unprocessedResponses.length == 0) {
          t.fireEvent("requestsComplete");
        }
        return result;
      }

      var t = this;

      if (proxyData.reqIndex < t.__expectedRequestIndex) {
        throw new Error("Unexpected request index " + proxyData.reqIndex + ", expected=" + t.__expectedRequestIndex);
      }
      if (proxyData.reqIndex > t.__expectedRequestIndex) {
        if (!proxyData.async) {
          this.warn("Warning: This synchronous request arrived out of sequence, reqIndex=" + proxyData.reqIndex + ", expected=" + t.__expectedRequestIndex);
          t.__unprocessedResponses.push(proxyData);
          process(proxyData, true);
        } else {
          if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.traceOverlaps")) {
            console.log("queuing " + proxyData.reqIndex + ", expected=" + t.__expectedRequestIndex);
          }
          t.__unprocessedResponses.push(proxyData);
        }
        return;
      }

      return process(proxyData);
    },

    __stats: {
      async: {
        count: 0,
        totalTime: 0,
        peakTime: 0
      },

      sync: {
        count: 0,
        totalTime: 0,
        peakTime: 0
      }
    },

    /**
     * Returns a basic usage and performance stats
     */
    getStats() {
      return this.__stats;
    },

    /**
     * Called when there is an error in the IO to the server
     */
    _handleIoError(ioData) {
      this.error("Error returned by server, code=" + ioData.statusCode);
      if (this.fireDataEvent("ioError", ioData)) {
        this._setException(new Error("statusCode=" + ioData.statusCode));
      }
      let err = new Error("Error returned by server, code=" + ioData.statusCode);
      ioData.proxyData.asyncIds.forEach(asyncId => {
        try {
          this.__asyncCallback[asyncId](null, err);
        } catch (ex) {
          console.error("Error rejecting promise: " + ex);
        }
      });
    },

    /**
     * Called to handle the response from an upload
     *
     * @lint ignoreDeprecated(eval)
     */
    uploadResponse(txt) {
      txt = txt.trim();
      this.__inProcessData++;
      try {
        if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.trace")) {
          console.log && console.log("received: txt=" + txt);
        } // Use console.log because
        // LogAppender would cause
        // recursive logging
        if (!txt.length) {
          return null;
        }
        var data = eval("(" + txt + ")");
        return this._processData(data);
      } catch (e) {
        this.error("Exception during uploadResponse: " + this.__describeException(e));
        throw e;
      } finally {
        this.__inProcessData--;
      }
    },

    /**
     * Called to interpret the text returned by the server and perform any
     * commands
     *
     * @param data
     *          {Object} the response compiled from JSON
     */
    _processData(data, asyncIds) {
      var t = this;
      var result = null;

      // Map client IDs first
      for (var i = 0, l = data.length; i < l; i++) {
        var elem = data[i];
        if (elem.type != "mapClientId") {
          continue;
        }

        var index = 0 - elem.data.clientId;
        var clientObject = this.__clientObjects[index];
        qx.core.Assert.assertEquals(elem.data.clientId, clientObject.getServerId());

        clientObject.setServerId(elem.data.serverId);
        if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.traceNullBoot")) {
          console.log("debug: processdata: added new object: " + clientObject);
        }
        this.__serverObjects[elem.data.serverId] = clientObject; //note
      }

      var stats = null;
      if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.perfTrace")) {
        class ClassStats {
          constructor(classname) {
            this.classname = classname;
            this.numObjects = 0;
            this.defineTime = 0;
            this.constructorTime = 0;
            this.propertiesTime = 0;
            this.properties = {};
            this.prefetch = {};
          }

          forProperty(propName) {
            var propStats = this.properties[propName];
            if (!propStats) {
              propStats = this.properties[propName] = { name: propName, numSets: 0, time: 0 };
            }
            return propStats;
          }

          forPrefetch(methodName) {
            var prefetchStats = this.prefetch[methodName];
            if (!prefetchStats) {
              prefetchStats = this.prefetch[methodName] = { name: methodName, numSets: 0, time: 0 };
            }
            return prefetchStats;
          }
        }

        class AllStats {
          constructor() {
            this.numObjects = 0;
            this.numCallbacks = 0;
            this.callbacksTime = 0;
            this.startTime = new Date().getTime();
            this.statsByClass = {};
          }

          forClass(classname) {
            var classStats = this.statsByClass[classname];
            if (!classStats) {
              classStats = this.statsByClass[classname] = new ClassStats(classname);
            }
            return classStats;
          }

          end() {
            this.totalTime = new Date().getTime() - this.startTime;
          }

          dumpStats() {
            const SPACES = "                    ";
            function rpad(str, len, spaces) {
              if (spaces === undefined) {
                spaces = SPACES;
              }
              str = str + "";
              while (str.length < len) {
                var add = len - str.length;
                if (add >= spaces.length) {
                  str += spaces;
                } else str += spaces.substring(0, add);
              }
              return str;
            }
            function lpad(str, len, spaces) {
              if (spaces === undefined) {
                spaces = SPACES;
              }
              str = str + "";
              while (str.length < len) {
                var add = len - str.length;
                if (add >= spaces.length) {
                  str = spaces + str;
                } else str = spaces.substring(0, add) + str;
              }
              return str;
            }
            var classnameLen = 0;
            var numSets = 0;
            var setsTime = 0;
            Object.keys(this.statsByClass).forEach(classname => (classnameLen = Math.max(classnameLen, classname.length)));

            console.log(rpad("Classname", classnameLen + 1) + " Count  Define   Constr   Props    Slowest Properties");
            Object.keys(this.statsByClass)
              .sort()
              .forEach(classname => {
                let classStats = this.statsByClass[classname];
                let str =
                  rpad(classname, classnameLen + 1) +
                  " " +
                  lpad(classStats.numObjects, 6) +
                  " " +
                  lpad(classStats.defineTime, 6) +
                  "ms " +
                  lpad(classStats.constructorTime, 6) +
                  "ms " +
                  lpad(classStats.propertiesTime, 6) +
                  "ms ";

                let properties = Object.keys(classStats.properties)
                  .sort((l, r) => {
                    l = classStats.properties[l].time;
                    r = classStats.properties[r].time;
                    return l < r ? 1 : l > r ? -1 : 0;
                  })
                  .map(propName => classStats.properties[propName]);
                properties.forEach(propStats => {
                  numSets += propStats.numSets;
                  setsTime += propStats.time;
                });

                for (let i = 0; i < properties.length && i < 2; i++) {
                  let propStats = properties[i];
                  if (i) {
                    str += ", ";
                  }
                  str += propStats.name + "= #" + propStats.numSets + " @ " + propStats.time + "ms";
                }
                console.log(str);
              });
            console.log("numObjects: " + this.numObjects + ", Callbacks: " + this.numCallbacks + " @ " + this.callbacksTime + "ms" + ", Property sets: " + numSets + " @ " + setsTime + "ms");
            console.log("TOTAL TIME: " + this.totalTime + "ms");
          }
        }
        stats = new AllStats();
      }

      for (var i = 0, l = data.length; i < l; i++) {
        var elem = data[i];
        var type = elem.type;

        // Init
        if (type == "bootstrap") {
          var asyncId = elem.data.asyncId;
          this.__sessionId = elem.data.sessionId;
          result = this.readProxyObject(elem.data.bootstrap, stats);
          var cb = asyncId ? this.__asyncCallback[asyncId] : null;
          if (cb) {
            delete this.__asyncCallback[asyncId];
            cb(result);
          }

          // Function return
        } else if (type == "return") {
          var asyncId = elem.data.asyncId;
          if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.perfTrace")) {
            var startTime = new Date().getTime();
          }
          result = this.readProxyObject(elem.data.result, stats);
          var cb = this.__asyncCallback[asyncId];
          if (cb) {
            var startTime = new Date().getTime();
            delete this.__asyncCallback[asyncId];
            cb(result);
            var ms = new Date().getTime() - startTime;
            if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.perfTrace")) {
              stats.numCallbacks++;
              stats.callbacksTime += ms;
            }
          }

          // Upload
        } else if (type == "upload") {
          var fileInfos = this.readProxyObject(elem.data, stats);
          if (!result) {
            result = [];
          }
          this.__inUploadReceived++;
          try {
            for (var j = 0; j < fileInfos.length; j++) {
              result.push(fileInfos[j]);
              this.fireDataEvent("uploadComplete", fileInfos[j]);
            }
          } finally {
            this.__inUploadReceived--;
          }

          // An exception was thrown
        } else if (type == "exception") {
          this._handleServerException(elem.data, "function", asyncIds);

          // A client-created object has been registered on the server, update
          // the IDs to server IDs
        } else if (type == "mapClientId") {
          // Now read in new/changed properties
          this.readProxyObject(elem.object, stats);

          // Setting a property failed with an exception - change the value back
          // and handle the exception
        } else if (type == "restore") {
          var obj = this.readProxyObject(elem.object, stats);
          try {
            var value = this.readProxyObject(elem.data.oldValue, stats);
            this.setPropertyValueFromServer(obj, elem.name, value);
          } catch (e) {
            // Ignore it - we were only trying to recover from a server
            // exception
          }
          this._handleServerException(elem.data, "property", asyncIds); // A server property value changed, update the client
        } else if (type == "set") {
          var obj = this.readProxyObject(elem.object, stats);
          var value = this.readProxyObject(elem.data, stats);
          this.setPropertyValueFromServer(obj, elem.name, value); // An on demand server property value changed, clear the cache
        } else if (type == "expire") {
          var obj = this.readProxyObject(elem.object, stats);
          var upname = qx.lang.String.firstUp(elem.name);
          obj["expire" + upname](false); // A server property value changed, update the client
        } else if (type == "edit-array") {
          (function () {
            var serverObject = t.readProxyObject(elem.object, stats);
            var savePropertyObject = t.__setPropertyObject;
            var savePropertyName = t.__setPropertyName;
            t.__setPropertyObject = serverObject;
            t.__setPropertyName = null;
            try {
              elem.data.forEach(function (data) {
                if (data.removed) {
                  data.removed.forEach(function (item) {
                    var obj = t.readProxyObject(item, stats);
                    serverObject.remove(obj);
                  });
                }
                if (data.added) {
                  data.added.forEach(function (item) {
                    var obj = t.readProxyObject(item, stats);
                    serverObject.push(obj);
                  });
                }
                if (data.put) {
                  data.put.forEach(function (entry) {
                    var key = t.readProxyObject(entry.key, stats);
                    var value = t.readProxyObject(entry.value, stats);
                    serverObject.put(key, value);
                  });
                }
              });
            } finally {
              t.__setPropertyObject = savePropertyObject;
              t.__setPropertyName = savePropertyName;
            }
          })();

          // The server has sent a class definition
        } else if (type == "define") {
          this.getClassOrCreate(elem.object, stats);

          // An event was fired on the server
        } else if (type == "fire") {
          var obj = this.readProxyObject(elem.object, stats);
          var eventData = elem.data ? this.readProxyObject(elem.data, stats) : null;
          obj.fireDataEvent(elem.name, eventData);

          // Explicitly load a type onto the client
        } else if (type == "load-type") {
          var clazz = this.getClassOrCreate(elem.object, stats);

          // Unknown!
        } else qx.core.Assert.assertTrue(false, "Unexpected type of command from server: " + type);
      }

      if (this.__numActiveRequests == 0 && this.__queuedServerMethods && this.__queuedServerMethods.length && !this.__shutdownPromise) {
        // Flatten the call stack when we flush the queue, and check that it is still valid to flush when
        //  the timeout kicks in
        setTimeout(
          function () {
            if (this.__numActiveRequests == 0 && this.__queuedServerMethods && this.__queuedServerMethods.length && !this.__shutdownPromise) {
              this._sendCommandToServer(null, true, true);
            }
          }.bind(this),
          1
        );
      }

      if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.perfTrace")) {
        stats.end();
        // Only report stats that were at least 100ms
        if (stats.totalTime >= 100) {
          stats.dumpStats();
        }
      }
      return result;
    },

    /**
     * Returns the serverid of the opbject currently being created; this is used
     * by the MProxy.initialiseProxy method, and resets after use
     */
    getCurrentNewServerId() {
      var result = this.__currentNewServerId;
      this.__currentNewServerId = null;
      return result;
    },

    /**
     * Reads a proxy object from the server and either creates a new object
     * (creating classes as required) or returns an existing one
     */
    readProxyObject(data, stats) {
      if (typeof data == "undefined" || data === null) {
        return null;
      }
      var result = null;
      var t = this;

      function readArray(data) {
        var result;

        // Do we really have to process each element?
        var ok = true;
        for (var i = 0; ok && i < data.length; i++) {
          if (typeof data[i] == "object") {
            ok = false;
          }
        }

        // All simple values, just use the parsed data
        if (ok) {
          result = data;
          // Copy values by hand
        } else {
          result = [];
          for (var i = 0; i < data.length; i++) {
            result[i] = t.readProxyObject(data[i], stats);
          }
        }

        return result;
      }

      function readMap(data) {
        var result;

        // Do we really have to process every value?
        var ok = true;
        for (var propName in data)
          if (typeof data[propName] == "object") {
            ok = false;
            break;
          }

        // All simple values? then just use the already parsed data
        if (ok) {
          result = data;
          // Copy one by one, recursively
        } else {
          /*
           * Note that ordering is not defined and if server objects with
           * recursive references are passed for the first time in a map, they
           * may fail to create.
           */
          result = {};
          for (var propName in data) {
            var propValue = data[propName];
            if (propValue) {
              propValue = t.readProxyObject(propValue, stats);
            }
            result[propName] = propValue;
          }
        }

        return result;
      }

      function readServerObject(data) {
        var result;
        var serverId = data.serverId;

        // Get or create it
        result = t.getServerObject(serverId);
        if (!result && data.clazz) {
          var clazz = t.getClassOrCreate(data.clazz, stats);
          if (!clazz) {
            t.error("Cannot find class for " + data.clazz);
            throw new Error("Cannot find class for " + data.clazz);
          }

          var startTime;
          if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.perfTrace")) {
            startTime = new Date().getTime();

            // Collects stats
            if (stats) {
              stats.numObjects++;
              var classStats = stats.forClass(data.clazz);
              classStats.numObjects++;
            }
          }

          // Collect constructor args now in case they refer to a Proxied object
          // (which would cause recursion
          // and would conflict with __currentNewServerId being a single use
          // global
          var constructorArgs = undefined;
          if (data.constructorArgs) {
            constructorArgs = readArray(data.constructorArgs);
          }

          t.__currentNewServerId = serverId;
          t.__inConstructor = true;
          if (data.constructorArgs) {
            function construct(constructor, args) {
              function F() {
                return constructor.apply(this, args);
              }
              F.prototype = constructor.prototype;
              return new F();
            }
            result = construct(clazz, constructorArgs);
          } else result = new clazz();
          t.__inConstructor = false;
          qx.core.Assert.assertEquals(serverId, result.getServerId());
          if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.traceNullBoot")) {
            console.log("debug: readServerObject: added new object: " + result.toHashCode());
          }
          t.__serverObjects[serverId] = result; //note: is called from responsereceived

          if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.perfTrace")) {
            var ms = new Date().getTime() - startTime;
            if (stats) {
              classStats.constructorTime += ms;
            }
          }
        } else if (!result) {
          throw new Error("Cannot find serverId " + serverId + ", probable recursion in loading");
        }

        if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.perfTrace")) {
          var startTime = new Date().getTime();
          var classStats = stats.forClass(data.clazz);
        }

        // Assign any values
        if (data.order) {
          if (data.values.uuid) {
            t.setPropertyValueFromServer(result, "uuid", data.values.uuid);
          }
          for (var i = 0; i < data.order.length; i++) {
            var propName = data.order[i];
            if (propName == "uuid") {
              continue;
            }
            var propValue = data.values[propName];

            if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.perfTrace")) {
              if (stats) {
                var propStartTime = new Date().getTime();
              }
            }

            if (propValue) {
              propValue = t.readProxyObject(propValue, stats);
            }
            t.setPropertyValueFromServer(result, propName, propValue);

            if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.perfTrace")) {
              if (stats) {
                var ms = new Date().getTime() - propStartTime;
                var propStats = classStats.forProperty(propName);
                propStats.numSets++;
                propStats.time += ms;
              }
            }
          }
        }

        // Prefetched method return values
        if (data.prefetch) {
          for (var methodName in data.prefetch) {
            if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.perfTrace")) {
              if (stats) {
                var prefetchStartTime = new Date().getTime();
              }
            }

            var value = data.prefetch[methodName];
            if (!result.$$proxy.cachedResults) {
              result.$$proxy.cachedResults = {};
            }
            if (value) {
              value = t.readProxyObject(value, stats);
            }
            result.$$proxy.cachedResults[methodName] = value;

            if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.perfTrace")) {
              if (stats) {
                var ms = new Date().getTime() - prefetchStartTime;
                var prefetchStats = classStats.forPrefetch(methodName);
                prefetchStats.numSets++;
                prefetchStats.time += ms;
              }
            }
          }
        }

        if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.perfTrace")) {
          if (stats) {
            var ms = new Date().getTime() - startTime;
            classStats.propertiesTime += ms;
          }
        }
        return result;
      }

      if (qx.lang.Type.isArray(data)) {
        result = readArray(data);
      } else if (data instanceof BigNumber) {
        result = data;
      } else if (typeof data == "object") {
        // Object - is it a server object or a map?
        if (data.serverId !== undefined) {
          result = readServerObject(data);
        } else if (typeof data["$date"] == "string") {
          return new Date(data["$date"]);
        } else if (typeof data["$numberDecimal"] == "string") {
          return BigNumber(data["$numberDecimal"]);
        } else {
          result = readMap(data);
        }
      } else {
        // Scalar value, just use it direct
        result = data;
      }

      return result;
    },

    /**
     * Reads a "clazz" and interprets it to return a class, creating new class
     * definitions as required
     *
     * @lint ignoreDeprecated(eval)
     */
    getClassOrCreate(data, stats) {
      var t = this;

      // If it's a string, then it's an existing class we need to create
      if (typeof data == "string") {
        if (this.__classesBeingDefined[data]) {
          return null;
        }
        var clazz = eval(data);
        return clazz;
      }

      function cleanup(data) {
        for (var name in data)
          if (data[name] === undefined) {
            delete data[name];
          }
        return data;
      }

      var startTime = 0;
      if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.perfTrace")) {
        startTime = new Date().getTime();
      }

      // Types are not created when encountered because that can lead to unsolvable recursive
      // problems; definitions are queued here instead
      var deferredTypes = [];

      this.__classesBeingDefined[data.className] = true;
      try {
        // Create the JSON definition for qx.Class
        var def;
        var strConstructorCode = null;
        var strDestructorCode = "";
        var strDeferCode = "clazz.$$eventMeta = {};\n" + "clazz.$$methodMeta = {};\n";
        if (data.isInterface) {
          def = {
            members: {},
            statics: {}
          };
        } else {
          strConstructorCode = "var args = qx.lang.Array.fromArguments(arguments);\n" + "args.unshift(arguments);\n" + "this.base.apply(this, args);\n" + "this.initialiseProxy();\n";
          def = {
            members: {},
            statics: {}
          };

          if (data.extend) {
            def.extend = this.getClassOrCreate(data.extend, stats);
          } else {
            def.extend = qx.core.Object;
          }
        }

        // Add interfaces
        if (data.interfaces) {
          var interfaces = data.interfaces;
          for (var i = 0; i < data.interfaces.length; i++) {
            interfaces[i] = this.getClassOrCreate(interfaces[i], stats);
          }
          if (interfaces.length) {
            if (data.isInterface) {
              def.extend = interfaces;
            } else def.implement = interfaces;
          }
        }

        // Add methods
        if (data.methods) {
          for (var methodName in data.methods) {
            var method = data.methods[methodName];
            method.name = methodName;
            if (data.isInterface) {
              def.members[methodName] = new Function("");
            } else if (method.staticMethod) {
              def.statics[methodName] = new Function("return com.zenesis.qx.remote.ProxyManager._callServer(" + data.className + ', "' + methodName + '", qx.lang.Array.fromArguments(arguments));');
            } else {
              def.members[methodName] = new Function('return this._callServer("' + methodName + '", qx.lang.Array.fromArguments(arguments));');
              def.members[methodName + "Async"] = new Function(
                "var args = qx.lang.Array.fromArguments(arguments);" +
                  "return new qx.Promise(function(resolve, reject) {" +
                  "args.push(function() {" +
                  "resolve.apply(this, qx.lang.Array.fromArguments(arguments));" +
                  "});" +
                  'this._callServer("' +
                  methodName +
                  '", args);' +
                  "}, this);"
              );
            }

            if (method.returnType && typeof method.returnType == "object") {
              deferredTypes.push(method.returnType);
            }

            var params = method.parameters;
            if (params) {
              for (var i = 0; i < params.length; i++) {
                if (params[i] && typeof params[i] == "object") {
                  deferredTypes.push(params[i]);
                }
              }
            }
            if (method.anno) {
              def.members["@" + methodName] = method.anno;
            }

            strDeferCode +=
              "clazz.$$methodMeta." +
              methodName +
              " = " +
              JSON.stringify(
                cleanup({
                  isServer: true,
                  returnType: method.returnType,
                  map: method.map,
                  cacheResult: method.cacheResult,
                  returnArray: method.returnArray
                })
              ) +
              ";\n";
          }
        }

        // Add properties
        if (data.properties) {
          def.properties = {};
          for (var propName in data.properties) {
            var upname = qx.lang.String.firstUp(propName);
            var fromDef = data.properties[propName];
            fromDef.name = propName;

            if (fromDef.clazz && typeof fromDef.clazz == "object") {
              deferredTypes.push(fromDef.clazz);
            }

            var toDef = (def.properties[propName] = { "@": [] });
            var propertyAnno = null;

            function getPropertyAnno() {
              if (propertyAnno) {
                return propertyAnno;
              }
              propertyAnno = new com.zenesis.qx.remote.annotations.Property();
              toDef["@"].push(propertyAnno);
              return propertyAnno;
            }

            // Define the property
            toDef.nullable = fromDef.nullable;
            if (fromDef.readOnly) {
              getPropertyAnno().setReadOnly(true);
            }
            if (!toDef.nullable && fromDef.check) {
              var defaultValue = com.zenesis.qx.remote.ProxyManager.__NON_NULLABLE_DEFAULTS[fromDef.check];
              if (defaultValue !== undefined) {
                if (typeof defaultValue == "function") {
                  toDef.init = defaultValue();
                } else toDef.init = defaultValue;
              }
            }
            if (fromDef.event) {
              toDef.event = fromDef.event;
            }

            var arrayClassName = null;
            var needsTransform = false;
            if ((fromDef.map || fromDef.array) && fromDef.arrayClass) {
              arrayClassName = fromDef.arrayClass.className;
              deferredTypes.push(fromDef.arrayClass);
            }

            if (fromDef.map) {
              var NATIVE_KEY_TYPES = {
                String: true,
                Integer: true,
                Double: true,
                Float: true
              };

              fromDef.nativeKeyType = !fromDef.keyTypeName || NATIVE_KEY_TYPES[fromDef.keyTypeName];
              if (fromDef.array && fromDef.array == "wrap") {
                toDef.check = arrayClassName || "com.zenesis.qx.remote.Map";
              }
              if (fromDef.keyTypeName) {
                getPropertyAnno().setKeyTypeName(fromDef.keyTypeName);
              }
              if (fromDef.componentTypeName) {
                getPropertyAnno().setComponentTypeName(fromDef.componentTypeName);
              }

              // Checks
            } else if (fromDef.check) {
              toDef.check = fromDef.check || fromDef.clazz;

              // Handle arrays
            } else if (fromDef.array) {
              if (fromDef.array == "wrap") {
                toDef.transform = "_transformToDataArray";
                toDef.check = arrayClassName || "qx.data.Array";
              } else toDef.check = "Array";
              if (fromDef.componentTypeName) {
                getPropertyAnno().setComponentTypeName(fromDef.componentTypeName);
              }
            }

            if ((fromDef.map || fromDef.array) && fromDef.create) {
              strConstructorCode += "this.set" + upname + "(new " + toDef.check + "());\n";
              strDestructorCode += "this.set" + upname + "(null);\n";
            }

            // Annotations
            if (fromDef.anno) {
              fromDef.anno.forEach(anno => toDef["@"].push(eval(anno)));
            }
            if (toDef.check === "Date") {
              needsTransform = true;
            }

            // Create an apply method
            toDef.apply = "_apply" + upname;
            def.members["_apply" + upname] = new Function("value", "oldValue", "name", 'this._applyProperty("' + propName + '", value, oldValue, name);');
            if (needsTransform) {
              toDef.apply = "_transform" + upname;
              def.members["_transform" + upname] = new Function("value", "oldValue", 'this._transformProperty("' + propName + '", value, oldValue);');
            }

            // onDemand properties - patch it later
            if (fromDef.onDemand) {
              def.members["get" + upname] = new Function("async", "return this._getPropertyOnDemand('" + propName + "', async);");
              def.members["expire" + upname] = new Function("sendToServer", "return this._expirePropertyOnDemand('" + propName + "', sendToServer);");
              def.members["set" + upname] = new Function("value", "async", "return this._setPropertyOnDemand('" + propName + "', value, async);");
              def.members["get" + upname + "Async"] = new Function("async", "return this._getPropertyOnDemandAsync('" + propName + "');");
              def.members["get" + upname + "Async"] = new Function(
                "return new qx.Promise(function(resolve) {" + "  this._getPropertyOnDemand('" + propName + "', function(result) {" + "    resolve(result);" + "  });" + "}, this);"
              );
            } else {
              def.members["get" + upname + "Async"] = new Function("return qx.Promise.resolve(this.get" + upname + "()).bind(this);");
            }

            // Meta data
            strDeferCode +=
              "qx.lang.Object.mergeWith(clazz.$$properties." +
              propName +
              ", " +
              JSON.stringify(
                cleanup({
                  isServer: true,
                  sync: fromDef.sync,
                  onDemand: fromDef.onDemand,
                  readOnly: fromDef.readOnly,
                  array: fromDef.array,
                  arrayClass: fromDef.arrayClass,
                  map: fromDef.map,
                  nativeKeyType: fromDef.nativeKeyType
                })
              ) +
              ");\n";
          }
        }

        // Add events
        if (data.events) {
          def.events = {};
          for (var eventName in data.events) {
            var fromDef = data.events[eventName];
            if (!fromDef.isProperty) {
              def.events[eventName] = "qx.event.type.Data";
            }
            strDeferCode +=
              "clazz.$$eventMeta." +
              eventName +
              " = " +
              JSON.stringify({
                isServer: true,
                isProperty: fromDef.isProperty
              }) +
              ";\n";
          }
        }

        // Define the class
        var clazz;
        if (data.isInterface) {
          clazz = qx.Interface.define(data.className, def) || qx.Interface.getByName(data.className);
        } else {
          def.construct = new Function(strConstructorCode);
          if (strDestructorCode) {
            def.destruct = new Function(strDestructorCode);
          }
          strDeferCode += "com.zenesis.qx.remote.MProxy.deferredClassInitialisation(clazz);\n";
          def.defer = new Function("clazz", strDeferCode);
          clazz = qx.Class.define(data.className, def);
          clazz = qx.Class.getByName(data.className);
        }
      } catch (e) {
        throw e;
      } finally {
        delete this.__classesBeingDefined[data.className];
      }

      // Create dependent classes
      for (var i = 0; i < deferredTypes.length; i++) {
        this.getClassOrCreate(deferredTypes[i], stats);
      }

      if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.perfTrace")) {
        if (stats) {
          var ms = new Date().getTime() - startTime;
          stats.forClass(data.className).defineTime = ms;
        }
      }

      // Done
      return clazz;
    },

    /**
     * Serialises a value for sending to the server
     */
    serializeValue(value, opts) {
      const PM = com.zenesis.qx.remote.ProxyManager;

      if (!value) {
        return value;
      }
      if (value instanceof BigNumber) {
        return PM.__PREFIX + "BigNumber(" + value + ")" + PM.__SUFFIX;
      }
      opts = opts || {};
      var to = typeof value;
      if (["boolean", "number", "string"].indexOf(to) > -1) {
        return value;
      }
      if (["function", "symbol"].indexOf(to) > -1) {
        this.error("Cannot serialize an object of type " + to + " to the server");
        return null;
      }

      if (value instanceof qx.core.Object && value.isDisposed()) {
        if (!value.$$proxy || (this.__disposedServerObjects && !this.__disposedServerObjects[value.getServerId()])) {
          throw new Error("Cannot serialise " + value.classname + " [" + value.toHashCode() + "] because it is disposed, object=" + value);
        }
      }

      // If serialising an entire array or map, then it will no longer be dirty;
      // this is important
      // otherwise the subsequent change records will cause duplicate entries
      if (value && this.__queuingCommandsForServer && this.__dirtyArrays && typeof value.toHashCode == "function") {
        delete this.__dirtyArrays[value.toHashCode()];
      }

      if (value instanceof com.zenesis.qx.remote.Map) {
        var result = {};
        for (var keys = value.getKeys(), i = 0; i < keys.getLength(); i++) {
          var key = keys.getItem(i);
          result[key] = this.serializeValue(value.get(key), opts);
        }
        return result;
      }

      if (value instanceof qx.data.Array) {
        value = value.toArray();
      }

      if (qx.lang.Type.isArray(value)) {
        var send = [];
        for (var j = 0; j < value.length; j++) {
          if (typeof value[j] === "undefined" || value[j] === null) {
            send[j] = null;
          } else {
            send[j] = this.serializeValue(value[j], opts);
          }
        }
        return send;
      }

      if (qx.lang.Type.isDate(value)) {
        try {
          value = PM.__PREFIX + "Date(" + PM.formatISO(value) + ")" + PM.__SUFFIX;
        } catch (ex) {
          qx.log.Logger.error("Error while formatting date=" + value + ", ex=" + ex + ", stack=" + JSON.stringify(qx.dev.StackTrace.getStackTrace(), null, 2));

          value = null;
        }
        return value;
      }

      if (qx.Class.hasMixin(value.constructor, com.zenesis.qx.remote.MProxy)) {
        var id = value.getServerId();
        if (id < 0) {
          this._queueClientObject(0 - id);
        }
        return value.getServerId();
      }

      if (value instanceof qx.core.Object) {
        this.error("Cannot serialize a Qooxdoo object to the server unless it implements com.zenesis.qx.remote.Proxied");
        return null;
      }

      // Assume it's an ordinary map object; deliberately not using
      // hasOwnProperty()
      var result = {};
      for (var name in value) {
        result[name] = this.serializeValue(value[name], opts);
      }
      return result;
    },

    /**
     * Called by Proxy to call a server method on a server object - not to be
     * invoked directly
     *
     * @param serverObject
     *          {Object} the server object
     * @param method
     *          {Object} method definition
     * @param args
     *          {Array} the arguments passed to the method
     */
    callServerMethod(serverObject, methodName, args) {
      var isClass = serverObject && serverObject.$$type === "Class";
      var methodDef;
      if (isClass) {
        methodDef = com.zenesis.qx.remote.MProxy.getMethodDefinition(serverObject, methodName);
      } else {
        if (serverObject.isDisposed()) {
          throw new Error("Cannot call method " + serverObject.classname + "." + methodName + " on [" + serverObject.toHashCode() + "] because it is disposed, object=" + serverObject);
        }

        methodDef = com.zenesis.qx.remote.MProxy.getMethodDefinition(serverObject.constructor, methodName);

        // Can we get it from the cache?
        if (methodDef && methodDef.cacheResult && serverObject.$$proxy.cachedResults && serverObject.$$proxy.cachedResults[methodName]) {
          return serverObject.$$proxy.cachedResults[methodName];
        }
      }

      // Serialise the request
      var parameters = [];
      var notify = [];
      for (var i = 0; i < args.length; i++) {
        if (qx.lang.Type.isPromise(args[i]) || typeof args[i] == "function") {
          notify.push(args[i]);
        } else {
          parameters.push(this.serializeValue(args[i]));
        }
      }
      var data = {
        cmd: "call",
        serverId: isClass ? serverObject.classname : serverObject.getServerId(),
        methodName: methodName,
        asyncId: ++this.__asyncId,
        parameters: parameters
      };

      var methodResult = undefined;
      let methodException = null;

      const callback = (result, err) => {
        if (err) {
          methodException = err;
          notify.forEach(notify => {
            if (qx.lang.Type.isPromise(notify)) {
              notify.reject(err);
            }
          });
          return;
        }

        try {
          if (methodDef) {
            // On-Demand property accessors don't have a method
            // definition
            if (methodDef.returnArray == "wrap") {
              if (methodDef.map) {
                result = new com.zenesis.qx.remote.Map(result);
              } else if (!(result instanceof qx.data.Array)) {
                result = new qx.data.Array(result || []);
              }
            }
          }

          //console.log(`asyncId=${data.asyncId} ${serverObject.classname}.${methodName}`);

          notify.forEach(notify => {
            if (qx.lang.Type.isPromise(notify)) {
              notify.resolve(result);
            } else {
              notify.call(serverObject, result);
            }
          });

          // Store in the cache and return (not available for static methods)
          if (methodDef && methodDef.cacheResult) {
            if (!serverObject.$$proxy.cachedResults) {
              serverObject.$$proxy.cachedResults = {};
            }
            serverObject.$$proxy.cachedResults[methodName] = result;
          }

          methodResult = result;
        } catch (err) {
          methodException = err;
          notify.forEach(notify => {
            if (qx.lang.Type.isPromise(notify)) {
              notify.reject(err);
            }
          });
        }
      };

      // Add index for tracking multiple, asynchronous callbacks
      this.__asyncCallback[data.asyncId] = callback;

      // Call the server
      if (notify.length && (this.__numActiveRequests || this.__inProcessData)) {
        if (!this.__queuedServerMethods) {
          this.__queuedServerMethods = [];
        }
        this.__queuedServerMethods.push(data);
      } else {
        this._sendCommandToServer(data, notify.length != 0);
      }

      if (methodException) {
        throw methodException;
      }
      return methodResult;
    },

    /**
     * Handler for "change" event on properties with arrays wrapped by
     * qx.data.Array. For use only by Proxy.
     *
     * @param evt
     *          {Data} original "change" event for the array
     * @param serverObject
     *          {Object} the Proxy instance for a server object
     * @param propDef
     *          {Map} the property definition
     */
    onWrappedArrayChange(evt, serverObject, propDef) {
      if (propDef.readOnly) {
        return;
      }
      // Changing a property from the server
      if (serverObject === this.__setPropertyObject && propDef.name == this.__setPropertyName) {
        return;
      }
      // Server is updating the array or map
      if (this.__setPropertyObject && !this.__setPropertyName && this.__setPropertyObject === evt.getTarget()) {
        return;
      }
      var data = evt.getData();

      // The change event for qx.data.Array doesn't give enough information to
      // replicate the change, so for now we just hack it by remembering the array is
      // dirty and copying the whole thing on the next server flush
      if (!this.__dirtyArrays) {
        this.__dirtyArrays = {};
      }
      var array = evt.getTarget();
      var data = evt.getData();
      var info = this.__dirtyArrays[array.toHashCode()];
      if (!info) {
        info = this.__dirtyArrays[array.toHashCode()] = {
          array: array,
          serverObject: serverObject,
          propertyName: propDef.name
        };
      }

      if (array instanceof qx.data.Array) {
        if (!info.added) {
          info.added = [];
        }
        if (!info.removed) {
          info.removed = [];
        }
        if (data.removed) {
          data.removed.forEach(function (item) {
            if (qx.lang.Array.remove(info.added, item) === undefined) {
              info.removed.push(item);
            }
          });
        }
        if (data.added) {
          data.added.forEach(function (item) {
            if (qx.lang.Array.remove(info.removed, item) === undefined) {
              info.added.push(item);
            }
          });
        }
      } else {
        if (!info.put) {
          info.put = {};
        }
        if (!info.removed) {
          info.removed = [];
        }
        function keyToId(key) {
          if (propDef.nativeKeyType) {
            return key;
          }
          return qx.core.ObjectRegistry.toHashCode(key);
        }
        if (data.type == "put") {
          data.values.forEach(function (entry) {
            qx.lang.Array.remove(info.removed, entry.key);
            info.put[keyToId(entry.key)] = { key: entry.key, value: entry.value };
          });
        } else if (data.type == "remove") {
          data.values.forEach(function (entry) {
            delete info.put[keyToId(entry.key)];
            info.removed.push(entry.key);
          });
        }
      }
    },

    /**
     * Queues all the dirty arrays ready to flush them to the server
     */
    _queueDirtyArrays() {
      if (!this.__dirtyArrays) {
        return;
      }
      for (var arrHash in this.__dirtyArrays) {
        var info = this.__dirtyArrays[arrHash];
        var queue = {
          cmd: "edit-array",
          serverId: info.serverObject.getServerId(),
          propertyName: info.propertyName,
          type: "update"
        };

        if (info.array instanceof qx.data.Array) {
          queue.removed = this.serializeValue(info.removed);
          queue.added = this.serializeValue(info.added);
          queue.array = this.serializeValue(info.array);

          // Must be a Map
        } else {
          queue.removed = this.serializeValue(info.removed);
          queue.put = this.serializeValue(info.put);
        }
        this._queueCommandToServer(queue);
      }
      this.__dirtyArrays = null;
    },

    /**
     * Moves pending asynchronous method calls onto the queue
     */
    _queueServerMethodCalls() {
      if (!this.__queuedServerMethods) {
        return;
      }
      var t = this;
      this.__queuedServerMethods.forEach(function (data) {
        t._queueCommandToServer(data);
      });
      this.__queuedServerMethods = null;
    },

    /**
     * Mark an object as disposed on the client and needing to have the
     * corresponding server object remove from the session tracker
     *
     * @param obj
     *          {Object}
     */
    disposeServerObject(obj) {
      if (!this.__disposedServerObjects) {
        this.__disposedServerObjects = {};
      }
      var serverId = obj.getServerId();
      this.__disposedServerObjects[serverId] = obj;
    },

    /**
     * Queues all disposed client objects to notify the server
     */
    _queueDisposedServerObjects() {
      var objects = this.__disposedServerObjects;
      if (!objects) {
        return;
      }
      var arr = [];
      for (var serverId in objects) arr.push(serverId);
      if (arr.length) {
        this._queueCommandToServer({
          cmd: "dispose",
          serverIds: arr
        });
      }
      this.__disposedServerObjects = null;
    },

    /**
     * Called by Proxy when a property value is changed - do not invoke directly
     *
     * @param serverObject
     *          {Object} the server object Proxy implementation
     * @param propertyName
     *          {Object} property name
     * @param value
     *          {Object?} the value to set the property to
     */
    setPropertyValue(serverObject, propertyName, value, oldValue) {
      if (this.__inConstructor) {
        return;
      }
      let pd = qx.Class.getPropertyDefinition(serverObject.constructor, propertyName);

      if (!this.isSettingProperty(serverObject, propertyName)) {
        let annoDate = null;
        if (value) {
          if (pd.check === "Date") {
            annoDate = qx.Annotation.getProperty(serverObject.constructor, propertyName, com.zenesis.qx.remote.annotations.PropertyDate)[0] || null;
          }
        }

        // Skip changing date instances if they are equivalent
        if (value instanceof Date && oldValue instanceof Date && value.getTime() == oldValue.getTime()) {
          return;
        }

        var data = {
          cmd: "set",
          serverId: serverObject.getServerId(),
          propertyName: propertyName,
          value: this.serializeValue(value, {
            dateValue: annoDate ? annoDate.getValue() : null
          })
        };

        if (pd.sync == "queue") {
          var queue = this.__queue;
          if (queue) {
            for (var i = 0; i < queue.length; i++) {
              var qd = queue[i];
              if (qd.cmd == "set" && qd.serverId == serverObject.getServerId() && qd.propertyName == propertyName) {
                queue.splice(i, 1);
                break;
              }
            }
          }
          this._queueCommandToServer(data);
        } else this._sendCommandToServer(data);
      }

      // OnDemand properties need to have their event fired for them
      if (pd.onDemand && pd.event) {
        serverObject.fireDataEvent(pd.event, value, oldValue);
      }
    },

    /**
     * Called by Proxy when cached property value is expired; causes the expire
     * method to be queued to the server
     */
    expireProperty(serverObject, propertyName) {
      var data = {
        cmd: "expire",
        serverId: serverObject.getServerId(),
        propertyName: propertyName
      };

      this._queueCommandToServer(data);
    },

    /**
     * Called internally to set a property value that has been received from the
     * server; this must suppress property-set events being triggered
     */
    setPropertyValueFromServer(serverObject, propertyName, value) {
      var savePropertyObject = this.__setPropertyObject;
      var savePropertyName = this.__setPropertyName;
      this.__setPropertyObject = serverObject;
      this.__setPropertyName = propertyName;
      try {
        var def = qx.Class.getPropertyDefinition(serverObject.constructor, propertyName);
        var upname = qx.lang.String.firstUp(propertyName);

        // If there is a property definition, and the value is not a Proxied instance,
        // then we coerce the value; EG dates are converted from strings, scalar
        // arrays are merged into qx.data.Array, etc
        //
        // However, if it is a proxied object then we cannot merge, we must replace it;
        // this is true of arrays and maps too (those that implement Proxied) because
        // otherwise the server instance changes an object that the client is not using
        if (def && (!value || value.$$proxy === undefined)) {
          if (def.check && def.check == "Date") {
            if (value !== null) {
              if (typeof value == "string") {
                value = new Date(com.zenesis.qx.remote.ProxyManager.__DF_NO_TIME.parse(value));
              } else value = new Date(value);
            }
          } else if (def.array && def.array == "wrap") {
            if (value == null) {
              serverObject["set" + upname](null);
            } else {
              // For arrays and maps we try to not replace the object, instead
              // preferring to
              // edit the existing object if there is one.
              var current = undefined;
              if (def.onDemand === true) {
                if (serverObject.$$proxy.onDemand) {
                  current = serverObject.$$proxy.onDemand[propertyName];
                }
              } else {
                try {
                  current = serverObject["get" + upname]();
                } catch (ex) {
                  // Nothing - property not be ready yet
                }
              }
              var arrayClass = def.arrayClass ? qx.Class.getByName(def.arrayClass.className) : null; // Maps
              if (!!def.map) {
                if (current === null) {
                  //this.debug("creating Map for " + serverObject.classname + "." + propertyName + " [" + serverObject.toHashCode() + "]");
                  value = new (arrayClass || com.zenesis.qx.remote.Map)(value);
                  serverObject["set" + upname](value);
                } else {
                  current.replace(value);
                } // Arrays
              } else {
                if (current === null || current === undefined) {
                  if (value instanceof qx.data.Array) {
                    serverObject["set" + upname](value);
                  } else {
                    var dataArray = new (arrayClass || qx.data.Array)();
                    dataArray.append(value);
                    serverObject["set" + upname](dataArray);
                  }
                } else {
                  var nativeArray = value;
                  if (value instanceof qx.data.Array) {
                    nativeArray = value.toArray();
                  }
                  nativeArray.unshift(0, current.getLength());
                  current.splice.apply(current, nativeArray);
                }
              }
            }
            return;
          }
        }

        serverObject["set" + upname](value);
      } catch (e) {
        this.error("Exception during call to setPropertyValueFromServer for " + serverObject.classname + "." + propertyName + " [" + serverObject.toHashCode() + "]: " + e);
        throw e;
      } finally {
        this.__setPropertyObject = savePropertyObject;
        this.__setPropertyName = savePropertyName;
      }
    },

    /**
     * Detects whether the property is currently being set (i.e. from the
     * server)
     */
    isSettingProperty(serverObject, propertyName) {
      if (!propertyName) {
        return this.__setPropertyObject == serverObject;
      }
      return this.__setPropertyObject == serverObject && this.__setPropertyName == propertyName;
    },

    /**
     * Detects whether a response is being processed; note that this will return false when an upload
     * is being processed because the purpose is to detect whether the server is sync'ing the client,
     * whereas upload is an actual physical event created by the user
     *
     * @return {Boolean}
     */
    isInResponse() {
      return !!this.__inProcessData && !this.__inUploadReceived;
    },

    /**
     * Called by Proxy when an event listener is added; this queues a command to
     * the server
     *
     * @param serverObject
     *          {Object} the server object Proxy implementation
     * @param eventName
     *          {Object} event name
     */
    addServerListener(serverObject, eventName) {
      var className = serverObject.classname;

      // If the event is not a server event or there is already a server based listener or it's for a
      //	property then skip (property change events will be triggered automatically by Qx when the
      //	property change is synchronised)
      var eventDef = com.zenesis.qx.remote.MProxy.getEventDefinition(serverObject.constructor, eventName);
      if (!eventDef) {
        this.error("No event handlers for " + serverObject.classname);
        return;
      }
      if (!eventDef.isServer || eventDef.numListeners) {
        return;
      }
      if (eventName.length > 6 && eventName.startsWith("change") && eventName[6] == eventName[6].toUpperCase()) {
        var propName = eventName[6].toLowerCase() + eventName.substring(7);
        if (qx.Class.getPropertyDefinition(serverObject.constructor, propName)) {
          return;
        }
      }

      // Queue the addListener to the server
      eventDef.numListeners = (eventDef.numListeners || 0) + 1;
      var data = {
        cmd: "listen",
        serverId: serverObject.getServerId(),
        eventName: eventName
      };

      this._queueCommandToServer(data);
    },

    /**
     * Called by Proxy when an event listener is removed; this queues a command
     * to the server
     *
     * @param serverObject
     *          {Object} the server object Proxy implementation
     * @param eventName
     *          {Object} event name
     */
    removeServerListener(serverObject, eventName) {
      var className = serverObject.classname;

      // If the event is not a server event or it's for a property then skip (property change
      //	events will be triggered automatically by Qx when the property change is synchronised)
      var eventDef = com.zenesis.qx.remote.MProxy.getEventDefinition(serverObject.constructor, eventName);
      if (!eventDef.isServer) {
        return;
      }
      if (eventName.length > 6 && eventName.startsWith("change") && eventName[6] == eventName[6].toUpperCase()) {
        var propName = eventName[6].toLowerCase() + eventName.substring(7);
        if (qx.Class.getPropertyDefinition(serverObject.constructor, propName)) {
          return;
        }
      }

      // Queue the removeListener to the server
      eventDef.numListeners--;
      qx.core.Assert.assertTrue(eventDef.numListeners >= 0);
      var data = {
        cmd: "unlisten",
        serverId: serverObject.getServerId(),
        eventName: eventName
      };

      this._queueCommandToServer(data);
    },

    /**
     * Flushes the outbound queue, but does nothing if there is nothing to send
     * unless force is true
     *
     * @param force
     *          {Boolean?} if true, the server will be connected to regardless
     *          of whether there is anything to send, default is to only poll if
     *          there is something to send
     * @param async
     *          {Boolean?} if true, connection is async, default is false
     * @returns
     */
    flushQueue(force, async) {
      this._sendCommandToServer(
        !!force
          ? {
              cmd: "poll"
            }
          : null,
        async
      );
    },

    /**
     * Method called to send data to the server; this is to be implemented by
     * the host framework on the client.
     *
     * @param obj
     *          {Object} object to be turned into a JSON string and sent to the
     *          server
     * @param async
     *          {Boolean?} whether to make it an async call (default is synchronous)
     * @return {String} the server response
     */
    _sendCommandToServer(obj, async, suppressWarnings) {
      if (this.__shutdownPromise) {
        throw new Error("Cannot connect to server because ProxyManager is shutdown");
      }

      var startTime = new Date().getTime();

      // We must not allow recursive commands, otherwise a partially formed
      // request can be sent to the server so we just queue it instead.
      if (this.__queuingCommandsForServer) {
        if (!this.__queue) {
          this.__queue = [];
        }
        this.__queue.push(obj);
        return;
      }
      this.__queuingCommandsForServer = true;

      try {
        // Queue any client-created object which need to be sent to the server
        this._queueClientObjects();

        // Queue any dirty arrays
        this._queueDirtyArrays();

        // Queue pending async server method calls
        this._queueServerMethodCalls();

        // Queue any objects which can be removed from the server
        this._queueDisposedServerObjects();

        // Allow listeners to
        this.fireEvent("queuePending");
      } finally {
        this.__queuingCommandsForServer = false;
      }

      let asyncIds = [];
      if (obj?.asyncId) {
        asyncIds.push(obj.asyncId);
      }

      // Consume the queue
      var queue = this.__queue;
      if (queue && queue.length) {
        this.__queue = null;
        queue.forEach(obj => {
          if (obj?.asyncId) {
            asyncIds.push(obj.asyncId);
          }
        });
        if (obj) {
          queue.push(obj);
        }
        obj = queue;
      }
      if (!obj) {
        return;
      }

      // Set the data
      var text = qx.lang.Json.stringify(obj, function (key, value) {
        if (typeof this[key] === "function") {
          return this[key]();
        }
        return value;
      });
      this.__numActiveRequests++;

      let headers = {};
      if (this.__sessionId) {
        headers["X-ProxyManager-SessionId"] = this.__sessionId;
      }
      var reqIndex = this.__numberOfCalls++;
      headers["X-ProxyManager-RequestIndex"] = reqIndex;

      if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.traceOverlaps")) {
        console.log && console.log("Sending request, reqIndex=" + reqIndex + ", async=" + !!async);
        if (!async) {
          var trace = qx.dev.StackTrace.getStackTrace();
          console.log(trace.join("\n"));
        }
      }

      if (this.__preRequestCallback) {
        this.__preRequestCallback.call(this, this.__proxyIo);
      }

      if (!suppressWarnings && qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.traceRecursion")) {
        if (this.__inProcessData) {
          var trace = qx.dev.StackTrace.getStackTrace();
          console.warn(["Recursive calls to ProxyManager may cause exceptions with object references, stack trace:"].concat(trace).join("\n"));
        }
      }

      this.__proxyIo.send({
        headers: headers,
        body: text,
        async: !!async,
        proxyData: {
          async: async,
          startTime: startTime,
          postTime: new Date().getTime(),
          reqIndex: reqIndex,
          asyncIds
        },

        handler: ioData => this.__onResponseReceived(ioData)
      });
    },

    /**
     * Queues a command to the server
     */
    _queueCommandToServer(obj) {
      var queue = this.__queue;
      if (!queue) {
        this.__queue = queue = [];
      }
      this._queueClientObjects();
      queue.push(obj);
    },

    /**
     * Takes any objects created on the client which have not yet been delivered
     * to the server and adds them to the queue (i.e. processes all
     * pendingClientObject's).
     */
    _queueClientObjects() {
      if (!this.__clientObjects) {
        return;
      }
      var queue = this.__queue;
      if (!queue) {
        this.__queue = queue = [];
      }
      for (var id in this.__clientObjects) {
        var clientObject = this.__clientObjects[id];

        // Array index is set to null when received back from the server
        if (!clientObject || clientObject.getSentToServer()) {
          continue;
        }

        this._queueClientObject(id);
      }
    },

    /**
     * Queues an individual client object
     *
     * @param clientId
     *          {Integer}
     */
    _queueClientObject(clientId) {
      var clientObject = this.__clientObjects && this.__clientObjects[clientId];
      if (!clientObject) {
        throw new Error("Invalid client ID " + clientId);
      }
      if (clientObject.getSentToServer()) {
        return;
      }
      //this.debug("Queuing client object " + clientObject.toHashCode() + " client id " + clientId);

      var queue = this.__queue;
      if (!queue) {
        this.__queue = queue = [];
      }

      // Send it
      clientObject.setSentToServer();
      var className = clientObject.classname;
      var data = {
        cmd: "new",
        className: className,
        clientId: clientObject.getServerId(),
        properties: {}
      };

      var clazz = clientObject.constructor;
      while (clazz.superclass) {
        for (var propName in clazz.$$properties) {
          var pd = clazz.$$properties[propName];
          if (pd.isServer && !pd.readOnly && !pd.onDemand) {
            var value = undefined;

            // If the get method is a standard Qooxdoo get method, then we
            // access the property value directly so that we can detect
            // uninitialised property values; this allows us to not send
            // property values to the server unless necessary, so that server
            // defaults are not overridden
            var value = clientObject["$$runtime_" + propName];
            if (value === undefined) {
              value = clientObject["$$user_" + propName];
            }

            if (value !== undefined) {
              data.properties[propName] = this.serializeValue(value);
            }
          }
        }
        clazz = clazz.superclass;
      }

      queue[queue.length] = data;
    },

    /**
     * Apply callback for pollServer property
     *
     * @param value
     *          {Boolean}
     * @param oldValue
     *          {Boolean}
     */
    _applyPollServer(value, oldValue) {
      this._killPollTimer();
      if (value) {
        this._startPollTimer();
      }
    },

    /**
     * Apply callback for pollFrqeuency property
     *
     * @param value
     *          {Integer}
     * @param oldValue
     *          {Integer}
     */
    _applyPollFrequency(value, oldValue) {
      this._killPollTimer();
      this._startPollTimer();
    },

    /**
     * Kills the timer that polls the server
     */
    _killPollTimer() {
      if (this.__pollTimerId) {
        clearTimeout(this.__pollTimerId);
        this.__pollTimerId = null;
      }
    },

    /**
     * Starts the timer that will poll the server; has no effect if pollServer
     * property is false
     */
    _startPollTimer() {
      if (this.__pollTimerId) {
        this.debug("ProxyManager poll timer already exists");
        this._killPollTimer();
      }
      if (this.getPollServer()) {
        this.__pollTimerId = setTimeout(this.__onPollTimeoutBinding, this.getPollFrequency());
      }
    },

    /**
     * Callback for polling the server
     */
    __onPollTimeout() {
      // this.debug("poll");
      this.__pollTimerId = null;
      if (this.__numActiveRequests == 0) {
        this.flushQueue(true, true);
      }
    },

    /**
     * Returns the number of calls to the server
     */
    getNumberOfCalls() {
      return this.__numberOfCalls;
    },

    /**
     * Returns the server object with a given ID
     */
    getServerObject(serverId) {
      if (serverId < 0) {
        return this.__clientObjects[0 - serverId];
      }

      return this.__serverObjects[serverId];
    },

    /**
     * Called when the server reports an exception to be handled by the client;
     * stores the exception to be obtained later by calling clearException or
     * getException
     *
     * @param data
     *          {Map} the details from the server
     * @param cause
     *          {String} the cause: "property" or "function"
     */
    _handleServerException(data, cause, asyncIds) {
      // this.error("Exception from server: " + data.exceptionClass + ": " +
      // data.message);
      var ex = new Error("Exception at server: " + data.exceptionClass + ": " + data.message);
      if (asyncIds) {
        asyncIds.forEach(asyncId => this.__asyncCallback[asyncId](null, ex));
      }
      this._setException(ex);
    },

    _setException(e) {
      this.__exception = e;
      this.fireDataEvent("exception", e);
    },

    /**
     * Clears the last known exception and returns it
     *
     * @return {Error?} null if there is no exception to return
     */
    clearException() {
      var ex = this.__exception;
      this.__exception = null;
      return ex;
    },

    /**
     * Returns the last known exception
     *
     * @return {Error?} null if there is no exception to return
     */
    getException() {
      return this.__exception;
    },

    /**
     * Sets a method to be called before the request is sent
     */
    setPreRequestCallback(callback) {
      this.__preRequestCallback = callback;
    },

    /**
     * Returns the callback
     */
    getPreRequestCallback() {
      return this.__preRequestCallback;
    },

    /**
     * Utility method to describe an exception
     */
    __describeException(e) {
      var desc = "";
      if (typeof e == "string") {
        return e;
      }
      if (e.name) {
        desc = e.name;
      }
      if (e.number) {
        desc += "[#" + (e.number & 0xffff) + "]";
      }
      if (desc.length == 0) {
        desc = "typeof Exception == " + typeof e + " " + e;
      }
      desc += ": ";
      if (e.message) {
        desc += e.message;
      }
      if (e.description && e.description != e.message) {
        desc += e.description;
      }
      if (e.fileName) {
        desc += " in file " + e.fileName;
      }
      if (e.lineNumber) {
        desc += " on line " + e.lineNumber;
      }
      return desc;
    }
  },

  statics: {
    __initialised: false,
    __instance: null,
    __DF_NO_TIME: new qx.util.format.DateFormat("yyyy-MM-dd"),

    __PREFIX: "[__QOOXDOO_SERVER_OBJECTS__[",
    __SUFFIX: "]]",

    __NON_NULLABLE_DEFAULTS: {
      Boolean: false,
      Number: 0,
      Integer: 0,
      String: "",
      Date() {
        return new Date();
      }
    },

    /**
     * Formats a date as ISO
     * @param dt {Date} the date to format
     * @return {String} the formatted datetime or null is dt was null
     */
    formatISO(dt) {
      if (!dt) {
        return null;
      }
      function dp2(v) {
        if (v < 10) {
          return "0" + v;
        }
        return "" + v;
      }
      function dp3(v) {
        if (v < 10) {
          return "00" + v;
        }
        if (v < 100) {
          return "0" + v;
        }
        return "" + v;
      }
      var str =
        dt.getUTCFullYear() +
        "-" +
        dp2(dt.getUTCMonth() + 1) +
        "-" +
        dp2(dt.getUTCDate()) +
        "T" +
        dp2(dt.getUTCHours()) +
        ":" +
        dp2(dt.getUTCMinutes()) +
        ":" +
        dp2(dt.getUTCSeconds()) +
        "." +
        dp3(dt.getUTCMilliseconds()) +
        "Z";
      return str;
    },

    /**
     * Called to set the singleton global instance that will be used to send
     * data
     */
    setInstance(instance) {
      if (this.__instance && instance) {
        this.warn("Overwriting existing instance " + this.__instance + " with " + instance);
      }
      this.__instance = instance;
    },

    /**
     * Returns the current instance
     * @returns {com.zenesis.qx.remote.ProxyManager}
     */
    getInstance() {
      return this.__instance;
    },

    /**
     * Calls a static method on the server
     */
    _callServer(clazz, name, args) {
      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      var result = PM.callServerMethod(clazz, name, args);
      var ex = PM.clearException();
      if (ex) {
        throw ex;
      }
      return result;
    }
  },

  environment: {
    "com.zenesis.qx.remote.ProxyManager.trace": false,
    "com.zenesis.qx.remote.ProxyManager.traceRecursion": true,
    "com.zenesis.qx.remote.ProxyManager.traceMethodSync": false,
    "com.zenesis.qx.remote.ProxyManager.traceOnDemandSync": false,
    "com.zenesis.qx.remote.ProxyManager.traceOverlaps": false,
    "com.zenesis.qx.remote.ProxyManager.perfTrace": false,
    "com.zenesis.qx.remote.ProxyManager.traceNullBoot": false
  }
});
