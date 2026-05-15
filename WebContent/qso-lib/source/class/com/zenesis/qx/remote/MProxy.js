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
  destruct() {
    if (this.__serverId > 0) {
      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      PM.disposeServerObject(this);
    }
  },

  events: {
    changeServerId: "qx.event.type.Data",
    sentToServer: "qx.event.type.Event"
  },

  members: {
    __serverId: null,
    __isPending: undefined,

    /**
     * Called by the constructor to initialise the instance
     */
    initialiseProxy() {
      if (this.$$proxy === undefined) {
        this.$$proxy = {};
        this.setServerId(com.zenesis.qx.remote.ProxyManager.getInstance().getCurrentNewServerId());
      }
      if (this.__initialisers !== undefined) {
        this.__initialisers.forEach(function (cb) {
          cb.call(this, this);
        });
        delete this.__initialisers;
      }
    },

    proxyAddInitialiser(cb, context) {
      if (context) {
        cb = qx.lang.Function.bind(cb, context);
      }
      if (this.__serverId !== null) {
        cb.call(this, this);
      } else {
        if (this.__initialisers === undefined) {
          this.__initialisers = [];
        }
        this.__initialisers.push(cb);
      }
    },

    /**
     * Returns the server ID for this object
     */
    getServerId() {
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
    hasServerId() {
      return this.__serverId !== null;
    },

    /**
     * Sets the server ID - used when the object was created on the client and
     * the server has returned a new server ID for it; to be called by
     * ProxyManager only
     */
    setServerId(serverId) {
      qx.core.Assert.assertTrue(this.__serverId === null || (serverId > 0 && this.__serverId < 0));
      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      delete this.__isPending;
      if (serverId > 0 && this.__serverId < 0) {
        PM.unregisterClientObject(this);
      }
      var oldValue = this.__serverId;
      this.__serverId = serverId;
      if (this.hasListener("changeServerId")) {
        this.fireDataEvent("changeServerId", serverId, oldValue);
      }
    },

    /**
     * Called by ProxyManager when a Proxy instance that was created on the
     * client has been sent to the server
     */
    setSentToServer() {
      qx.core.Assert.assertTrue(this.__isPending);
      delete this.__isPending;
      this.fireEvent("sentToServer");
    },

    /**
     * Detects whether this object has already been sent to the server
     */
    getSentToServer() {
      return !this.__isPending;
    },

    /**
     * Called by methods to invoke the server method call
     */
    _callServer(name, args) {
      if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.traceMethodSync")) {
        var async = args.some(function (arg) {
          return typeof arg == "function" || qx.lang.Type.isPromise(arg);
        });
        if (!async) {
          var trace = qx.dev.StackTrace.getStackTrace();
          qx.log.Logger.warn(com.zenesis.qx.remote.MProxy, `Calling method ${this.classname}.${name} [${this}] synchronously, stack trace:${trace}\n`);
          if (qx.core.Environment.get("com.zenesis.qx.remote.ProxyManager.debuggerSync")) {
            debugger;
          }
        }
      }

      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      PM.clearException();
      var result = PM.callServerMethod(this, name, args);

      var ex = PM.clearException();
      if (ex) {
        throw ex;
      }
      return result;
    },

    _transformProperty(propertyName, value, oldValue) {
      // Skip changing date instances if they are equivalent
      if (value instanceof Date && oldValue instanceof Date && value.getTime() == oldValue.getTime()) {
        return oldValue;
      }

      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      var pd = qx.Class.getPropertyDefinition(this.constructor, propertyName);

      if (value && pd.check === "Date") {
        qx.Annotation.getProperty(this.constructor, propertyName, com.zenesis.qx.remote.annotations.PropertyDate).forEach(annoDate => {
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
     * Overrides the default so that `uuid` properties can be kept in sync with the Qooxdoo uuid
     *
     * @returns {String} the new/current uuid
     */
    toUuid() {
      let uuid = super.toUuid();
      if (typeof this.setUuid == "function") {
        this.setUuid(uuid);
      }
      return uuid;
    },

    setExplicitUuid(uuid) {
      super.setExplicitUuid(uuid);
      if (typeof this.setUuid == "function") {
        this.setUuid(uuid);
      }
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
    _applyProperty(propertyName, value, oldValue) {
      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      var propDef = qx.Class.getPropertyDefinition(this.constructor, propertyName);
      let prop = qx.Class.getByProperty(this.constructor, propertyName);

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
        if (oldValue && propDef.changeListenerId) {
          oldValue.removeListenerById(propDef.changeListenerId);
        }

        if (value) {
          propDef.changeListenerId = value.addListener("change", evt => {
            PM.onWrappedArrayChange(evt, this, prop);
          });
        } else {
          propDef.changeListenerId = null;
        }
      }

      if (propertyName == "uuid") {
        if (this.$$uuid !== value) {
          this.setExplicitUuid(value);
        }
      }

      // We mustn't tell the server anything if it does not yet know about this
      // object[]
      if (this.__isPending) {
        return;
      }
      PM.clearException();

      //We only send the changes to the server if we're not in the middle of a server response,
      //and we are not fetching a property asynchronously
      if (!PM.isInResponse() && !(prop.supportsGetAsync() && prop.isInitializing(this))) {
        if (!prop.hasLocalValue(this) && prop.supportsGetAsync()) {
          PM.expireProperty(this, propertyName);
        } else {
          PM.setPropertyValue(this, propertyName, value, oldValue);
        }
      }
      var ex = PM.clearException();
      if (ex) {
        throw ex;
      }
    },

    /*
     * @Override
     */
    addListener(name, listener, context, capture) {
      var result = super.addListener(name, listener, context, capture);
      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      PM.addServerListener(this, name);
      return result;
    },

    /*
     * @Override
     */
    removeListener(name, listener, context, capture) {
      var existed = super.removeListener(name, listener, context, capture);
      if (!existed) {
        return;
      }
      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();
      PM.removeServerListener(this, name);
      return existed;
    }
  },

  statics: {
    deferredClassInitialisation(clazz) {
      // Make sure it has this mixin - but check first because a super class may have already
      //	included it
      if (!qx.Class.hasMixin(clazz, com.zenesis.qx.remote.MProxy)) {
        qx.Class.patch(clazz, com.zenesis.qx.remote.MProxy);
        clazz = qx.Class.getByName(clazz.classname);
      }

      // Update Packages global - this is for compatibility with Rhino server apps
      var tld = clazz.classname.match(/^[^.]+/)[0];
      if (tld) {
        if (window.Packages === undefined) {
          window.Packages = {};
        }
        window.Packages[tld] = window[tld];
      }

      return clazz;
    },

    /**
     * Used as the transform property for arrays to make sure that the value is
     * always a qx.data.Array or derived
     */
    transformToDataArray(value, clazz) {
      if (!value) {
        return new clazz();
      }
      if (qx.Class.isSubClassOf(value.constructor, clazz)) {
        return value;
      }
      var result = new clazz();
      if (value instanceof qx.data.Array || qx.lang.Type.isArray(value)) {
        result.append(value);
      } else result.push(value);
      return result;
    },

    getEventDefinition(clazz, name) {
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

    getMethodDefinition(clazz, name) {
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
