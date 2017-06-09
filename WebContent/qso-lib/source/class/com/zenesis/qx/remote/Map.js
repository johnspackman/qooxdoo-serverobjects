/* ************************************************************************

   server-objects - a contrib to the Qooxdoo project (http://qooxdoo.org/)

   http://qooxdoo.org

   Copyright:
     2010 Zenesis Limited, http://www.zenesis.com

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     
     This software is provided under the same licensing terms as Qooxdoo,
     please see the LICENSE file in the Qooxdoo project's top-level directory 
     for details.

   Authors:
 * John Spackman (john.spackman@zenesis.com)

 ************************************************************************ */

/**
 * Implementation of Map, which is a set of key:value pairs with event handlers
 * for changes to the keys, values, and the entries (where an "entry" is a key/value 
 * pair); this class is to <code>{}</code> as <code>qx.data.Array</code> is to 
 * <code>[]</code>
 * 
 * By default, the keys are stored in a native object which means that only native
 * types which can be converted to a String can be used as keys; however, by 
 * specifying the `keysAreHashed` parameter to the constructor, the class will
 * require that all keys are instance of <code>qx.core.Object</code> and will
 * use the object's hash code.
 * 
 * This has a side effect when importing and exporting the Map into an external
 * form, via the <code>replace()</code> and <code>toObject()</code methods; Maps
 * which have `keysAreHashed==true` are imported and exported as an array of
 * native objects with key & value properties.
 * 
 * @see com.zenesis.qx.remote.Entry
 */
