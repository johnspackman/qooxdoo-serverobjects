/* ************************************************************************

   server-objects - a contrib to the Qooxdoo project that makes server 
   and client objects operate seamlessly; like Qooxdoo, server objects 
   have properties, events, and methods all of which can be access from
   either server or client, regardless of where the original object was
   created.

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

/* ************************************************************************

 #asset(arraytest/*)

 ************************************************************************ */

/**
 * This is the main application class of your custom application "arraytest"
 */
qx.Class.define("arraytest.Application", {
	extend : qx.application.Standalone,

	/*
	 * ****************************************************************************
	 * MEMBERS
	 * ****************************************************************************
	 */

	members : {
		/**
		 * This method contains the initial application code and gets called
		 * during startup of the application
		 * 
		 * @lint ignoreDeprecated(alert)
		 */
		main : function() {
			// Call super class
			this.base(arguments);
		
			// Enable logging in debug variant
			if (qx.core.Variant.isSet("qx.debug", "on")) {
				// support native logging capabilities, e.g. Firebug for Firefox
				qx.log.appender.Native;
				// support additional cross-browser console. Press F7 to toggle
				// visibility
				qx.log.appender.Console;
			}

			var arr = new qx.data.Array([ "One", "Two", "Three", "Four", "Five" ]);
			var purpose="";
			
			arr.addListener("change", function(evt) {
				var data = evt.getData();
				this.debug(purpose + ": " + data.type + ", start=" + data.start + ", end=" + data.end + ", items=" + qx.lang.Json.stringify(data.items));
				this.debug(" ==> " + qx.lang.Json.stringify(arr.toArray()));
			}, this);
			
			// Doesn't fire event!
			purpose="append"; 		arr.append([ "Six", "Seven" ]);
			
			
			purpose="insertAfter";	arr.insertAfter(arr.getItem(2), "ThreeA");
			purpose="insertBefore";	arr.insertBefore(arr.getItem(2), "TwoA");
			purpose="insertAt";		arr.insertAfter(arr.getItem(1), "OneA");

			purpose="pop";			arr.pop();
			purpose="push";			arr.push("Eight");
			purpose="remove";		arr.remove(arr.getItem(5));
			purpose="removeAt";		arr.removeAt(5);
			
			purpose="setItem";		arr.setItem(5, "Nine");
			purpose="shift";		arr.shift();
			
			purpose="sort";			arr.sort();
			purpose="splice";		arr.splice(1, 2, "Lemon");
			
			purpose="unshift";		arr.unshift("Ten");
			
			purpose="removeAll";	arr.removeAll();
		}
	}
});
