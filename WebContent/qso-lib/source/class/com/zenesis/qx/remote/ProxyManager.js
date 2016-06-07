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
 */
/*
 * @require(qx.core.Aspect) @ignore(auto-require)
 */
qx.Class.define("com.zenesis.qx.remote.ProxyManager", {
  extend: qx.core.Object,

  /**
   * Constructor
   * 
   * @param proxyUrl
   *          {String} the URL for communicating with the server
   * @ignore(qx.util.Json)
   */
  construct: function(proxyUrl) {
    this.base(arguments);
    this.__unprocessedResponses = [];
    if (!com.zenesis.qx.remote.ProxyManager.__initialised) {
      com.zenesis.qx.remote.ProxyManager.__initialised = true;
    }
    if (this.constructor.__instance)
      this.warn("Not setting ProxyManager instance because one is already defined");
    else
      com.zenesis.qx.remote.ProxyManager.setInstance(this);

    this.__onPollTimeoutBinding = qx.lang.Function.bind(this.__onPollTimeout, this);

    this.__serverObjects = [];
    this.setProxyUrl(proxyUrl);
  },

  properties: {
    /** URL to connect to */
    proxyUrl: {
      init: null,
      nullable: false,
      check: "String"
    },

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
    "exception": "qx.event.type.Data",

    /**
     * Fired when there is an I/O error communicating to the server; if this
     * event is not preventDefault()'d, an exception is thrown. Data is the
     * event that was returned by qx.io.Request
     */
    "ioError": "qx.event.type.Data",

    /**
     * Fired when a file upload is completed, data is the FileInfo form the
     * server
     */
    "uploadComplete": "qx.event.type.Data",

    /** Fired to queue any pending requests */
    "queuePending": "qx.event.type.Event",

    /** Fired when connected, data is the bootstrap object */
    "connected": "qx.event.type.Data",
    
    /** Fired when all outstanding requests are complete */
    "requestsComplete": "qx.event.type.Event"
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

    // Extra class information
    __classInfo: {},

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

    // Exception returned from the server, to be thrown at end of current
    // function call
    __exception: null,

    __preRequestCallback: null,

    /**
     * The Servlet at the other end is configured to return an initial object
     * for this session; it can be any arbitrary object because QSO will
     * instantiate it correctly at this end. That becomes the entry point for
     * the application from here on in.
     * 
     * Can be called multiple times, the first object is always returned.
     */
    getBootstrapObject: function() {
      if (this.__serverObjects.length)
        return this.__serverObjects[0];
      var result = null;
      var msg = {
        cmd: "bootstrap"
      };
      this._sendCommandToServer(msg);
      var ex = this.clearException();
      if (ex)
        throw ex;
      this.fireDataEvent("connected", this.__serverObjects[0]);
      return this.__serverObjects[0];
    },

    /**
     * Returns the unique session id for this client, null until first call to
     * getBootstrap()
     * 
     * @return {String}
     */
    getSessionId: function() {
      return this.__sessionId;
    },

    /**
     * Detects whether the connection has ever been made, i.e. whether
     * getBootstrapObject() has already been called.
     * 
     * @return {Boolean} true if connected
     */
    hasConnected: function() {
      return this.__serverObjects.length > 0;
    },

    /**
     * Registers a client object
     * 
     * @return {Integer} the new ID for this object
     */
    registerClientObject: function(obj) {
      qx.core.Assert.assertTrue(!obj.hasServerId());
      if (!this.__clientObjects)
        this.__clientObjects = {};
      var index = ++this.__clientObjectsLastId;
      this.__clientObjects[index] = obj;
      this.debug("Registering client object " + obj.toHashCode() + " " + obj.classname + " client id=" + index);
      obj.addListenerOnce("changeServerId", function(evt) {
        this.debug("Changing ServerId for " + evt.getTarget().toHashCode() + " from " + evt.getOldData() + " to " + evt.getData());
      });
      return 0 - index;
    },

    /**
     * Unregisters a client object
     */
    unregisterClientObject: function(obj) {
      var id = obj.getServerId() * -1;
      qx.core.Assert.assertTrue(id > 0, "Object does not have a client ID, id=" + id);
      qx.core.Assert.assertTrue(this.__clientObjects[id] === obj, "Object is not a pending client object");
      this.debug("Unregistering client object " + obj.toHashCode() + " client id=" + id);
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
    __onResponseReceived: function(evt) {
      this.__numActiveRequests--;
      var txt = evt.getContent();
      var statusCode = evt.getStatusCode();
      var req = evt.getTarget();
      
      var proxyData = req.getUserData("proxyData");
      proxyData.receivedTime = new Date().getTime();
      proxyData.txt = txt;

      var result = null;
      var reqIndex = null;

      this.__inProcessData++;
      try {
        if (statusCode == 200) {
          reqIndex = parseInt(evt.getResponseHeaders()["X-ProxyManager-RequestIndex"], 10);
          if (qx.core.Environment.get("com.zenesis.qx.remote.traceOverlaps"))
            console.log && console.log("__onResponseReceived 1: request index=" + reqIndex + ", __expectedRequestIndex=" + this.__expectedRequestIndex);
          if (qx.core.Environment.get("qx.debug")) {
            var sha = evt.getResponseHeaders()["X-ProxyManager-SHA1"];
            if (sha) {
              var digest = com.zenesis.qx.remote.Sha1.digest(txt);
              if (sha != digest) {
                throw new Error("Invalid SHA received from server, expected=" + sha + ", found=" + digest);
              }
            }
          }
          
          qx.core.Assert.assertTrue(isNaN(reqIndex) || reqIndex === proxyData.reqIndex);
          result = this.__processResponse(proxyData);
  
        } else {
          this._handleIoError(evt);
          if (typeof proxyData.async == "function")
            proxyData.async(evt);
        }
        
        return result;
      } finally {
        if (qx.core.Environment.get("com.zenesis.qx.remote.traceOverlaps"))
          console.log && console.log("__onResponseReceived 2: request index=" + reqIndex + ", __expectedRequestIndex=" + this.__expectedRequestIndex);
        this.__inProcessData--;
      }
    },
    
    __processResponse: function(proxyData) {
      function process(proxyData, outOfSequence) {
        if (!proxyData.processed) {
          proxyData.processed = true;
          
          var txt = proxyData.txt.trim();
          var result;
          if (qx.core.Environment.get("com.zenesis.qx.remote.traceOverlaps"))
            console.log("process: start: " + proxyData.reqIndex + ", expected=" + t.__expectedRequestIndex + ", outOfSequence=" + outOfSequence);
          try {
            if (qx.core.Environment.get("com.zenesis.qx.remote.trace"))
              console.log && console.log("received: txt=" + txt); // Use console.log because
                                                    // LogAppender would cause
                                                    // recursive logging
            if (txt.length) {
              var data = eval("(" + txt + ")");
              result = t._processData(data);
            }
            if (typeof proxyData.async == "function")
              proxyData.async();
  
          } catch (e) {
            t.error("Exception during receive: " + t.__describeException(e), e);
            t._setException(e);
            if (typeof proxyData.async == "function")
              proxyData.async();
  
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
        
        if (qx.core.Environment.get("com.zenesis.qx.remote.traceOverlaps"))
          console.log("process: loaded: " + proxyData.reqIndex + ", expected=" + t.__expectedRequestIndex + ", outOfSequence=" + outOfSequence);
        if (!outOfSequence) {
          t.__expectedRequestIndex = proxyData.reqIndex + 1;
          if (qx.core.Environment.get("com.zenesis.qx.remote.traceOverlaps"))
            console.log("process: updated expected=" + t.__expectedRequestIndex);
          
          for (var i = 0; i < t.__unprocessedResponses.length; i++) {
            if (t.__unprocessedResponses[i].reqIndex == t.__expectedRequestIndex) {
              var next = qx.lang.Array.removeAt(t.__unprocessedResponses, i--);
              if (qx.core.Environment.get("com.zenesis.qx.remote.traceOverlaps"))
                console.log("process: shifting, next=" + next.reqIndex + ", next.processed=" + next.processed);
              if (!next.processed) {
                process(next);
                break;
              } else {
                t.__expectedRequestIndex = next.reqIndex + 1;
              }
            }
          }
        }
        
        if (t.__unprocessedResponses.length == 0)
          t.fireEvent("requestsComplete");
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
          if (qx.core.Environment.get("com.zenesis.qx.remote.traceOverlaps"))
            console.log("queuing " + proxyData.reqIndex + ", expected=" + t.__expectedRequestIndex);
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
    getStats: function() {
      return this.__stats;
    },

    /**
     * Called when there is an error in the IO to the server
     */
    _handleIoError: function(evt) {
      var statusCode = evt.getStatusCode();
      this.error("Error returned by server, code=" + statusCode);
      if (this.fireDataEvent("ioError", evt)) {
        this._setException(new Error("statusCode=" + statusCode));
      }
    },

    /**
     * Called to handle the response from an upload
     * 
     * @lint ignoreDeprecated(eval)
     */
    uploadResponse: function(txt) {
      txt = txt.trim();
      this.__inProcessData++;
      try {
        if (qx.core.Environment.get("com.zenesis.qx.remote.trace"))
          console.log && console.log("received: txt=" + txt); // Use console.log because
                                                // LogAppender would cause
                                                // recursive logging
        if (!txt.length)
          return null;
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
    _processData: function(data) {
      var t = this;
      var result = null;
      for (var i = 0, l = data.length; i < l; i++) {
        var elem = data[i];
        var type = elem.type;

        // Init
        if (type == "bootstrap") {
          this.__sessionId = elem.data.sessionId;
          result = this.readProxyObject(elem.data.bootstrap);

          // Function return
        } else if (type == "return") {
          var asyncId = elem.data.asyncId;
          result = this.readProxyObject(elem.data.result);
          var cb = this.__asyncCallback[asyncId];
          if (cb) {
            delete this.__asyncCallback[asyncId];
            cb(result);
          }

          // Upload
        } else if (type == "upload") {
          var fileInfos = this.readProxyObject(elem.data);
          if (!result)
            result = [];
          for (var j = 0; j < fileInfos.length; j++) {
            result.push(fileInfos[j]);
            this.fireDataEvent("uploadComplete", fileInfos[j]);
          }

          // An exception was thrown
        } else if (type == "exception") {
          this._handleServerException(elem.data, "function");

          // A client-created object has been registered on the server, update
          // the IDs to server IDs
        } else if (type == "mapClientId") {
          var index = 0 - elem.data.clientId;
          var clientObject = this.__clientObjects[index];
          qx.core.Assert.assertEquals(elem.data.clientId, clientObject.getServerId());

          clientObject.setServerId(elem.data.serverId);
          this.__serverObjects[elem.data.serverId] = clientObject;

          // Now read in new/changed properties
          this.readProxyObject(elem.object);

          // Setting a property failed with an exception - change the value back
          // and handle the exception
        } else if (type == "restore") {
          var obj = this.readProxyObject(elem.object);
          try {
            var value = this.readProxyObject(elem.data.oldValue);
            this.setPropertyValueFromServer(obj, elem.name, value);
          } catch (e) {
            // Ignore it - we were only trying to recover from a server
            // exception
          }
          this._handleServerException(elem.data, "property");

          // A server property value changed, update the client
        } else if (type == "set") {
          var obj = this.readProxyObject(elem.object);
          var value = this.readProxyObject(elem.data);
          this.setPropertyValueFromServer(obj, elem.name, value);

          // An on demand server property value changed, clear the cache
        } else if (type == "expire") {
          var obj = this.readProxyObject(elem.object);
          var upname = qx.lang.String.firstUp(elem.name);
          obj["expire" + upname](false);

          // A server property value changed, update the client
        } else if (type == "edit-array") {
          (function() {
            var serverObject = t.readProxyObject(elem.object);
            var savePropertyObject = t.__setPropertyObject;
            var savePropertyName = t.__setPropertyName;
            t.__setPropertyObject = serverObject;
            t.__setPropertyName = null;
            try {
              elem.data.forEach(function(data) {
                if (data.removed)
                  data.removed.forEach(function(item) {
                    var obj = t.readProxyObject(item);
                    serverObject.remove(obj);
                  });
                if (data.added) {
                  data.added.forEach(function(item) {
                    var obj = t.readProxyObject(item);
                    serverObject.push(obj);
                  });
                }
                if (data.put) {
                  data.put.forEach(function(entry) {
                    var key = t.readProxyObject(entry.key);
                    var value = t.readProxyObject(entry.value);
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
          this.getClassOrCreate(elem.object);

          // An event was fired on the server
        } else if (type == "fire") {
          var obj = this.readProxyObject(elem.object);
          var eventData = elem.data ? this.readProxyObject(elem.data) : null;
          obj.fireDataEvent(elem.name, eventData);

          // Explicitly load a type onto the client
        } else if (type == "load-type") {
          var clazz = this.getClassOrCreate(elem.object);

          // Unknown!
        } else
          qx.core.Assert.assertTrue(false, "Unexpected type of command from server: " + type);
      }
      
      if (this.__numActiveRequests == 0 && this.__queuedServerMethods && this.__queuedServerMethods.length)
        this._sendCommandToServer(null, true, true);

      return result;
    },

    /**
     * Returns the serverid of the opbject currently being created; this is used
     * by the MProxy.initialiseProxy method, and resets after use
     */
    getCurrentNewServerId: function() {
      var result = this.__currentNewServerId;
      this.__currentNewServerId = null;
      return result;
    },

    /**
     * Reads a proxy object from the server and either creates a new object
     * (creating classes as required) or returns an existing one
     */
    readProxyObject: function(data) {
      if (typeof data == "undefined" || data === null)
        return null;
      var result = null;
      var t = this;

      function readArray(data) {
        var result;

        // Do we really have to process each element?
        var ok = true;
        for (var i = 0; ok && i < data.length; i++)
          if (typeof data[i] == "object")
            ok = false;

        // All simple values, just use the parsed data
        if (ok)
          result = data;

        // Copy values by hand
        else {
          result = [];
          for (var i = 0; i < data.length; i++)
            result[i] = t.readProxyObject(data[i]);
        }

        return result;
      }

      function readMap(data) {
        var result;

        // Do we really have to process every value?
        var ok = true;
        for ( var propName in data)
          if (typeof data[propName] == "object") {
            ok = false;
            break;
          }

        // All simple values? then just use the already parsed data
        if (ok)
          result = data;

        // Copy one by one, recursively
        else {
          /*
           * Note that ordering is not defined and if server objects with
           * recursive references are passed for the first time in a map, they
           * may fail to create.
           */
          result = {};
          for ( var propName in data) {
            var propValue = data[propName];
            if (propValue)
              propValue = t.readProxyObject(propValue);
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
          var clazz = t.getClassOrCreate(data.clazz);

          // Collect constructor args now in case they refer to a Proxied object
          // (which would cause recursion
          // and would conflict with __currentNewServerId being a single use
          // global
          var constructorArgs = undefined;
          if (data.constructorArgs)
            constructorArgs = readArray(data.constructorArgs);

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
          } else
            result = new clazz();
          t.__inConstructor = false;
          qx.core.Assert.assertEquals(serverId, result.getServerId());
          t.__serverObjects[serverId] = result;
        } else if (!result) {
          throw new Error("Cannot find serverId " + serverId + ", probable recursion in loading");
        }

        // Assign any values
        if (data.order) {
          for (var i = 0; i < data.order.length; i++) {
            var propName = data.order[i];
            var propValue = data.values[propName];
            if (propValue)
              propValue = t.readProxyObject(propValue);
            t.setPropertyValueFromServer(result, propName, propValue);
          }
        }

        /*
         * Cannot cycle through the names in "values" because the order is not
         * guaranteed, and ordering is important if we're going to be able to
         * recreate the objects because only the first reference contains the
         * class and object definition - thereafter, just a serverId is sent if
         * (data.values) { for (var propName in data.values) { var propValue =
         * data.values[propName]; if (propValue) propValue =
         * t.readProxyObject(propValue); t.setPropertyValueFromServer(result,
         * propName, propValue); } }
         */

        // Prefetched method return values
        if (data.prefetch) {
          for ( var methodName in data.prefetch) {
            var value = data.prefetch[methodName];
            if (!result.$$proxy.cachedResults)
              result.$$proxy.cachedResults = {};
            if (value)
              value = t.readProxyObject(value);
            result.$$proxy.cachedResults[methodName] = value;
          }
        }

        return result;
      }

      if (qx.lang.Type.isArray(data)) {
        result = readArray(data);

      } else if (typeof data == "object") {

        // Object - is it a server object or a map?
        if (data.serverId !== undefined) {
          result = readServerObject(data);
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
    getClassOrCreate: function(data) {
      var t = this;
      
      // If it's a string, then it's an existing class we need to create
      if (typeof data == "string") {
        if (this.__classesBeingDefined[data])
          return null;
        var clazz = eval(data);
        return clazz;
      }

      // Types are not created when encountered because thatr can lead to
      // unsolvable recursive
      // problems; definitions are queued here instead
      var deferredTypes = [];

      this.__classesBeingDefined[data.className] = true;
      try {
        // Create the JSON definition for qx.Class
        var def;
        var strConstructorCode = null;
        var strDestructorCode = "";
        if (data.isInterface)
          def = {
            members: {},
            statics: {}
          };
        else {
          strConstructorCode = "var args = qx.lang.Array.fromArguments(arguments);\n" + "args.unshift(arguments);\n"
              + "this.base.apply(this, args);\n" + "this.initialiseProxy();\n";
          def = {
            members: {},
            statics: {}
          };
          if (data.extend) {
            def.extend = this.getClassOrCreate(data.extend);
            data.extend = def.extend.prototype.$$proxyDef;
          } else {
            def.extend = qx.core.Object;
          }
          var mis = com.zenesis.qx.remote.ProxyManager.__mixins[data.className];
          if (mis) {
            mis.forEach(function(mixin) {
              if (mixin.patch) {
                if (def.patch === undefined)
                  def.patch = [ mixin.mixin ];
                else
                  def.patch.push(mixin.mixin);
              } else {
                if (def.include === undefined)
                  def.include = [ mixin.mixin ];
                else
                  def.include.push(mixin.mixin);
              }
            });
          }
        }

        // Add interfaces
        if (data.interfaces) {
          var interfaces = data.interfaces;
          for (var i = 0; i < data.interfaces.length; i++)
            interfaces[i] = this.getClassOrCreate(interfaces[i]);
          if (interfaces.length) {
            if (data.isInterface)
              def.extend = interfaces;
            else
              def.implement = interfaces;
          }
        }

        // Add methods
        if (data.methods)
          for ( var methodName in data.methods) {
            var method = data.methods[methodName];
            method.name = methodName;
            if (data.isInterface)
              def.members[methodName] = new Function('');
            else if (method.staticMethod)
              def.statics[methodName] = new Function('return com.zenesis.qx.remote.ProxyManager._callServer(' + data.className
                  + ', "' + methodName + '", qx.lang.Array.fromArguments(arguments));');
            else
              def.members[methodName] = new Function('return this._callServer("' + methodName
                  + '", qx.lang.Array.fromArguments(arguments));');

            if (method.returnType && typeof method.returnType == "object")
              deferredTypes.push(method.returnType);

            var params = method.parameters;
            if (params)
              for (var i = 0; i < params.length; i++)
                if (params[i] && typeof params[i] == "object")
                  deferredTypes.push(params[i]);
          }

        // Add properties
        var onDemandProperties = [];
        var normalProperties = [];
        if (data.properties) {
          def.properties = {};
          for ( var propName in data.properties) {
            var upname = qx.lang.String.firstUp(propName);
            var fromDef = data.properties[propName];
            fromDef.name = propName;
            
            if (fromDef.clazz && typeof fromDef.clazz == "object")
              deferredTypes.push(fromDef.clazz);

            var toDef = def.properties[propName] = {};

            // Define the property
            toDef.nullable = fromDef.nullable;
            if (!toDef.nullable && fromDef.check) {
              var defaultValue = com.zenesis.qx.remote.ProxyManager.__NON_NULLABLE_DEFAULTS[fromDef.check];
              if (defaultValue !== undefined) {
                if (typeof defaultValue == "function")
                  toDef.init = defaultValue();
                else
                  toDef.init = defaultValue;
              }
            }
            if (fromDef.event)
              toDef.event = fromDef.event;

            var arrayClassName = null;
            if ((fromDef.map || fromDef.array) && fromDef.arrayClass) {
              arrayClassName = fromDef.arrayClass.className;
              deferredTypes.push(fromDef.arrayClass);
            }

            if (fromDef.map) {
              if (fromDef.array && fromDef.array == "wrap")
                toDef.check = arrayClassName || "com.zenesis.qx.remote.Map";

              // Checks
            } else if (fromDef.check) {
              toDef.check = fromDef.check || fromDef.clazz;

              // Handle arrays
            } else if (fromDef.array) {
              if (fromDef.array == "wrap") {
                toDef.transform = "_transformToDataArray";
                toDef.check = arrayClassName || "qx.data.Array";
              } else
                toDef.check = "Array";
            }

            if ((fromDef.map || fromDef.array) && fromDef.create) {
              strConstructorCode += "this.set" + upname + "(new " + toDef.check + "());\n";
              strDestructorCode += "this.set" + upname + "(null);\n";
            }

            if (data.className.match(/AbstractIngredient2$/) && propName == "weight") {
              toDef.transform = "_transformWeight";
              def.members._transformWeight = function(value) {
                if (value === null || value === undefined)
                  this.trace("Invalid value set, value=" + value);
                return value;
              }
            }

            // Create an apply method
            var applyName = "_apply" + upname;
            toDef.apply = applyName;
            def.members[applyName] = new Function('value', 'oldValue', 'name', 'this._applyProperty("' + propName
                + '", value, oldValue, name);');

            // onDemand properties - patch it later
            if (fromDef.onDemand)
              onDemandProperties.push(fromDef);
            else
              normalProperties.push(fromDef);
          }
        }

        // Add events
        if (data.events) {
          def.events = {};
          for ( var eventName in data.events) {
            var fromDef = data.events[eventName];
            if (!fromDef.isProperty)
              def.events[eventName] = "qx.event.type.Data";
          }
        }

        // Define the class
        var clazz;
        if (data.isInterface) {
          clazz = qx.Interface.define(data.className, def) || qx.Interface.getByName(data.className);
          clazz.$$proxyDef = data;
        } else {
          var patch = def.patch;
          delete def.patch;
          def.construct = new Function(strConstructorCode);
          if (strDestructorCode)
            def.destruct = new Function(strDestructorCode);
          clazz = qx.Class.define(data.className, def);
          if (patch)
            patch.forEach(function(mixin) {
              qx.Class.patch(clazz, mixin);
            });
          if (!qx.Class.hasMixin(clazz, com.zenesis.qx.remote.MProxy))
            qx.Class.patch(clazz, com.zenesis.qx.remote.MProxy);
          clazz = qx.Class.getByName(data.className);
          clazz.prototype.$$proxyDef = data;
        }
        
        // Make sure that the class package structure is mirrored in the Packages global; this
        //  allows for source code compatibility with Rhino server apps
        var tld = data.className.match(/^[^.]+/)[0];
        if (tld) {
          if (window.Packages === undefined)
            window.Packages = {};
          window.Packages[tld] = window[tld];
        }
        this.__classInfo[data.className] = data;

        // Patch on demand properties
        onDemandProperties.forEach(function(propDef) {
          t.__addOnDemandProperty(clazz, propDef.name, propDef.readOnly || false);
        });
        normalProperties.forEach(function(propDef) {
          t.__patchNormalProperty(clazz, propDef.name);
        });
      } catch (e) {
        throw e;
      } finally {
        delete this.__classesBeingDefined[data.className];
      }

      // Create dependent classes
      for (var i = 0; i < deferredTypes.length; i++)
        this.getClassOrCreate(deferredTypes[i]);

      // Done
      return clazz;
    },

    /**
     * Adds an on-demand property
     */
    __addOnDemandProperty: function(clazz, propName, readOnly) {
      var upname = qx.lang.String.firstUp(propName);
      clazz.prototype["get" + upname] = function(async) {
        return this._getPropertyOnDemand(propName, async);
      };
      clazz.prototype["expire" + upname] = function(sendToServer) {
        return this._expirePropertyOnDemand(propName, sendToServer);
      };
      clazz.prototype["set" + upname] = function(value, async) {
        return this._setPropertyOnDemand(propName, value, async);
      };
    },
    
    /**
     * Patches a normal property so that it can take a callback as the parameter and have the
     * value passed to the callback; this is important because it allows a uniform coding pattern
     * which is the same for on demand and normal properties
     */
    __patchNormalProperty: function(clazz, name) {
      var upname = qx.lang.String.firstUp(name);
      var get = clazz.prototype["get" + upname];
      clazz.prototype["get" + upname] = function(cb) {
        // qx.core.Property.executeOptimisedSetter changes the implementation of the 
        //  get method the first time it is called; we detect that and swap our overridden
        //  method back in
        var currentGet = clazz.prototype["get" + upname];
        var value = get.call(this);
        var newGet = clazz.prototype["get" + upname];
        if (newGet != currentGet) {
          get = newGet;
          clazz.prototype["get" + upname] = currentGet;
        }
        if (typeof cb == "function")
          cb(value);
        return value;
      };
    },

    /**
     * Returns the class definition received from the server for a named class
     */
    getClassInfo: function(className) {
      var info = this.__classInfo[className];
      qx.core.Assert.assertNotNull(info);
      return info;
    },

    /**
     * Serialises a value for sending to the server
     */
    serializeValue: function(value) {
      if (!value)
        return value;
      var to = typeof value;
      if ([ "boolean", "number", "string" ].indexOf(to) > -1)
        return value;
      if ([ "function", "symbol" ].indexOf(to) > -1) {
        this.error("Cannot serialize an object of type " + to + " to the server");
        return null;
      }
      
      if (value instanceof qx.core.Object && value.isDisposed()) {
        if (!value.$$proxy || (this.__disposedServerObjects && !this.__disposedServerObjects[value.getServerId()]))
          throw new Error("Cannot serialise " + value.classname + " [" + value.toHashCode() + "] because it is disposed, object=" + value);
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
          result[key] = this.serializeValue(value.get(key));
        }
        return result;
      }

      if (value instanceof qx.data.Array)
        value = value.toArray();

      if (qx.lang.Type.isArray(value)) {
        var send = [];
        for (var j = 0; j < value.length; j++) {
          if (typeof value[j] === "undefined" || value[j] === null)
            send[j] = null;
          else
            send[j] = this.serializeValue(value[j]);
        }
        return send;
      }

      if (qx.lang.Type.isDate(value)) {
        return value.getTime();
      }

      if (value.$$proxyDef) {
      //-- this doesn't work in older versions of Qx when using build target
      //if (qx.Class.hasMixin(value.constructor, com.zenesis.qx.remote.MProxy)) {
        var id = value.getServerId();
        if (id < 0)
          this._queueClientObject(0 - id);
        return value.getServerId();
      }

      if (value instanceof qx.core.Object) {
        this.error("Cannot serialize a Qooxdoo object to the server unless it implements com.zenesis.qx.remote.Proxied");
        return null;
      }

      // Assume it's an ordinary map object; deliberately not using
      // hasOwnProperty()
      var result = {};
      for ( var name in value) {
        result[name] = this.serializeValue(value[name]);
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
    callServerMethod: function(serverObject, methodName, args) {
      var isClass = serverObject && serverObject.$$type !== undefined && serverObject.$$type === "Class";
      var methodDef;
      if (isClass) {
        var cinfo = this.getClassInfo(serverObject.classname);
        methodDef = cinfo.methods[methodName];
      } else {
        if (serverObject.isDisposed())
            throw new Error("Cannot call method " + serverObject.classname + "." + methodName + " on [" + serverObject.toHashCode() + "] because it is disposed, object=" + serverObject);
        
        methodDef = this._getMethodDef(serverObject, methodName);
        // Can we get it from the cache?
        if (methodDef && methodDef.cacheResult && serverObject.$$proxy.cachedResults
            && serverObject.$$proxy.cachedResults[methodName])
          return serverObject.$$proxy.cachedResults[methodName];
      }

      // Serialise the request
      var parameters = [];
      var notify = [];
      for (var i = 0; i < args.length; i++) {
        if (typeof args[i] == "function")
          notify.push(args[i]);
        else
          parameters.push(this.serializeValue(args[i]));
      }
      var data = {
        cmd: "call",
        serverId: isClass ? serverObject.classname : serverObject.getServerId(),
        methodName: methodName,
        asyncId: ++this.__asyncId,
        parameters: parameters
      };

      var methodResult = undefined;

      // Add index for tracking multiple, asynchronous callbacks
      this.__asyncCallback[data.asyncId] = function(result) {
        if (!this.getException()) {
          if (methodDef) {// On-Demand property accessors don't have a method
            // definition
            if (methodDef.returnArray == "wrap") {
              if (!!methodDef.map)
                result = new com.zenesis.qx.remote.Map(result);
              else
                result = new qx.data.Array(result || []);
            }
          }
        }

        for (var i = 0; i < notify.length; i++)
          notify[i].call(serverObject, result);

        // Store in the cache and return (not available for static methods)
        if (methodDef && methodDef.cacheResult) {
          if (!serverObject.$$proxy.cachedResults)
            serverObject.$$proxy.cachedResults = {};
          serverObject.$$proxy.cachedResults[methodName] = result;
        }

        methodResult = result;
      }.bind(this);

      // Call the server
      if (notify.length && (this.__numActiveRequests || this.__inProcessData)) {
        if (!this.__queuedServerMethods)
          this.__queuedServerMethods = [];
        this.__queuedServerMethods.push(data);
        
      } else {
        this._sendCommandToServer(data, notify.length != 0);
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
    onWrappedArrayChange: function(evt, serverObject, propDef) {
      if (propDef.readOnly)
        return;
      // Changing a property from the server
      if (serverObject === this.__setPropertyObject && propDef.name == this.__setPropertyName)
        return;
      // Server is updating the array or map
      if (this.__setPropertyObject && !this.__setPropertyName && this.__setPropertyObject === evt.getTarget())
        return;
      var data = evt.getData();

      // The change event for qx.data.Array doesn't give enough information to
      // replicate
      // the change, so for now we just hack it by remembering the array is
      // dirty and
      // copying the whole thing on the next server flush
      if (!this.__dirtyArrays)
        this.__dirtyArrays = {};
      var array = evt.getTarget();
      var data = evt.getData();
      var info = this.__dirtyArrays[array.toHashCode()];
      if (!info)
        info = this.__dirtyArrays[array.toHashCode()] = {
          array: array,
          serverObject: serverObject,
          propertyName: propDef.name
        };
      if (array instanceof qx.data.Array) {
        if (!info.added)
          info.added = [];
        if (!info.removed)
          info.removed = [];
        if (data.removed)
          data.removed.forEach(function(item) {
            if (qx.lang.Array.remove(data.added, item) === undefined) {
              info.removed.push(item);
            }
          });
        if (data.added)
          data.added.forEach(function(item) {
            if (qx.lang.Array.remove(info.removed, item) === undefined) {
              info.added.push(item);
            }
          });
      } else {
        if (!info.put)
          info.put = {};
        if (!info.removed)
          info.removed = [];
        if (data.type == "put") {
          data.values.forEach(function(entry) {
            qx.lang.Array.remove(info.removed, entry.key);
            info.put[entry.key] = entry.value;
          });
        } else if (data.type == "remove") {
          data.values.forEach(function(entry) {
            var key = entry.key;
            if (info.put[key] !== undefined) {
              delete info.put[key];
            }
            info.removed.push(key);
          });
        }
      }
    },

    /**
     * Queues all the dirty arrays ready to flush them to the server
     */
    _queueDirtyArrays: function() {
      if (!this.__dirtyArrays)
        return;
      for ( var arrHash in this.__dirtyArrays) {
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
    _queueServerMethodCalls: function() {
      if (!this.__queuedServerMethods)
        return;
      var t = this;
      this.__queuedServerMethods.forEach(function(data) {
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
    disposeServerObject: function(obj) {
      if (!this.__disposedServerObjects)
        this.__disposedServerObjects = {};
      var serverId = obj.getServerId();
      this.__disposedServerObjects[serverId] = obj;
    },

    /**
     * Queues all disposed client objects to notify the server
     */
    _queueDisposedServerObjects: function() {
      var objects = this.__disposedServerObjects;
      if (!objects)
        return;
      var arr = [];
      for ( var serverId in objects)
        arr.push(serverId);
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
    setPropertyValue: function(serverObject, propertyName, value, oldValue) {
      if (this.__inConstructor)
        return;
      var pd = serverObject.getPropertyDef(propertyName);

      if (!this.isSettingProperty(serverObject, propertyName)) {
        // Skip changing date instances if they are equivalent
        if (value instanceof Date && oldValue instanceof Date && value.getTime() == oldValue.getTime())
          return;

        var data = {
          cmd: "set",
          serverId: serverObject.getServerId(),
          propertyName: propertyName,
          value: this.serializeValue(value)
        };
        if (serverObject.classname.match(/Ingredient2$/) && propertyName == "weight" && 
            (value === null || data.value === null)) {
          this.trace("Invalid property value for weight, value=" + value + ", data.value=" + data.value);
          debugger;
        }
        var def = this.__classInfo[serverObject.classname];

        if (pd.sync == "queue") {
          var queue = this.__queue;
          if (queue)
            for (var i = 0; i < queue.length; i++) {
              var qd = queue[i];
              if (qd.cmd == "set" && qd.serverId == serverObject.getServerId() && qd.propertyName == propertyName) {
                queue.splice(i, 1);
                break;
              }
            }
          this._queueCommandToServer(data);
        } else
          this._sendCommandToServer(data);
      }

      // OnDemand properties need to have their event fired for them
      if (pd.onDemand && pd.event)
        serverObject.fireDataEvent(pd.event, value, oldValue);
    },

    /**
     * Called by Proxy when cached property value is expired; causes the expire
     * method to be queued to the server
     */
    expireProperty: function(serverObject, propertyName) {
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
    setPropertyValueFromServer: function(serverObject, propertyName, value) {
      var savePropertyObject = this.__setPropertyObject;
      var savePropertyName = this.__setPropertyName;
      this.__setPropertyObject = serverObject;
      this.__setPropertyName = propertyName;
      try {
        var def = serverObject.getPropertyDef(propertyName);
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
            value = value !== null ? new Date(value) : null;

          } else if (def.array && def.array == "wrap") {
            if (value == null) {
              serverObject["set" + upname](null);
            } else {

              // For arrays and maps we try to not replace the object, instead
              // preferring to
              // edit the existing object if there is one.
              var current = undefined;
              if (def.onDemand === true) {
                if (serverObject.$$proxyUser)
                  current = serverObject.$$proxyUser[propertyName];
              } else {
                try {
                  current = serverObject["get" + upname]();
                } catch (ex) {
                  // Nothing - property not be ready yet
                }
              }

              var arrayClass = def.arrayClass ? qx.Class.getByName(def.arrayClass.className) : null;

              // Maps
              if (!!def.map) {
                if (current === null) {
                  //this.debug("creating Map for " + serverObject.classname + "." + propertyName + " [" + serverObject.toHashCode() + "]");
                  value = new (arrayClass || com.zenesis.qx.remote.Map)(value);
                  serverObject["set" + upname](value);
                } else {
                  current.replaceAll(value);
                }

                // Arrays
              } else {
                if (current === null || current === undefined) {
                  if (value instanceof qx.data.Array)
                    serverObject["set" + upname](value);
                  else {
                    var dataArray = new (arrayClass || qx.data.Array)();
                    dataArray.append(value);
                    serverObject["set" + upname](dataArray);
                  }
                } else {
                  var nativeArray = value;
                  if (value instanceof qx.data.Array)
                    nativeArray = value.toArray();
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
        this.error(e);
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
    isSettingProperty: function(serverObject, propertyName) {
      return this.__setPropertyObject == serverObject && this.__setPropertyName == propertyName;
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
    addServerListener: function(serverObject, eventName) {
      var className = serverObject.classname;
      var def = this.__classInfo[className];
      var event = serverObject.getEventDef(eventName);

      // If the event is not a server event, or it's for a property, or there is
      // already
      // a server based listener, then skip (property change events will be
      // triggered
      // automatically by Qx when the property change is synchronised)
      if (!event || event.isProperty || event.numListeners)
        return;

      // Queue the addListener to the server
      event.numListeners = (event.numListeners || 0) + 1;
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
    removeServerListener: function(serverObject, eventName) {
      var className = serverObject.classname;
      var def = this.__classInfo[className];
      var event = def.events && def.events[eventName];

      // If the event is not a server event or it's for a property then skip
      // (property change
      // events will be triggered automatically by Qx when the property change
      // is synchronised)
      if (!event || event.isProperty)
        return;

      // Queue the removeListener to the server
      event.numListeners--;
      qx.core.Assert.assertTrue(event.numListeners >= 0);
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
    flushQueue: function(force, async) {
      this._sendCommandToServer(!!force ? {
        cmd: "poll"
      } : null, async);
    },

    /**
     * Method called to send data to the server; this is to be implemented by
     * the host framework on the client.
     * 
     * @param obj
     *          {Object} object to be turned into a JSON string and sent to the
     *          server
     * @param aync
     *          {Boolean?} whether to make it an async call (default is synchronous)
     * @return {String} the server response
     */
    _sendCommandToServer: function(obj, async, suppressWarnings) {
      var startTime = new Date().getTime();
      
      // We must not allow recursive commands, otherwise a partially formed
      // request can be sent to the server so we just queue it instead.
      if (this.__queuingCommandsForServer) {
        if (!this.__queue)
          this.__queue = [];
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

      // Consume the queue
      var queue = this.__queue;
      if (queue && queue.length) {
        this.__queue = null;
        if (obj)
          queue.push(obj);
        obj = queue;
      }
      if (!obj)
        return;

      // Set the data
      var text = qx.lang.Json.stringify(obj);
      this.__numActiveRequests++;
      var req = new qx.io.remote.Request(this.getProxyUrl(), "POST", "text/plain");
      req.setAsynchronous(!!async);
      req.setTimeout(this.getTimeout());
      if (this.__sessionId)
        req.setRequestHeader("X-ProxyManager-SessionId", this.__sessionId);
      if (qx.core.Environment.get("qx.debug")) {
        req.setRequestHeader("X-ProxyManager-SHA1", com.zenesis.qx.remote.Sha1.digest(text));
      }
      req.setRequestHeader("X-ProxyManager-ClientTime", new Date().getTime());
      var reqIndex = this.__numberOfCalls++;
      req.setRequestHeader("X-ProxyManager-RequestIndex", reqIndex);
      req.setData(text);

      // You must set the character encoding explicitly; even if the page is
      // served as UTF8 and everything else is
      // UTF8, not specifying will lead to the server screwing up decoding
      // (presumably the default charset for the
      // JVM).
      var charset = document.characterSet || document.charset || "UTF-8";
      req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=" + charset);

      // Send it
      if (qx.core.Environment.get("com.zenesis.qx.remote.trace")) {
        // Use console.log because LogAppender would cause recursive logging
        console.log && console.log("Sending to server: " + text); 
      }

      req.addListener("completed", this.__onResponseReceived, this);
      req.addListener("failed", this.__onResponseReceived, this);
      req.addListener("timeout", this.__onResponseReceived, this);
      req.setUserData("proxyData", { 
        async: async,
        startTime: startTime,
        postTime: new Date().getTime(),
        reqIndex: reqIndex
      });
      if (qx.core.Environment.get("com.zenesis.qx.remote.traceOverlaps")) {
        console.log && console.log("Sending request, reqIndex=" + reqIndex + ", async=" + (!!async));
        if (!async) {
          var trace = qx.dev.StackTrace.getStackTrace();
          console.log(trace.join("\n"));
        }
      }
      if (this.__preRequestCallback)
        this.__preRequestCallback.call(this, req);
      if (!suppressWarnings && qx.core.Environment.get("com.zenesis.qx.remote.traceRecursion")) {
        if (this.__inProcessData) {
          var trace = qx.dev.StackTrace.getStackTrace();
          this.warn(["Recursive calls to ProxyManager may cause exceptions with object references, stack trace:"].concat(trace).join("\n"));
        }
      }
      req.send();
    },

    /**
     * Queues a command to the server
     */
    _queueCommandToServer: function(obj) {
      var queue = this.__queue;
      if (!queue)
        this.__queue = queue = [];
      this._queueClientObjects();
      queue.push(obj);
    },

    /**
     * Takes any objects created on the client which have not yet been delivered
     * to the server and adds them to the queue (i.e. processes all
     * pendingClientObject's).
     */
    _queueClientObjects: function() {
      if (!this.__clientObjects)
        return;
      var queue = this.__queue;
      if (!queue)
        this.__queue = queue = [];
      for ( var id in this.__clientObjects) {
        var clientObject = this.__clientObjects[id];

        // Array index is set to null when received back from the server
        if (!clientObject || clientObject.getSentToServer())
          continue;

        this._queueClientObject(id);
      }
    },

    /**
     * Queues an individual client object
     * 
     * @param clientId
     *          {Integer}
     */
    _queueClientObject: function(clientId) {
      var clientObject = this.__clientObjects && this.__clientObjects[clientId];
      if (!clientObject)
        throw new Error("Invalid client ID " + clientId);
      if (clientObject.getSentToServer())
        return;
      this.debug("Queuing client object " + clientObject.toHashCode() + " client id " + clientId);

      var queue = this.__queue;
      if (!queue)
        this.__queue = queue = [];

      // Send it
      clientObject.setSentToServer();
      var className = clientObject.classname;
      var def = this.__classInfo[className];
      var data = {
        cmd: "new",
        className: className,
        clientId: clientObject.getServerId(),
        properties: {}
      };
      for (; def; def = def.extend) {
        if (def.properties)
          for ( var propName in def.properties) {
            var pd = def.properties[propName];
            if (!pd.readOnly && !pd.onDemand) {
              var value = undefined;

              // If the get method is a standard Qooxdoo get method, then we
              // access the property
              // value directly so that we can detect uninitialised property
              // values; this allows
              // to not send property values to the server unless necessary, so
              // that server
              // defaults are not overridden
              var value = clientObject["$$runtime_" + propName];
              if (value === undefined)
                value = clientObject["$$user_" + propName];

              if (value !== undefined)
                data.properties[propName] = this.serializeValue(value);
            }
          }
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
    _applyPollServer: function(value, oldValue) {
      this._killPollTimer();
      if (value)
        this._startPollTimer();
    },

    /**
     * Apply callback for pollFrqeuency property
     * 
     * @param value
     *          {Integer}
     * @param oldValue
     *          {Integer}
     */
    _applyPollFrequency: function(value, oldValue) {
      this._killPollTimer();
      this._startPollTimer();
    },

    /**
     * Kills the timer that polls the server
     */
    _killPollTimer: function() {
      if (this.__pollTimerId) {
        clearTimeout(this.__pollTimerId);
        this.__pollTimerId = null;
      }
    },

    /**
     * Starts the timer that will poll the server; has no effect if pollServer
     * property is false
     */
    _startPollTimer: function() {
      if (this.__pollTimerId) {
        this.debug("ProxyManager poll timer already exists");
        this._killPollTimer();
      }
      if (this.getPollServer())
        this.__pollTimerId = setTimeout(this.__onPollTimeoutBinding, this.getPollFrequency());
    },

    /**
     * Callback for polling the server
     */
    __onPollTimeout: function() {
      // this.debug("poll");
      this.__pollTimerId = null;
      if (this.__numActiveRequests == 0)
        this.flushQueue(true, true);
    },
    
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
     * Returns the number of calls to the server
     */
    getNumberOfCalls: function() {
      return this.__numberOfCalls;
    },

    /**
     * Returns the server object with a given ID
     */
    getServerObject: function(serverId) {
      if (serverId < 0)
        return this.__clientObjects[0 - serverId];

      return this.__serverObjects[serverId];
    },

    /**
     * Returns the proxy definition for a named method
     * 
     * @param serverObject
     *          {Object} the object to get the method from
     * @param methodName
     *          {String} the name of the method
     */
    _getMethodDef: function(serverObject, methodName) {
      for (var def = serverObject.$$proxyDef; def; def = def.extend) {
        if (def.methods) {
          var methodDef = def.methods[methodName];
          if (methodDef)
            return methodDef;
        }
      }
      return null;
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
    _handleServerException: function(data, cause) {
      // this.error("Exception from server: " + data.exceptionClass + ": " +
      // data.message);
      var ex = new Error("Exception at server: " + cause + " " + data);
      ex.serverData = data;
      ex.serverCause = cause;
      this._setException(new Error("Exception at server: " + data.exceptionClass + ": " + data.message));
    },

    _setException: function(e) {
      this.__exception = e;
      this.fireDataEvent("exception", e);
    },

    /**
     * Clears the last known exception and returns it
     * 
     * @return {Error?} null if there is no exception to return
     */
    clearException: function() {
      var ex = this.__exception;
      this.__exception = null;
      return ex;
    },

    /**
     * Returns the last known exception
     * 
     * @return {Error?} null if there is no exception to return
     */
    getException: function() {
      return this.__exception;
    },

    /**
     * Sets a method to be called before the request is sent
     */
    setPreRequestCallback: function(callback) {
      this.__preRequestCallback = callback;
    },

    /**
     * Returns the callback
     */
    getPreRequestCallback: function() {
      return this.__preRequestCallback;
    },

    /**
     * Utility method to describe an exception
     */
    __describeException: function(e) {
      var desc = "";
      if (typeof e == "string")
        return e;
      if (e.name)
        desc = e.name;
      if (e.number)
        desc += "[#" + (e.number & 0xFFFF) + "]";
      if (desc.length == 0)
        desc = "typeof Exception == " + (typeof e) + " " + e;
      desc += ": ";
      if (e.message)
        desc += e.message;
      if (e.description && e.description != e.message)
        desc += e.description;
      if (e.fileName)
        desc += " in file " + e.fileName;
      if (e.lineNumber)
        desc += " on line " + e.lineNumber;
      return desc;
    }
  },

  statics: {
    __initialised: false,
    __instance: null,

    __NON_NULLABLE_DEFAULTS: {
      "Boolean": false,
      "Number": 0,
      "Integer": 0,
      "String": "",
      "Date": function() {
        return new Date();
      }
    },

    /**
     * Called to set the singleton global instance that will be used to send
     * data
     */
    setInstance: function(instance) {
      if (this.__instance && instance)
        this.warn("Overwriting existing instance " + this.__instance + " with " + instance);
      this.__instance = instance;
    },

    /**
     * Returns the current instance
     */
    getInstance: function() {
      return this.__instance;
    },

    // Client mixins to add to server classes
    __mixins: {},

    /**
     * Adds a mixin for a server class
     * 
     * @param className
     *          {String} the name of the server class
     * @param mixin
     *          {Mixin} the mixin to add
     * @param patch
     *          {Boolean?} if true patch is used instead of include, default
     *          false
     */
    addMixin: function(className, mixin, patch) {
      if (qx.Class.getByName(className) != null)
        throw new Error("Cannot add mixins for class " + className + " because the class has already been loaded");
      var mis = this.__mixins[className];
      if (mis === undefined)
        mis = this.__mixins[className] = [];
      mis.push({
        mixin: mixin,
        patch: patch
      });
    },

    /**
     * Calls a static method on the server
     */
    _callServer: function(clazz, name, args) {
      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      var result = PM.callServerMethod(clazz, name, args);
      var ex = PM.clearException();
      if (ex)
        throw ex;
      return result;
    }

  },

  environment: {
    "com.zenesis.qx.remote.trace": false,
    "com.zenesis.qx.remote.traceRecursion": true,
    "com.zenesis.qx.remote.traceMethodSync": false,
    "com.zenesis.qx.remote.traceOnDemandSync": false,
    "com.zenesis.qx.remote.traceOverlaps": false
  }
});