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
 * Proxy class
 * 
 * Base class for proxy objects to mirror Proxied objects on the server
 */
qx.Mixin.define("com.zenesis.qx.remote.MProxy", {

  construct: function() {
    var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
    this.__serverClass = PM.getClassInfo(this.classname);
  },

  destruct: function() {
    if (this.__serverId > 0) {
      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      PM.disposeServerObject(this);
    }
  },

  events: {
    "changeServerId": "qx.event.type.Data"
  },

  members: {
    __serverId: null,
    __serverClass: null,
    // __isPending: undefined,

    /**
     * Called by the constructor to initialise the instance
     */
    initialiseProxy: function() {
      if (this.$$proxy === undefined) {
        this.$$proxy = {};
        this.setServerId(com.zenesis.qx.remote.ProxyManager.getInstance().getCurrentNewServerId());
      }
    },

    /**
     * Returns the server ID for this object
     */
    getServerId: function() {
      if (this.__serverId === undefined || this.__serverId === null) {
        var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
        this.__isPending = true;
        this.__serverId = PM.registerClientObject(this);
      }
      return this.__serverId;
    },
    
    /**
     * Detects whether the object has a server id assigned
     */
    hasServerId: function() {
      return this.__serverId !== null;
    },

    /**
     * Sets the server ID - used when the object was created on the client and
     * the server has returned a new server ID for it; to be called by
     * ProxyManager only
     */
    setServerId: function(serverId) {
      qx.core.Assert.assertTrue(this.__serverId === null || (serverId > 0 && this.__serverId < 0));
      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      delete this.__isPending;
      if (serverId > 0 && this.__serverId < 0)
        PM.unregisterClientObject(this);
      var oldValue = this.__serverId;
      this.__serverId = serverId;
      if (this.hasListener("changeServerId"))
        this.fireDataEvent("changeServerId", serverId, oldValue);
    },

    /**
     * Called by ProxyManager when a Proxy instance that was created on the
     * client has been sent to the server
     */
    setSentToServer: function() {
      qx.core.Assert.assertTrue(this.__isPending);
      delete this.__isPending;
    },

    /**
     * Detects whether this object has already been sent to the server
     */
    getSentToServer: function() {
      return !this.__isPending;
    },

    /**
     * Called by methods to invoke the server method call
     */
    _callServer: function(name, args) {
      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      var result = PM.callServerMethod(this, name, args);
      
      if (qx.core.Environment.get("com.zenesis.qx.remote.traceMethodSync")) {
        var async = args.some(function(arg) {
          return typeof arg == "function";
        });
        if (!async) {
          var trace = qx.dev.StackTrace.getStackTrace();
          this.warn(["Calling method ", this.classname, ".", name, " [" + this + "] synchronously, stack trace:"].concat(trace).join("\n"));
        }
      }
      
      var ex = PM.clearException();
      if (ex)
        throw ex;
      return result;
    },

    /**
     * Called when a property value is applied
     * 
     * @param propertyName
     *          {String} the name of the property
     * @param value
     *          {Object} the new value
     * @param oldValue
     *          {Object?} the previous value, only applicable to onDemand
     *          properties
     */
    _applyProperty: function(propertyName, value, oldValue) {
      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      var propDef = this.getPropertyDef(propertyName);
      
      if (oldValue && oldValue instanceof qx.core.Object) {
        oldValue.$$proxyOwnerDetached = this;
        delete oldValue.$$proxyOwner;
      }
      
      if (value && value instanceof qx.core.Object) {
        value.$$proxyOwner = this;
      }

      // Add change handler for arrays; note that this works for maps too
      // because they are also "wrapped"
      if (propDef.array == "wrap" && !propDef.noArrayEdits) {
        if (oldValue)
          oldValue.removeListenerById(propDef.changeListenerId);

        if (value) {
          propDef.changeListenerId = value.addListener("change", function(evt) {
            PM.onWrappedArrayChange(evt, this, propDef);
          }, this);
        } else
          propDef.changeListenerId = null;
      }

      // We mustn't tell the server anything if it does not yet know about this
      // object
      if (this.__isPending)
        return;
      PM.setPropertyValue(this, propertyName, value, oldValue);
      var ex = PM.clearException();
      if (ex)
        throw ex;
    },

    /**
     * Called when an on-demand property's get method is called
     * @param propName {String} name of the property to get
     * @param async {Boolean|Function} whether asynchronous or not
     */
    _getPropertyOnDemand: function(propName, async) {
      // Check the cache
      if (this.$$proxyUser) {
        var value = this.$$proxyUser[propName];
        if (value !== undefined) {
          if (typeof async == "function")
            async.call(this, value);
          return value;
        }
      } else
        this.$$proxyUser = {};
      
      if (qx.core.Environment.get("com.zenesis.qx.remote.traceOnDemandSync")) {
        if (async === undefined) {
          var trace = qx.dev.StackTrace.getStackTrace();
          this.warn(["Getting ondemand property " + propName + " of " + this.classname + " [" + this + "] synchronously, stack trace:"].concat(trace).join("\n"));
        }
      }

      // Call the server
      var upname = qx.lang.String.firstUp(propName);
      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      var propDef = this.getPropertyDef(propName);
      
      if (async) {
        PM.callServerMethod(this, "get" + upname, [function(value){
          var ex = PM.clearException();
          if (!ex)
            value = this.__storePropertyOnDemand(propDef, value);
          else
            value = undefined;
          if (typeof async == "function")
            async(value, ex);

        }]);
      } else {
        var value = PM.callServerMethod(this, "get" + upname, []);
        var ex = PM.clearException();
        if (ex)
          throw ex;

        // Update the cache and done
        return this.__storePropertyOnDemand(propDef, value);
      }
    },

    /**
     * Stores a value for an on-demand property, adding and removing listeners
     * as required
     * 
     * @param propDef
     *          {Map}
     * @param value
     *          {Object}
     * @returns
     */
    __storePropertyOnDemand: function(propDef, value) {
      var oldValue;
      if (this.$$proxyUser && (oldValue = this.$$proxyUser[propDef.name])) {
        if (propDef.array = "wrap" && propDef.changeListenerId) {
          oldValue.removeListenerById(propDef.changeListenerId);
          propDef.changeListenerId = null;
        }
        delete this.$$proxyUser[propDef.name];
      }
      if (value !== undefined) {
        if (value && propDef.array == "wrap") {
          if (!!propDef.map) {
            if (!(value instanceof com.zenesis.qx.remote.Map))
              value = new com.zenesis.qx.remote.Map(value);

          } else if (!(value instanceof qx.data.Array))
            value = new qx.data.Array(value);
          propDef.changeListenerId = value.addListener("change", function(evt) {
            var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
            PM.onWrappedArrayChange(evt, this, propDef);
          }, this);
        }
        this.$$proxyUser[propDef.name] = value;
      }
      return value;
    },

    /**
     * Called when an on-demand property's expire method is called
     */
    _expirePropertyOnDemand: function(propName, sendToServer) {
      if (this.$$proxyUser && this.$$proxyUser[propName])
        this.__storePropertyOnDemand(this.getPropertyDef(propName), undefined);

      if (sendToServer === undefined || sendToServer) {
        var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
        PM.expireProperty(this, propName);
      }
    },

    /**
     * Called when an on-demand property's set method is called
     */
    _setPropertyOnDemand: function(propName, value) {
      // Update the cache
      var oldValue;
      if (!this.$$proxyUser)
        this.$$proxyUser = {};
      else
        oldValue = this.$$proxyUser[propName];

      // Don't use __storePropertyOnDemand here - use _applyProperty instead
      var propDef = this.getPropertyDef(propName);
      if (propDef.array == "wrap" && !(value instanceof qx.data.Array))
        value = new qx.data.Array(value);

      this.$$proxyUser[propName] = value;
      this._applyProperty(propName, value, oldValue);
    },

    /**
     * Used as the transform property for arrays to make sure that the value is
     * always a qx.data.Array
     */
    _transformToDataArray: function(value) {
      if (!value)
        return new qx.data.Array();
      if (qx.Class.isSubClassOf(value.constructor, qx.data.Array))
        return value;
      return new qx.data.Array([ value ]);
    },

    /*
     * @Override
     */
    addListener: function(name, listener, context, capture) {
      var result = this.base(arguments, name, listener, context, capture);
      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      PM.addServerListener(this, name);
      return result;
    },

    /*
     * @Override
     */
    removeListener: function(name, listener, context, capture) {
      var existed = this.base(arguments, name, listener, context, capture);
      if (!existed)
        return;
      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      PM.removeServerListener(this, name);
      return existed;
    },

    /**
     * Gets the proxy property definition for a named property
     * 
     * @param propertyName
     *          {String} the name of the property
     * @return {Map} the property definition received from the server
     */
    getPropertyDef: function(propertyName) {
      for (var $$proxyDef = this.$$proxyDef; $$proxyDef; $$proxyDef = $$proxyDef.extend) {
        if ($$proxyDef.properties) {
          var propDef = $$proxyDef.properties[propertyName];
          if (propDef)
            return propDef;
        }
      }
      return null;
    },

    /**
     * Gets the proxy event definition for a named event
     * 
     * @param name
     *          {String} the name of the event
     * @return {Map} the event definition received from the server
     */
    getEventDef: function(name) {
      for (var $$proxyDef = this.$$proxyDef; $$proxyDef; $$proxyDef = $$proxyDef.extend) {
        if ($$proxyDef.events) {
          var eventDef = $$proxyDef.events[name];
          if (eventDef)
            return eventDef;
        }
      }
      return null;
    }
  }

});
