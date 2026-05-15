qx.Bootstrap.define("com.zenesis.qx.remote.OnDemandPropertyStorage", {
  extend: Object,
  implement: qx.core.property.IPropertyStorage,

  members: {
    /**
     * @override
     * @param {qx.core.Object} thisObj
     * @param {qx.core.propety.IProperty} property
     * @return {*}
     */
    get(thisObj, property) {
      return thisObj.$$proxy.onDemand?.[property.getPropertyName()];
    },

    /**
     * @override
     * @param {qx.core.Object} thisObj
     * @param {qx.core.propety.IProperty} property
     * @return {Promise<*>}
     */
    async getAsync(thisObj, property) {
      let propName = property.getPropertyName();
      let propDef = property.getDefinition();
      if (thisObj.$$proxy.onDemandPromise === undefined) {
        thisObj.$$proxy.onDemandPromise = {};
      }
      var promise = thisObj.$$proxy.onDemandPromise[propName];
      var upname = qx.lang.String.firstUp(propName);
      var PM = com.zenesis.qx.remote.ProxyManager.getInstance();

      const checkWrapArray = value => {
        if (value && propDef.array == "wrap") {
          if (!!propDef.map) {
            if (!(value instanceof com.zenesis.qx.remote.Map)) {
              value = new com.zenesis.qx.remote.Map(value);
            }
          } else if (!(value instanceof qx.data.Array)) {
            value = new qx.data.Array(value);
          }
        }
        return value;
      };

      if (!promise) {
        promise = new Promise((resolve, reject) => {
          PM.callServerMethod(thisObj, "get" + upname, [
            function (value) {
              delete this.$$proxy.onDemandPromise[propName];
              value = checkWrapArray(value);
              var ex = PM.clearException();
              if (ex) {
                reject(ex);
              } else {
                resolve(value);
              }
            }
          ]);
        });
        thisObj.$$proxy.onDemandPromise[propName] = promise;
      }

      return promise;
    },

    /**
     * @override
     * @param {qx.core.Object} thisObj
     * @param {qx.core.propety.IProperty} property the property to set the value of
     * @param {*} value
     */
    set(thisObj, property, value) {
      if (!thisObj.$$proxy.onDemand) {
        thisObj.$$proxy.onDemand = {};
      }
      thisObj.$$proxy.onDemand[property.getPropertyName()] = value;
    },

    /**@override */
    dereference(thisObj, property) {
      delete thisObj.$$proxy.onDemand[property.getPropertyName()];
    },

    /**@override */
    supportsGetAsync() {
      return true;
    }
  }
});
