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

  destruct: function() {
    if (this.__serverId > 0) {
      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      PM.disposeServerObject(this);
    }
  },

  events: {
    "changeServerId": "qx.event.type.Data",
    "sentToServer": "qx.event.type.Event"
  },

  members: {
    __serverId: null,
    // __isPending: undefined,

    /**
     * Called by the constructor to initialise the instance
     */
    initialiseProxy: function() {
      if (this.$$proxy === undefined) {
        this.$$proxy = {};
        this.setServerId(com.zenesis.qx.remote.ProxyManager.getInstance().getCurrentNewServerId());
      }
      if (this.__initialisers !== undefined) {
        this.__initialisers.forEach(function(cb) {
          cb.call(this, this);
        });
        delete this.__initialisers;
      }
    },
    
    proxyAddInitialiser: function(cb, context) {
      if (context)
        cb = qx.lang.Function.bind(cb, context);
      if (this.__serverId !== null) {
        cb.call(this, this);
      } else {
        if (this.__initialisers === undefined)
          this.__initialisers = [];
        this.__initialisers.push(cb);
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
      this.fireEvent("sentToServer");
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
      if (qx.core.Environment.get("com.zenesis.qx.remote.traceMethodSync")) {
        var async = args.some(function(arg) {
          return typeof arg == "function";
        });
        if (!async) {
          var trace = qx.dev.StackTrace.getStackTrace();
          qx.log.Logger.warn(com.zenesis.qx.remote.MProxy, `Calling method ${this.classname}.${name} [${this}] synchronously, stack trace:${trace}\n`);
        }
      }

      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      var result = PM.callServerMethod(this, name, args);
      
      var ex = PM.clearException();
      if (ex)
        throw ex;
      return result;
    },
    
    _transformProperty(propertyName, value, oldValue) {
      // Skip changing date instances if they are equivalent
      if (value instanceof Date && oldValue instanceof Date && value.getTime() == oldValue.getTime())
        return oldValue;
      
      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      var pd = qx.Class.getPropertyDefinition(this.constructor, propertyName);
      
      if (value && pd.check === "Date") {
        qx.Annotation.getProperty(this.constructor, propertyName, com.zenesis.qx.remote.annotations.PropertyDate)
          .forEach(annoDate => {
            if (annoDate.getValue() == "date") {
              if (annoDate.isZeroTime()) {
                if (value.getHours() != 0 || value.getMinutes() != 0 || value.getSeconds() != 0 || value.getMilliseconds() != 0) {
                  value = new Date(value.getFullYear(), value.getMonth(), value.getDate());
                }
              } else {
                if (value.getHours() != 23 || value.getMinutes() != 59 || value.getSeconds() != 59 || value.getMilliseconds() != 0) {
                  value = new Date(value.getFullYear(), value.getMonth(), value.getDate(), 23, 59, 59, 0);
                }
              }
            }
          });
      }
      
      return value;
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
      var propDef = qx.Class.getPropertyDefinition(this.constructor, propertyName);
      
      if (oldValue && oldValue instanceof qx.core.Object) {
        oldValue.$$proxyOwnerDetached = this;
        delete oldValue.$$proxyOwner;
      }
      
      if (value && value instanceof qx.core.Object) {
        value.$$proxyOwner = this;
      }

      // Add change handler for arrays; note that this works for maps too
      // because they are also "wrapped"
      if (propDef.array == "wrap") {
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
    
    _getPropertyOnDemandAsync: function(propName) {
      if (this.$$proxy.onDemandPromise === undefined)
        this.$$proxy.onDemandPromise = {};
      var promise = this.$$proxy.onDemandPromise[propName];
      if (promise)
        return promise;
      
      return this.$$proxy.onDemandPromise[propName] = new qx.Promise(function(resolve) {
        this._getPropertyOnDemand(propName, resolve);
      }, this);
    },

    /**
     * Called when an on-demand property's get method is called
     * @param propName {String} name of the property to get
     * @param async {Boolean|Function} whether asynchronous or not
     */
    _getPropertyOnDemand: function(propName, async) {
      // Check the cache
      if (this.$$proxy.onDemand) {
        var value = this.$$proxy.onDemand[propName];
        if (value !== undefined) {
          if (typeof async == "function")
            async.call(this, value);
          return value;
        }
      } else
        this.$$proxy.onDemand = {};
      
      if (qx.core.Environment.get("com.zenesis.qx.remote.traceOnDemandSync")) {
        if (async === undefined) {
          var trace = qx.dev.StackTrace.getStackTrace();
          qx.log.Logger.warn(com.zenesis.qx.remote.MProxy, `Getting ondemand property ${propName} of ${this.classname} [${this}] synchronously, stack trace:${trace}\n`);
        }
      }

      // Call the server
      var upname = qx.lang.String.firstUp(propName);
      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      var propDef = qx.Class.getPropertyDefinition(this.constructor, propName);
      
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
      if (this.$$proxy.onDemand && (oldValue = this.$$proxy.onDemand[propDef.name])) {
        if (propDef.array == "wrap" && propDef.changeListenerId) {
          oldValue.removeListenerById(propDef.changeListenerId);
          propDef.changeListenerId = null;
        }
        delete this.$$proxy.onDemand[propDef.name];
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
        this.$$proxy.onDemand[propDef.name] = value;
      }
      return value;
    },

    /**
     * Called when an on-demand property's expire method is called
     */
    _expirePropertyOnDemand: function(propName, sendToServer) {
      if (this.$$proxy.onDemand && this.$$proxy.onDemand[propName]) {
        var propDef = qx.Class.getPropertyDefinition(this.constructor, propName);
        this.__storePropertyOnDemand(propDef, undefined);
      }

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
      if (!this.$$proxy.onDemand)
        this.$$proxy.onDemand = {};
      else
        oldValue = this.$$proxy.onDemand[propName];

      // Don't use __storePropertyOnDemand here - use _applyProperty instead
      var propDef = qx.Class.getPropertyDefinition(this.constructor, propName);
      if (propDef.array == "wrap" && !(value instanceof qx.data.Array))
        value = new qx.data.Array(value);

      this.$$proxy.onDemand[propName] = value;
      this._applyProperty(propName, value, oldValue);
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
    }
  },
  
  statics: {
    
    /**
     * Patches a normal property so that it can take a callback as the parameter and have the
     * value passed to the callback; this is important because it allows a uniform coding pattern
     * which is the same for on demand and normal properties
     */
    patchNormalProperty: function(clazz, name) {
      var upname = qx.lang.String.firstUp(name);
      var get = clazz.prototype["get" + upname];
      var fn = clazz.prototype["get" + upname] = function(cb) {
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
      if (get.$$install)
        fn.$$install = get.$$install;
    },
    
    /**
     * Adds an on-demand property
     */
    addOnDemandProperty: function(clazz, propName, readOnly) {
      var upname = qx.lang.String.firstUp(propName);
      clazz.prototype["get" + upname] = new Function("async", "return this._getPropertyOnDemand('" + propName + "', async);");
      clazz.prototype["expire" + upname] = new Function("sendToServer", "return this._expirePropertyOnDemand('" + propName + "', sendToServer);");
      clazz.prototype["set" + upname] = new Function("value", "async", "return this._setPropertyOnDemand('" + propName + "', value, async);");
      clazz.prototype["get" + upname + "Async"] = new Function(
          "return new qx.Promise(function(resolve) {" +
            "this._getPropertyOnDemand('" + propName + "', function(result) {" +
              "resolve(result);" + 
            "});" +
          "}, this);");
    },

    deferredClassInitialisation: function(clazz) {
    	// Make sure it has this mixin - but check first because a super class may have already
    	//	included it
      if (!qx.Class.hasMixin(clazz, com.zenesis.qx.remote.MProxy)) {
    	  qx.Class.patch(clazz, com.zenesis.qx.remote.MProxy);
    	  clazz = qx.Class.getByName(clazz.classname);
      }
    	
    	for (var name in clazz.$$properties) {
    		var def = clazz.$$properties[name];
    		if (def.isServer) {
    			if (def.onDemand)
    				this.addOnDemandProperty(clazz, name, !!def.readOnly);
    			else
    				this.patchNormalProperty(clazz, name);
    		}
    	}
    	
    	// Update Packages global - this is for compatibility with Rhino server apps
      var tld = clazz.classname.match(/^[^.]+/)[0];
      if (tld) {
        if (window.Packages === undefined)
          window.Packages = {};
        window.Packages[tld] = window[tld];
      }
      
      return clazz;
    },
    
    /**
     * Used as the transform property for arrays to make sure that the value is
     * always a qx.data.Array or derived
     */
    transformToDataArray: function(value, clazz) {
      if (!value)
        return new clazz();
      if (qx.Class.isSubClassOf(value.constructor, clazz))
        return value;
      var result = new clazz();
      if (value instanceof qx.data.Array || qx.lang.Type.isArray(value))
        result.append(value);
      else
        result.push(value);
      return result;
    },
    
    getEventDefinition: function(clazz, name) {
    	while (clazz.superclass) {
    		if (clazz.$$eventMeta && clazz.$$eventMeta[name]) {
    			return clazz.$$eventMeta[name];
    		}
    		if (clazz.$$events && clazz.$$events[name] !== undefined) {
          return {};
        }
        clazz = clazz.superclass;
    	}
    	return null;
    },
    
    getMethodDefinition: function(clazz, name) {
    	var exists = false;
    	while (clazz.superclass) {
    		if (clazz.$$methodMeta && clazz.$$methodMeta[name]) {
    			return clazz.$$methodMeta[name];
    		}
    		if (typeof clazz[name] == "function") {
          exists = true;
        }
    		clazz = clazz.superclass;
    	}
    	return exists ? {} : null;
    }

  }

});
