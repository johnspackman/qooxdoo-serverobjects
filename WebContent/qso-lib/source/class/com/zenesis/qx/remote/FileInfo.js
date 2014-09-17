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
 * Immutable representation of a file returned from the server (FileApi.FileInfo)
 */
qx.Class.define("com.zenesis.qx.remote.FileInfo", {
	extend: qx.core.Object,
	
	/**
	 * Constructor
	 * @param info
	 */
	construct: function(info) {
		this.base(arguments);
		if (info)
			this.set({ absolutePath: info.absolutePath, name: info.name, type: info.type, lastModified: info.lastModified, size: info.size });
	},
	
	properties: {
		/**
		 * The absolute path of the file, relative to the FileApi's root
		 */
		absolutePath: {
			nullable: false,
			check: "String",
			event: "changeAbsolutePath",
			apply: "_applyAbsolutePath",
			transform: "_transformAbsolutePath"
		},
		
		/**
		 * The filename and extension of the file (no path information)
		 */
		name: {
			nullable: false,
			check: "String",
			event: "changeName",
			apply: "_applyName"
		},
		
		/**
		 * The type of file
		 */
		type: {
			nullable: false,
			check: [ "file", "folder" ],
			event: "changeType",
			apply: "_apply"
		},
		
		size: {
		  nullable: false,
		  check: "Integer",
		  event: "changeSize",
		  apply: "_apply"
		},
		
		/**
		 * When last modified, 0 if the file does not exist
		 */
		lastModified: {
			nullable: false,
			check: "Number",
			event: "changeLastModified",
			apply: "_apply"
		}
	},
	
	members: {
		/**
		 * Quick check whether the file is a file (as opposed to a folder)
		 * @returns {Boolean}
		 */
		isFile: function() {
			return this.getType() == "file";
		},
		
		/**
		 * Quick check whether the file is a folder
		 * @returns {Boolean}
		 */
		isFolder: function() {
			return this.getType() == "folder";
		},
		
		/**
		 * Returns the parent folder as a string.  
		 * @returns null if there is no parent (eg this is the root)
		 */
		getParentFolder: function() {
			var name = this.getAbsolutePath();
			
			// Root folder has no parent
			if (name == "/")
				return null;
			
			// No slashes means improperly formatted (an error)
			var pos = name.lastIndexOf('/');
			if (pos < 0)
				return null;
			
			// Get the folder
			var folder = name.substring(0, pos);
			return folder ? folder : "/";
		},
		
		/**
		 * Returns the extension, excluding the "."
		 */
		getExtension: function() {
		  var m = this.getName().match(/\.([^.]+)$/);
		  return m ? m[1] : "";
		},
		
		/**
		 * Apply for name
		 */
		_applyName: function(value, oldValue) {
		  if (!value)
		    this.setAbsolutePath("/");
		  else {
  		  var abs = this.getParentFolder();
  		  if (!abs)
  		    abs = "/";
  		  else if (!abs.match(/\/$/))
  		    abs += "/";
  		  this.setAbsolutePath(abs + value);
		  }
		},
		
		/**
		 * Apply for absolutePath
		 */
		_applyAbsolutePath: function(value, oldValue) {
		  var name;
		  if (value == "/" || value == "")
		    name = "";
		  else {
  		  var m = value.match(/([^/]+)$/);
  		  if (!m || !m.length)
  		    throw new Error("Cannot interpret absolutePath " + value);
  		  name = m[0];
		  }
		  this.setName(name);
		},
		
		/**
		 * Apply method for all properties, throws an exception
		 * @param value
		 * @param oldValue
		 */
		_apply: function(value, oldValue) {
			if (oldValue)
				throw new Error("Do not change the properties of grasshopper.app.FileInfo (get a new instance from the server)");
		},
		
		/**
		 * Transforms the path
		 * @param value
		 * @returns
		 */
		_transformAbsolutePath: function(value) {
			if (value)
				value = value.replace('\\', '/');
			return value;
		}
	}
});