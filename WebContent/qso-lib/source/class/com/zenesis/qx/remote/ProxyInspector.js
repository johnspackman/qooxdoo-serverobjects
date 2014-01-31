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
 * Garbage collection inspector for Proxy objects
 */
qx.Class.define("com.zenesis.qx.remote.ProxyInspector", {
	extend: qx.core.Object,
	implement: [ com.zenesis.gc.IInspector ],
	
	members: {
		
    /**
     * Quick method for recording a new reference to an object
     */
    add: function(parent, child) {
      com.zenesis.gc.GC.addReference(parent, child);
    },
    
    /**
     * Quick method for recording a removed reference to an object
     */
    remove: function(parent, child) {
      com.zenesis.gc.GC.removeReference(parent, child);
    },
    
    /**
     * Quick method for recording a change in value
     */
    change: function(parent, newValue, oldValue) {
      if (newValue !== oldValue) {
        com.zenesis.gc.GC.removeReference(parent, oldValue);
        com.zenesis.gc.GC.addReference(parent, newValue);
      }
    },
    
		/*
		 * @Override com.zenesis.gc.Inspector.gcIterate
		 */
		gcIterate: function(obj, mark, context) {
			var properties = com.zenesis.gc.Inspector.getProperties(obj.constructor);
			for (var name in properties) {
				var prop = properties[name];
				var value = prop.get.call(obj);
				if (value)
					mark.call(context, value);
			}
		},
		
		/*
		 * @Override com.zenesis.gc.Inspector.gcGetPropertyValue
		 */
		gcGetPropertyValue: function(obj, name) {
			var properties = com.zenesis.gc.Inspector.getProperties(obj.constructor);
			var prop = properties[name];
			var oldValue = undefined;
			if (prop) {
				// Get the current property value; this can have side effects, EG if
				//	the property is not yet initialised an exception is raised
				try {
					oldValue = prop.get.call(this);
				} catch(e) {
					// Nothing
				}
			}
			return oldValue;
		},
		
		/*
		 * @Override com.zenesis.gc.Inspector.gcCreate
		 */
		gcCreate: function(value) {
		},
		
		/*
		 * @Override com.zenesis.gc.Inspector.gcDispose
		 */
		gcDispose: function(value) {
		}
	},
	
	statics: {
		__getOnDemand: function(name) {
			return this.$$proxyUser && this.$$proxyUser[name];
		},
		
		getProperties: function(clz) {
			if (clz.hasOwnProperty("$$gc_properties"))
				return clz.$$gc_properties;
			
			var properties = {};
			for (var tmp = clz.prototype; tmp; tmp = tmp.superclass) {
				if (tmp.hasOwnProperty("$$gc_properties")) {
					properties = qx.lang.Object.clone(tmp.$$gc_properties);
					break;
				}
			}
			
			var $$proxyDef = clz.prototype.$$proxyDef;
			if ($$proxyDef.properties) {
				for (var name in $$proxyDef.properties) {
					var def = $$proxyDef.properties[name];
					var getMethod = null;
					if (def.onDemand)
						getMethod = qx.lang.Function.bind(com.zenesis.qx.remote.ProxyInspector.__getOnDemand, null, name);
					else
						getMethod = clz.prototype["get" + qx.lang.String.firstUp(name)];
					
					// Add the property
					properties[name] = {
						get: getMethod	
					};
				}
			}
			
			return clz.$$gc_properties = properties;
		}
	}
});