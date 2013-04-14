qx.Class.define("com.zenesis.qx.remote.Map", {
	extend: qx.core.Object,
	
	construct: function() {
		this.base(arguments);
		this.clear();
	},
	
	events: {
		/**
		 * Fired when the map changes, data is a map containing:
		 * 	type {String} one of "put", "remove", "clear
		 * when type == "put", map also contains:
		 * 	key {String}
		 * 	value {Object}
		 * 	oldValue {Object?}
		 * when type == "remove", map also contains:
		 * 	key {String}
		 * 	value {Object}
		 * when type == "clear", map also contains:
		 * 	values {Map} map of key:value pairs that were removed
		 */
		"change": "qx.event.type.Data"
	},
	
	members: {
		__lookup: null,
		__values: null,
		
		get: function(key) {
			return this.__lookup[key];
		},
		
		put: function(key, value) {
			var oldValue = this.__lookup[key];
			this.__lookup[key = value];
			if (oldValue !== undefined)
				this.__values = null;
			else if (this.__values)
				this.__values.push(value);
			this.fireDataEvent("change", { type: "put", key: key, value: value, oldValue: oldValue });
			return old;
		},
		
		remove: function(key) {
			var value = this.__lookup[key];
			if (value) {
				delete this.__lookup[key];
				this.__values = null;
				this.fireDataEvent("change", { type: "remove", key: key, value: value });
				return true;
			}
			return false;
		},
		
		size: function() {
			if (this.__values)
				return this.__values.length;
			var count = 0;
			for (var name in this.__lookup)
				count++;
			return count;
		},
		
		clear: function() {
			var old = {};
			for (var name in this.__lookup)
				old[name] = this.__lookup[name];
			this.__lookup = {};
			this.__values = null;
			this.fireDataEvent("change", { type: "clear", values: old });
		},
		
		isEmpty: function() {
			for (var name in this.__lookup)
				return false;
			return true;
		},
		
		containsKey: function(key) {
			return this.__lookup[key] !== undefined;
		},
		
		containsValue: function(value) {
			return this.values().indexOf(value) > -1;
		},
		
		values: function() {
			if (!this.__values) {
				var values = this.__values = [];
				for (var name in this.__lookup)
					values.push(this.__lookup[name]);
			}
			return this.__values;
		},
		
		keys: function() {
			var keys = [];
			for (var name in this.__lookup)
				keys.push(name);
			return keys;
		}
	}
});