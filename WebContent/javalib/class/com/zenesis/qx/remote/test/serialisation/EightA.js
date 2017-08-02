/**
 * Class generated by Qoxodoo Server Objects com.zenesis.qx.remote.ClassWriter
 * 
 * @use(com.zenesis.qx.remote.test.serialisation.IEightB)
 */

qx.Class.define("com.zenesis.qx.remote.test.serialisation.EightA", {
  "extend" : qx.core.Object,
  "implement" : [ com.zenesis.qx.remote.test.serialisation.IEightA ],
  "construct" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    args.unshift(arguments);
    this.base.apply(this, args);
    this.initialiseProxy();
 },
  "properties" : {
    "eightB" : {
      "@" : [ new com.zenesis.qx.remote.annotations.Property().set({"readOnly":true,"componentTypeName":"com.zenesis.qx.remote.test.serialisation.IEightB"}) ],
      "nullable" : true,
      "apply" : "_applyEightB",
      "check" : "Array",
      "event" : "changeEightB"
    }
  },
  "members" : {
    "getEightBAsync" : function() {
    return qx.Promise.resolve(this.getEightB()).bind(this);
 },
    "_applyEightB" : function(value, oldValue, name) {
    this._applyProperty("eightB", value, oldValue, name);
 }
  },
  "defer" : function(clazz) {
    clazz.$$eventMeta = {};
    clazz.$$methodMeta = {};
    com.zenesis.qx.remote.MProxy.deferredClassInitialisation(clazz);
    qx.lang.Object.mergeWith(clazz.$$properties.eightB, {"onDemand":false,"isServer":true,"array":"native","readOnly":true,"sync":"queue","nativeKeyType":true});
    clazz.$$eventMeta.changeEightB = {"isServer":true,"isProperty":true};
 }
});
