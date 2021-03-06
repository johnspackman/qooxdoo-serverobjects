/**
 * Class generated by Qoxodoo Server Objects com.zenesis.qx.remote.ClassWriter
 * 
 */

qx.Class.define("com.zenesis.qx.remote.test.serialisation.TestAlwaysProxy", {
  "extend" : qx.core.Object,
  "implement" : [ com.zenesis.qx.remote.test.serialisation.ITestDoNotProxy ],
  "construct" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    args.unshift(arguments);
    this.base.apply(this, args);
    this.initialiseProxy();
 },
  "members" : {
    "additionalMethodAsync" : function() {
    var args = qx.lang.Array.fromArguments(arguments);
    return new qx.Promise(function(resolve, reject) {
      args.push(function() {
        resolve.apply(this, qx.lang.Array.fromArguments(arguments));
      });
      this._callServer("additionalMethod", args);
    }, this);
 },
    "additionalMethod" : function() {
    return this._callServer("additionalMethod", qx.lang.Array.fromArguments(arguments));
 }
  },
  "defer" : function(clazz) {
    clazz.$$eventMeta = {};
    clazz.$$methodMeta = {};
    com.zenesis.qx.remote.MProxy.deferredClassInitialisation(clazz);
    clazz.$$methodMeta.additionalMethod = {
      "isServer" : true
    };
 }
});
