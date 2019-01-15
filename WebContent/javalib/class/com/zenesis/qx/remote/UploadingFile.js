/**
 * Class generated by Qoxodoo Server Objects com.zenesis.qx.remote.ClassWriter
 * 
 */

qx.Class.define("com.zenesis.qx.remote.UploadingFile", {
  "extend" : qx.core.Object,
  "construct" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    args.unshift(arguments);
    this.base.apply(this, args);
    this.initialiseProxy();
 },
  "properties" : {
    "bytesUploaded" : {
      "@" : [ new com.zenesis.qx.remote.annotations.Property().set({"readOnly":true}) ],
      "nullable" : false,
      "apply" : "_applyBytesUploaded",
      "event" : "changeBytesUploaded"
    },
    "uploadId" : {
      "@" : [ new com.zenesis.qx.remote.annotations.Property().set({"readOnly":true}) ],
      "nullable" : true,
      "apply" : "_applyUploadId",
      "check" : "String",
      "event" : "changeUploadId"
    }
  },
  "members" : {
    "expireBytesUploaded" : function(sendToServer) {
    return this._expirePropertyOnDemand('bytesUploaded', sendToServer);
 },
    "setBytesUploaded" : function(value, async) {
    return this._setPropertyOnDemand('bytesUploaded', value, async);
 },
    "getBytesUploadedAsync" : function() {
    return qx.Promise.resolve(this.getBytesUploaded()).bind(this);
 },
    "_applyUploadId" : function(value, oldValue, name) {
    this._applyProperty("uploadId", value, oldValue, name);
 },
    "getUploadIdAsync" : function() {
    return qx.Promise.resolve(this.getUploadId()).bind(this);
 },
    "_applyBytesUploaded" : function(value, oldValue, name) {
    this._applyProperty("bytesUploaded", value, oldValue, name);
 },
    "getBytesUploaded" : function(async) {
    return this._getPropertyOnDemand('bytesUploaded', async);
 }
  },
  "defer" : function(clazz) {
    clazz.$$eventMeta = {};
    clazz.$$methodMeta = {};
    com.zenesis.qx.remote.MProxy.deferredClassInitialisation(clazz);
    qx.lang.Object.mergeWith(clazz.$$properties.bytesUploaded, {"onDemand":true,"isServer":true,"readOnly":true,"sync":"queue","nativeKeyType":true});
    qx.lang.Object.mergeWith(clazz.$$properties.uploadId, {"onDemand":false,"isServer":true,"readOnly":true,"sync":"queue","nativeKeyType":true});
    clazz.$$eventMeta.changeBytesUploaded = {"isServer":true,"isProperty":true};
    clazz.$$eventMeta.changeUploadId = {"isServer":true,"isProperty":true};
 }
});