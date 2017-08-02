/**
 * Class generated by Qoxodoo Server Objects com.zenesis.qx.remote.ClassWriter
 * 
 */

qx.Class.define("com.zenesis.qx.remote.FileApi", {
  "extend" : qx.core.Object,
  "construct" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    args.unshift(arguments);
    this.base.apply(this, args);
    this.initialiseProxy();
 },
  "properties" : {
    "mimeTypes" : {
      "@" : [ new com.zenesis.qx.remote.annotations.Property().set({"keyTypeName":"String"}) ],
      "nullable" : true,
      "apply" : "_applyMimeTypes",
      "event" : "changeMimeTypes"
    },
    "rootUrl" : {
      "@" : [ new com.zenesis.qx.remote.annotations.Property().set({"readOnly":true}) ],
      "nullable" : true,
      "apply" : "_applyRootUrl",
      "check" : "String",
      "event" : "changeRootUrl"
    }
  },
  "members" : {
    "createFolderAsync" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    return new qx.Promise(function(resolve, reject) {
      args.push(function() {
        resolve.apply(this, qx.lang.Array.fromArguments(arguments));
      });
      this._callServer("createFolder", args);
    }, this);
 },
    "listFileInfos" : function() {
    return this._callServer("listFileInfos", qx.lang.Array.fromArguments(arguments));
 },
    "renameTo" : function() {
    return this._callServer("renameTo", qx.lang.Array.fromArguments(arguments));
 },
    "getFileInfoAsync" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    return new qx.Promise(function(resolve, reject) {
      args.push(function() {
        resolve.apply(this, qx.lang.Array.fromArguments(arguments));
      });
      this._callServer("getFileInfo", args);
    }, this);
 },
    "copyTo" : function() {
    return this._callServer("copyTo", qx.lang.Array.fromArguments(arguments));
 },
    "getTypeAsync" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    return new qx.Promise(function(resolve, reject) {
      args.push(function() {
        resolve.apply(this, qx.lang.Array.fromArguments(arguments));
      });
      this._callServer("getType", args);
    }, this);
 },
    "_applyRootUrl" : function(value, oldValue, name) {
    this._applyProperty("rootUrl", value, oldValue, name);
 },
    "copyToUnique" : function() {
    return this._callServer("copyToUnique", qx.lang.Array.fromArguments(arguments));
 },
    "deleteRecursiveAsync" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    return new qx.Promise(function(resolve, reject) {
      args.push(function() {
        resolve.apply(this, qx.lang.Array.fromArguments(arguments));
      });
      this._callServer("deleteRecursive", args);
    }, this);
 },
    "createFolder" : function() {
    return this._callServer("createFolder", qx.lang.Array.fromArguments(arguments));
 },
    "deleteRecursive" : function() {
    return this._callServer("deleteRecursive", qx.lang.Array.fromArguments(arguments));
 },
    "deleteFile" : function() {
    return this._callServer("deleteFile", qx.lang.Array.fromArguments(arguments));
 },
    "listFilenames" : function() {
    return this._callServer("listFilenames", qx.lang.Array.fromArguments(arguments));
 },
    "renameToAsync" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    return new qx.Promise(function(resolve, reject) {
      args.push(function() {
        resolve.apply(this, qx.lang.Array.fromArguments(arguments));
      });
      this._callServer("renameTo", args);
    }, this);
 },
    "copyToUniqueAsync" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    return new qx.Promise(function(resolve, reject) {
      args.push(function() {
        resolve.apply(this, qx.lang.Array.fromArguments(arguments));
      });
      this._callServer("copyToUnique", args);
    }, this);
 },
    "moveToAsync" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    return new qx.Promise(function(resolve, reject) {
      args.push(function() {
        resolve.apply(this, qx.lang.Array.fromArguments(arguments));
      });
      this._callServer("moveTo", args);
    }, this);
 },
    "_applyMimeTypes" : function(value, oldValue, name) {
    this._applyProperty("mimeTypes", value, oldValue, name);
 },
    "copyToAsync" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    return new qx.Promise(function(resolve, reject) {
      args.push(function() {
        resolve.apply(this, qx.lang.Array.fromArguments(arguments));
      });
      this._callServer("copyTo", args);
    }, this);
 },
    "listFileInfosAsync" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    return new qx.Promise(function(resolve, reject) {
      args.push(function() {
        resolve.apply(this, qx.lang.Array.fromArguments(arguments));
      });
      this._callServer("listFileInfos", args);
    }, this);
 },
    "getRootUrlAsync" : function() {
    return qx.Promise.resolve(this.getRootUrl()).bind(this);
 },
    "getType" : function() {
    return this._callServer("getType", qx.lang.Array.fromArguments(arguments));
 },
    "deleteFileAsync" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    return new qx.Promise(function(resolve, reject) {
      args.push(function() {
        resolve.apply(this, qx.lang.Array.fromArguments(arguments));
      });
      this._callServer("deleteFile", args);
    }, this);
 },
    "exists" : function() {
    return this._callServer("exists", qx.lang.Array.fromArguments(arguments));
 },
    "getMimeTypesAsync" : function() {
    return qx.Promise.resolve(this.getMimeTypes()).bind(this);
 },
    "existsAsync" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    return new qx.Promise(function(resolve, reject) {
      args.push(function() {
        resolve.apply(this, qx.lang.Array.fromArguments(arguments));
      });
      this._callServer("exists", args);
    }, this);
 },
    "listFilenamesAsync" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    return new qx.Promise(function(resolve, reject) {
      args.push(function() {
        resolve.apply(this, qx.lang.Array.fromArguments(arguments));
      });
      this._callServer("listFilenames", args);
    }, this);
 },
    "getFileInfo" : function() {
    return this._callServer("getFileInfo", qx.lang.Array.fromArguments(arguments));
 },
    "moveTo" : function() {
    return this._callServer("moveTo", qx.lang.Array.fromArguments(arguments));
 }
  },
  "defer" : function(clazz) {
    clazz.$$eventMeta = {};
    clazz.$$methodMeta = {};
    com.zenesis.qx.remote.MProxy.deferredClassInitialisation(clazz);
    clazz.$$methodMeta.copyTo = {"isServer":true};
    clazz.$$methodMeta.copyToUnique = {"isServer":true};
    clazz.$$methodMeta.createFolder = {"isServer":true};
    clazz.$$methodMeta.deleteFile = {"isServer":true};
    clazz.$$methodMeta.deleteRecursive = {"isServer":true};
    clazz.$$methodMeta.exists = {"isServer":true};
    clazz.$$methodMeta.getFileInfo = {"isServer":true};
    clazz.$$methodMeta.getType = {"isServer":true};
    clazz.$$methodMeta.listFileInfos = {"isServer":true,"returnArray":"native"};
    clazz.$$methodMeta.listFilenames = {"isServer":true,"returnArray":"native"};
    clazz.$$methodMeta.moveTo = {"isServer":true};
    clazz.$$methodMeta.renameTo = {"isServer":true};
    qx.lang.Object.mergeWith(clazz.$$properties.mimeTypes, {"onDemand":false,"isServer":true,"array":"native","readOnly":false,"sync":"queue","map":true,"nativeKeyType":true});
    qx.lang.Object.mergeWith(clazz.$$properties.rootUrl, {"onDemand":false,"isServer":true,"readOnly":true,"sync":"queue","nativeKeyType":true});
    clazz.$$eventMeta.changeMimeTypes = {"isServer":true,"isProperty":true};
    clazz.$$eventMeta.changeRootUrl = {"isServer":true,"isProperty":true};
 }
});
