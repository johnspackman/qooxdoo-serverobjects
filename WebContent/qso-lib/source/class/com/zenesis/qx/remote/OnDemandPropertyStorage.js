qx.Bootstrap.define("com.zenesis.qx.remote.OnDemandPropertyStorage", {
  extend: Object,
  implement: qx.core.property.IPropertyStorage,

  members: {
    /**@override */
    get(thisObj, property) {
      return thisObj._getPropertyOnDemand(property.getPropertyName()); //!todo no protected
    },

    async getAsync(thisObj, property) {
      return thisObj._getPropertyOnDemandAsync(property.getPropertyName());
    },

    /**@override */
    set(thisObj, property, value) {
      thisObj._setPropertyOnDemand(property.getPropertyName(), value);
    },

    /**@override */
    async setAsync(thisObj, property, value) {
      thisObj._setPropertyOnDemand(property.getPropertyName(), value);
    }
  }
});
