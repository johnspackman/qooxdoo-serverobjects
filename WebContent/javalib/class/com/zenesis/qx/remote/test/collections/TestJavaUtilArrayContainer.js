/**
 * Class generated by Qoxodoo Server Objects com.zenesis.qx.remote.ClassWriter
 * 
 * @use(com.zenesis.qx.remote.test.collections.TestJavaUtilArrayContainer$Child)
 * @use(com.zenesis.qx.remote.collections.HashMap)
 * @use(com.zenesis.qx.remote.collections.ArrayList)
 */

qx.Class.define("com.zenesis.qx.remote.test.collections.TestJavaUtilArrayContainer", {
  "extend" : qx.core.Object,
  "construct" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    args.unshift(arguments);
    this.base.apply(this, args);
    this.initialiseProxy();
 },
  "properties" : {
    "list" : {
      "@" : [ new com.zenesis.qx.remote.annotations.Property().set({
  "componentTypeName":"com.zenesis.qx.remote.test.collections.TestJavaUtilArrayContainer$Child"
}) ],
      "transform":"__transformList",
      "nullable" : true,
      "apply":"_applyList",
      "check":"com.zenesis.qx.remote.collections.ArrayList",
      "event":"changeList"
    },
    "map" : {
      "@" : [ new com.zenesis.qx.remote.annotations.Property().set({
  "keyTypeName":"String",
  "componentTypeName":"String"
}) ],
      "nullable" : true,
      "apply":"_applyMap",
      "check":"com.zenesis.qx.remote.collections.HashMap",
      "event":"changeMap"
    }
  },
  "members" : {
    "test" : function() {
    return this._callServer("test", qx.lang.Array.fromArguments(arguments));
 },
    "_applyMap" : function(value, oldValue, name) {
    this._applyProperty("map", value, oldValue, name);
 },
    "_applyList" : function(value, oldValue, name) {
    this._applyProperty("list", value, oldValue, name);
 },
    "getListAsync" : function() {
    return qx.Promise.resolve(this.getList()).bind(this);
 },
    "getMapAsync" : function() {
    return qx.Promise.resolve(this.getMap()).bind(this);
 },
    "testAsync" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    return new qx.Promise(function(resolve, reject) {
      args.push(function() {
        resolve.apply(this, qx.lang.Array.fromArguments(arguments));
      });
      this._callServer("test", args);
    }, this);
 },
    "__transformList" : function(value) {
    return com.zenesis.qx.remote.MProxy.transformToDataArray(value, com.zenesis.qx.remote.collections.ArrayList);
 }
  },
  "defer" : function(clazz) {
    clazz.$$eventMeta = {};
    clazz.$$methodMeta = {};
    com.zenesis.qx.remote.MProxy.deferredClassInitialisation(clazz);
    clazz.$$methodMeta.test = {
      "isServer" : true
    };
    qx.lang.Object.mergeWith(clazz.$$properties.list, {
      "onDemand" : false,
      "isServer" : true,
      "arrayClass":"com.zenesis.qx.remote.collections.ArrayList",
      "array":"wrap",
      "readOnly" : false,
      "sync":"queue",
      "nativeKeyType" : true
    });
    qx.lang.Object.mergeWith(clazz.$$properties.map, {
      "onDemand" : false,
      "isServer" : true,
      "arrayClass":"com.zenesis.qx.remote.collections.HashMap",
      "array":"wrap",
      "readOnly" : false,
      "sync":"queue",
      "map" : true,
      "nativeKeyType" : true
    });
    clazz.$$eventMeta.changeMap = {
      "isServer" : true,
      "isProperty" : true
    };
    clazz.$$eventMeta.changeList = {
      "isServer" : true,
      "isProperty" : true
    };
 }
});
