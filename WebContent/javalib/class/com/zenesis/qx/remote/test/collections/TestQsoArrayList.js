/**
 * Class generated by Qoxodoo Server Objects com.zenesis.qx.remote.ClassWriter
 * 
 * @use(com.zenesis.qx.remote.collections.ArrayList)
 */

qx.Class.define("com.zenesis.qx.remote.test.collections.TestQsoArrayList", {
  "extend" : qx.core.Object,
  "construct" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    args.unshift(arguments);
    this.base.apply(this, args);
    this.initialiseProxy();
 },
  "properties" : {
    "stringArray" : {
      "@" : [ new com.zenesis.qx.remote.annotations.Property().set({"componentTypeName":"String"}) ],
      "transform" : "__transformStringArray",
      "nullable" : true,
      "apply" : "_applyStringArray",
      "check" : "com.zenesis.qx.remote.collections.ArrayList",
      "event" : "changeStringArray"
    }
  },
  "members" : {
    "getStringArrayAsync" : function() {
    return qx.Promise.resolve(this.getStringArray()).bind(this);
 },
    "_applyStringArray" : function(value, oldValue, name) {
    this._applyProperty("stringArray", value, oldValue, name);
 },
    "makeChanges" : function() {
    return this._callServer("makeChanges", qx.lang.Array.fromArguments(arguments));
 },
    "makeChangesAsync" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    return new qx.Promise(function(resolve, reject) {
      args.push(function() {
        resolve.apply(this, qx.lang.Array.fromArguments(arguments));
      });
      this._callServer("makeChanges", args);
    }, this);
 },
    "__transformStringArray" : function(value) {
    return com.zenesis.qx.remote.MProxy.transformToDataArray(value, com.zenesis.qx.remote.collections.ArrayList);
 }
  },
  "defer" : function(clazz) {
    clazz.$$eventMeta = {};
    clazz.$$methodMeta = {};
    com.zenesis.qx.remote.MProxy.deferredClassInitialisation(clazz);
    clazz.$$methodMeta.makeChanges = {"isServer":true};
    qx.lang.Object.mergeWith(clazz.$$properties.stringArray, {"onDemand":false,"isServer":true,"arrayClass":"com.zenesis.qx.remote.collections.ArrayList","array":"wrap","readOnly":false,"sync":"queue","nativeKeyType":true});
    clazz.$$eventMeta.changeStringArray = {"isServer":true,"isProperty":true};
 }
});
