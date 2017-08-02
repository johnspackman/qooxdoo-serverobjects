/**
 * Class generated by Qoxodoo Server Objects com.zenesis.qx.remote.ClassWriter
 * 
 * @use(com.zenesis.qx.remote.collections.ArrayList)
 * @use(com.zenesis.qx.remote.test.simple.TestAnnos)
 */

qx.Class.define("com.zenesis.qx.remote.test.simple.TestAnnos", {
  "extend" : qx.core.Object,
  "construct" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    args.unshift(arguments);
    this.base.apply(this, args);
    this.initialiseProxy();
 },
  "properties" : {
    "test" : {
      "@" : [ "qso.test.myAnno" ],
      "nullable" : true,
      "apply" : "_applyTest",
      "check" : "String",
      "event" : "changeTest"
    },
    "myStrings" : {
      "@" : [ new com.zenesis.qx.remote.annotations.Property().set({"componentTypeName":"String"}) ],
      "transform" : "__transformMyStrings",
      "nullable" : true,
      "apply" : "_applyMyStrings",
      "check" : "com.zenesis.qx.remote.collections.ArrayList",
      "event" : "changeMyStrings"
    },
    "myTestAnnosMap" : {
      "@" : [ new com.zenesis.qx.remote.annotations.Property().set({"keyTypeName":"String","componentTypeName":"com.zenesis.qx.remote.test.simple.TestAnnos"}) ],
      "nullable" : true,
      "apply" : "_applyMyTestAnnosMap",
      "check" : "com.zenesis.qx.remote.Map",
      "event" : "changeMyTestAnnosMap"
    },
    "myTestAnnos" : {
      "@" : [ new com.zenesis.qx.remote.annotations.Property().set({"componentTypeName":"com.zenesis.qx.remote.test.simple.TestAnnos"}) ],
      "transform" : "__transformMyTestAnnos",
      "nullable" : true,
      "apply" : "_applyMyTestAnnos",
      "check" : "com.zenesis.qx.remote.collections.ArrayList",
      "event" : "changeMyTestAnnos"
    }
  },
  "members" : {
    "helloWorldAsync" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    return new qx.Promise(function(resolve, reject) {
      args.push(function() {
        resolve.apply(this, qx.lang.Array.fromArguments(arguments));
      });
      this._callServer("helloWorld", args);
    }, this);
 },
    "_applyMyStrings" : function(value, oldValue, name) {
    this._applyProperty("myStrings", value, oldValue, name);
 },
    "getMyStringsAsync" : function() {
    return qx.Promise.resolve(this.getMyStrings()).bind(this);
 },
    "_applyTest" : function(value, oldValue, name) {
    this._applyProperty("test", value, oldValue, name);
 },
    "@helloWorld" : [ "qso.test.myMethodAnno" ],
    "__transformMyStrings" : function(value) {
    return com.zenesis.qx.remote.MProxy.transformToDataArray(value, com.zenesis.qx.remote.collections.ArrayList);
 },
    "getMyTestAnnosMapAsync" : function() {
    return qx.Promise.resolve(this.getMyTestAnnosMap()).bind(this);
 },
    "_applyMyTestAnnosMap" : function(value, oldValue, name) {
    this._applyProperty("myTestAnnosMap", value, oldValue, name);
 },
    "getMyTestAnnosAsync" : function() {
    return qx.Promise.resolve(this.getMyTestAnnos()).bind(this);
 },
    "getTestAsync" : function() {
    return qx.Promise.resolve(this.getTest()).bind(this);
 },
    "__transformMyTestAnnos" : function(value) {
    return com.zenesis.qx.remote.MProxy.transformToDataArray(value, com.zenesis.qx.remote.collections.ArrayList);
 },
    "_applyMyTestAnnos" : function(value, oldValue, name) {
    this._applyProperty("myTestAnnos", value, oldValue, name);
 },
    "helloWorld" : function() {
    return this._callServer("helloWorld", qx.lang.Array.fromArguments(arguments));
 }
  },
  "defer" : function(clazz) {
    clazz.$$eventMeta = {};
    clazz.$$methodMeta = {};
    com.zenesis.qx.remote.MProxy.deferredClassInitialisation(clazz);
    clazz.$$methodMeta.helloWorld = {"isServer":true};
    qx.lang.Object.mergeWith(clazz.$$properties.test, {"onDemand":false,"isServer":true,"readOnly":false,"sync":"queue","nativeKeyType":true});
    qx.lang.Object.mergeWith(clazz.$$properties.myStrings, {"onDemand":false,"isServer":true,"arrayClass":"com.zenesis.qx.remote.collections.ArrayList","array":"wrap","readOnly":false,"sync":"queue","nativeKeyType":true});
    qx.lang.Object.mergeWith(clazz.$$properties.myTestAnnosMap, {"onDemand":false,"isServer":true,"array":"wrap","readOnly":false,"sync":"queue","map":true,"nativeKeyType":true});
    qx.lang.Object.mergeWith(clazz.$$properties.myTestAnnos, {"onDemand":false,"isServer":true,"arrayClass":"com.zenesis.qx.remote.collections.ArrayList","array":"wrap","readOnly":false,"sync":"queue","nativeKeyType":true});
    clazz.$$eventMeta.changeMyTestAnnos = {"isServer":true,"isProperty":true};
    clazz.$$eventMeta.changeMyStrings = {"isServer":true,"isProperty":true};
    clazz.$$eventMeta.changeMyTestAnnosMap = {"isServer":true,"isProperty":true};
    clazz.$$eventMeta.changeTest = {"isServer":true,"isProperty":true};
 }
});
