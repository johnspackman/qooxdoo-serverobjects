qx.Bootstrap.define("com.zenesis.qx.remote.OnDemandPropertyStorage", {
  extend: Object,
  implement: qx.core.property.IPropertyStorage,

  members: {
    /**@override */
    get(thisObj, property) {
      return thisObj.getPropertyOnDemand(property.getPropertyName());
    },

    async getAsync(thisObj, property) {
      return thisObj.getPropertyOnDemandAsync(property.getPropertyName());
    },

    /**@override */
    set(thisObj, property, value) {
      thisObj.setPropertyOnDemand(property.getPropertyName(), value);
    },

    /**@override */
    async setAsync(thisObj, property, value) {
      thisObj.setPropertyOnDemand(property.getPropertyName(), value);
    },

    /**@override */
    reset() {},

    /**@override */
    dereference(thisObj, property) {},

    /**@override */
    supportsGetAsync() {
      return true;
    }
  }
});