qx.Class.define("com.zenesis.qx.remote.Map", {
  extend: qx.core.Object,

  /**
   * Constructor.
   * @param values {Object?} values to import
   * @param keysAreHashed {Boolean?} whether keys are objects and according to their hash  
   */
  construct: function(values, keysAreHashed, keyClass, valueClass) {
    this.base(arguments);
    var args = qx.lang.Array.fromArguments(arguments);
    if (typeof args[0] == "boolean")
      args.unshift(undefined);
    values = args.shift();
    keysAreHashed = args.shift();
    keyClass = args.shift();
    valueClass = args.shift();
    
    this.__keysAreHashed = keysAreHashed;
    this.__lookupEntries = {};
    this.set({
      keys: new qx.data.Array(),
      values: new qx.data.Array(),
      entries: new qx.data.Array()
    });
    if (keyClass !== undefined)
      this.setKeyClass(keyClass);
    if (valueClass !== undefined)
      this.setValueClass(valueClass);
    if (values !== undefined) {
      this.replace(values);
    }
  },
  
  events: {
    /**
     * Fired when the map changes, data is a map containing: 
     * type {String} one of "put", "remove" 
     * values {Object[]} values which had changed, each map contains: 
     *  key {String} 
     *  value {Object} 
     *  oldValue {Object?}
     */
    "change": "qx.event.type.Data"
  },

  properties: {
    /**
     * List of all keys in the map, should never be set explicitly
     */
    keys: {
      nullable: false,
      check: "qx.data.Array",
      event: "changeValues",
      apply: "_applyKeys"
    },

    /**
     * List of all values in the map, should never be set explicitly
     */
    values: {
      nullable: false,
      check: "qx.data.Array",
      event: "changeValues",
      apply: "_applyValues"
    },
    
    /** List of all Entry's in the map */
    entries: {
      nullable: false,
      check: "qx.data.Array",
      event: "changeEntries",
      apply: "_applyEntries"
    },
    
    /** Class of keys (ignored for native key types) */
    keyClass: {
      nullable: true,
      init: null,
      check: "Class",
      transform: "_transformToClass"
    },
    
    /** Class of values */
    valueClass: {
      nullable: true,
      init: null,
      check: "Class",
      transform: "_transformToClass"
    }
  },

  members: {
    // Implementation of the map
    __lookupEntries: null,
    
    // Whether keys are objects and the hashcode is stored (as opposed to native values, ie string)
    __keysAreHashed: false,
    
    // Anti recursion mutex
    __changingValue: false,
    
    /**
     * Gets a value from the map
     * 
     * @param key
     *          {String} the key to lookup
     * @returns {Object?} the object that was found, or undefined
     */
    get: function(key) {
      var id = this.__getKey(key);
      var entry = this.__lookupEntries[id];
      return entry ? entry.getValue() : undefined;
    },
    
    /**
     * Gets an entry from the map
     * 
     * @param key
     *          {String} the key to lookup
     * @returns {Entry?} the entry that was found, or null
     */
    getEntry: function(key) {
      var id = this.__getKey(key);
      return this.__lookupEntries[id] || null;
    },

    /**
     * Puts a new value in the map
     * 
     * @param key
     *          {String} the key to assign
     * @param value
     *          {Object} the object to set for the key, if undefined the key is
     *          removed
     * @returns {Object?} the previous object for the key, or undefined
     */
    put: function(key, value) {
      if (value === undefined) {
        return this.remove(key);
      }

      var data = this.__putImpl(key, value);

      this.fireDataEvent("change", {
        type: "put",
        values: [ {
          key: key,
          value: value,
          oldValue: data.oldValue
        } ]
      });
      return data.oldValue;
    },
    
    __getKey: function(key) {
      if (this.__keysAreHashed) {
        if (key === null || key === undefined)
          throw new Error("Invalid key passed to Map.__getKey");
        var hash = qx.core.ObjectRegistry.toHashCode(key);
        return hash;
      }
      return key;
    },
    
    /**
     * Internal implementation of put
     * @param key {String} the key
     * @param value {Object} the object
     */
    __putImpl:function(key, value) {
      var keyClass = this.getKeyClass();
      var valueClass = this.getValueClass();
      if (keyClass && !(key instanceof keyClass))
        throw new Error("Cannot put key into map because key is the wrong class, expected " + keyClass + ", given key=" + key);
      if (valueClass && !(value instanceof valueClass))
        throw new Error("Cannot put value into map because value is the wrong class, expected " + valueClass + ", given value=" + value);
      qx.core.Assert.assertFalse(this.__changingValue);
      this.__changingValue = true;
      try {
        var values = this.getValues();
        var keys = this.getKeys();
        var entries = this.getEntries();
        var id = this.__getKey(key);
        
        var entry = this.__lookupEntries[id];
        var oldValue = null;
        var result;
        
        if (entry) {
          oldValue = entry.getValue();
          values.remove(oldValue);
          entry.setValue(value);
          if (!values.contains(value))
            values.push(value);
          result = {
            key: key,
            value: value,
            entry: entry,
            oldValue: oldValue
          };
        } else {
          entry = new com.zenesis.qx.remote.Entry(key, value);
          this.__attachEntry(entry);
          if (!values.contains(value))
            values.push(value);
          if (!keys.contains(key))
            keys.push(key);
          result = {
            key: key,
            value: value,
            entry: entry
          };
        }
        
        return result;
      } finally {
        this.__changingValue = false;
      }
    },
    
    /**
     * Attaches an entry
     */
    __attachEntry: function(entry) {
      entry.addListener("changeValue", this.__onEntryChangeValue, this);
      this.getEntries().push(entry);
      var id = this.__getKey(entry.getKey());
      this.__lookupEntries[id] = entry;
    },
    
    /**
     * Detaches an entry
     */
    __detachEntry: function(entry) {
      entry.removeListener("changeValue", this.__onEntryChangeValue, this);
      this.getEntries().remove(entry);
      var id = this.__getKey(entry.getKey());
      delete this.__lookupEntries[id];
    },
    
    /**
     * Event handler for changes to an entry's value property
     */
    __onEntryChangeValue: function(evt) {
      if (this.__changingValue)
        return;
      var entry = evt.getTarget();
      var value = entry.getValue();
      var oldValue = evt.getOldData();
      var remove = true;
      for (var id in this.__lookupEntries) {
        if (this.__lookupEntries[id].getValue() === oldValue) {
          remove = false;
          break;
        }
      }
      var values = this.getValues();
      if (remove)
        values.remove(oldValue);
      if (!values.contains(value))
        values.push(value);
      
      this.fireDataEvent("change", {
        type: "put",
        values: [ {
          key: entry.getKey(),
          value: entry.getValue(),
          oldValue: oldValue,
          entry: entry
        } ]
      });
    },
    
    
    /**
     * Replaces all of the elements in this map
     * @deprected in favour of better name to match qx.data.Array @see <code>replace</code>
     */
    replaceAll: function(src) {
      qx.log.Logger.deprecatedMethodWarning(arguments.callee);
      this.replace(src);
    },
    

    /**
     * Replaces all of the elements in this map with another, firing only one or
     * two "change" events for "put" and/or "remove"
     * 
     * @param src
     *          {com.zenesis.qx.remote.Map|Object} the map or object to copy
     *          from
     */
    replace: function(src) {
      var t = this;
      if (src instanceof com.zenesis.qx.remote.Map)
        src = src.toObject();
      
      var values = this.getValues();
      var keys = this.getKeys();
      var entries = this.getEntries();

      var removed = [];
      var changed = [];
      
      var srcEntries = {};
      if (this.__keysAreHashed) {
        src.forEach(function(entry) {
          var id = t.__getKey(entry.key);
          srcEntries[id] = entry;
        });
      } else if (qx.lang.Type.isArray(src)) {
        src.forEach(function(entry) {
          var id = entry.key;
          srcEntries[id] = entry;
        });
      } else {
        for (var name in src) {
          srcEntries[name] = { key: name, value: src[name] };
        }
      }

      for (var id in this.__lookupEntries) {
        if (srcEntries[id] === undefined) {
          var tmp = this.__lookupEntries[id];
          removed.push({
            key: tmp.getKey(),
            value: tmp.getValue(),
            entry: tmp
          });
          values.remove(tmp.getValue());
          keys.remove(id);
          this.__detachEntry(tmp);
        }
      }
      
      for (var id in srcEntries) {
        var entry = srcEntries[id];
        changed.push(this.__putImpl(entry.key, entry.value));
      }

      if (Object.keys(removed).length !== 0) {
        this.fireDataEvent("change", {
          type: "remove",
          values: removed
        });
      }
      if (Object.keys(changed).length !== 0) {
        this.fireDataEvent("change", {
          type: "put",
          values: changed
        });
      }
    },

    /**
     * Removes a key:value pair
     * 
     * @param key
     *          {String|Entry} the key to remove
     * @returns {Object} the previous value for the key, or undefined
     */
    remove: function(key) {
      var entry;
      if (key instanceof com.zenesis.qx.remote.Entry) {
        if (qx.core.Environment.get("qx.debug")) {
          qx.core.Assert.assertIdentical(key, this.__lookupEntries[this.__getKey(key.getKey())]);
        }
        entry = key;
      } else {
        var id = this.__getKey(key);
        entry = this.__lookupEntries[id];
      }
      
      if (entry) {
        this.__detachEntry(entry);
        this.getValues().remove(entry.getValue());
        this.getKeys().remove(entry.getKey());
        this.fireDataEvent("change", {
          type: "remove",
          values: [ {
            key: key,
            value: entry.getValue(),
            entry: entry
          } ]
        });
      }
      
      return entry ? entry.getValue() : undefined;
    },

    /**
     * Removes all entries from the map
     */
    removeAll: function() {
      var old = [];
      for (var id in this.__lookupEntries) {
        var entry = this.__lookupEntries[id];
        old.push({
          key: entry.getKey(),
          value: entry.getValue(),
          entry: entry
        });
        this.__detachEntry(entry);
      }
      this.getValues().removeAll();
      this.getKeys().removeAll();
      this.fireDataEvent("change", {
        type: "remove",
        values: old
      });
    },
    
    /**
     * Equivalent of Array.forEach for every key/value pair
     * @param cb {Function} called with (key, value, entry)
     */
    forEach: function(cb) {
      var t = this;
      return Object.keys(this.__lookupEntries).forEach(function(id) {
        var entry = this.__lookupEntries[id];
        return cb(entry.getKey(), entry.getValue(), entry);
      });
    },

    /**
     * Equivalent of Array.some for every key/value pair
     * @param cb {Function} called with (key, value)
     */
    some: function(cb) {
      var t = this;
      return Object.keys(this.__lookupEntries).some(function(id) {
        var entry = this.__lookupEntries[id];
        return cb(entry.getKey(), entry.getValue(), entry);
      });
    },

    /**
     * Equivalent of Array.every for every key/value pair
     * @param cb {Function} called with (key, value)
     */
    every: function(cb) {
      var t = this;
      return Object.keys(this.__lookupEntries).every(function(id) {
        var entry = this.__lookupEntries[id];
        return cb(entry.getKey(), entry.getValue(), entry);
      });
    },

    /**
     * Number of entries in the map
     * 
     * @returns {Integer}
     */
    getLength: function() {
      return this.getKeys().getLength();
    },

    /**
     * Returns true if the map is empty
     * 
     * @returns {Boolean}
     */
    isEmpty: function() {
      return this.getLength() == 0;
    },

    /**
     * Detects whether the key is in use
     * 
     * @param key
     *          {String}
     * @returns {Boolean}
     */
    containsKey: function(key) {
      var id = this.__getKey(key);
      return this.__lookupEntries[id] !== undefined;
    },

    /**
     * Detects whether the value is in use
     * 
     * @param value
     *          {Object}
     * @returns {Boolean}
     */
    containsValue: function(value) {
      return this.getValues().indexOf(value) > -1;
    },

    /**
     * Returns a copy of the native object containing the lookup; note that this cannot
     * work (and will throw an exception) if the keys are hashed, because it is not possible
     * to use objects as keys in a native map (@see toArray instead)
     */
    toObject: function() {
      if (this.__keysAreHashed) {
        throw new Error("Cannot export as an object because the map uses keys which are objects");
      }
      
      var result = {};
      var lookup = this.__lookupEntries;
      this.getKeys().forEach(function(id) {
        var entry = this.__lookupEntries[id];
        result[entry.getKey()] = entry.getValue();
      }.bind(this));
      
      return result;
    },
    
    /**
     * Outputs the map as an array of objects with key & value properties; this is a guaranteed
     * export mechanism because it will work whether the keys are hashed or not
     */
    toArray: function() {
      var result = [];
      for (var id in this.__lookupEntries) {
        var entry = this.__lookupEntries[id];
        result.push({
          key: entry.getKey(),
          value: entry.getValue()
        });
      }
      return result;
    },

    /**
     * Apply method for values property
     * 
     * @param value
     *          {Object}
     * @param oldValue
     *          {Object}
     */
    _applyValues: function(value, oldValue) {
      if (oldValue)
        throw new Error("Cannot change property values of com.zenesis.qx.remote.Map");
    },

    /**
     * Apply method for keys property
     * 
     * @param value
     *          {Object}
     * @param oldValue
     *          {Object}
     */
    _applyKeys: function(value, oldValue) {
      if (oldValue)
        throw new Error("Cannot change property keys of com.zenesis.qx.remote.Map");
    },

    /**
     * Apply method for entries property
     * 
     * @param value
     *          {Object}
     * @param oldValue
     *          {Object}
     */
    _applyEntries : function(value, oldValue) {
      if (oldValue)
        throw new Error("Cannot change property entries of com.zenesis.qx.remote.Map");
    },
    
    /**
     * Transform for keyClass and valueClass, converts strings to classes
     */
    _transformToClass: function(value) {
      if (value)
        value = qx.Class.getByName(value);
      return value;
    }

  }
});