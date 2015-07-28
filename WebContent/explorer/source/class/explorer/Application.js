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

/* ************************************************************************

 @asset(explorer/*)

 ************************************************************************ */

/**
 * This is the main application class of your custom application "explorer"
 */
qx.Class.define("explorer.Application", {
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
			if (qx.core.Environment.get("qx.debug") == "on") {
				// support native logging capabilities, e.g. Firebug for Firefox
				qx.log.appender.Native;
				// support additional cross-browser console. Press F7 to toggle
				// visibility
				qx.log.appender.Console;
			}
		
			// Document is the application root
			var doc = this.getRoot();
			
			// We want to use some of the high-level node operation convenience methods rather 
			//	than manually digging into the TreeVirtual helper classes.  Include the mixin 
			//	that provides them.
			qx.Class.include(qx.ui.treevirtual.TreeVirtual, qx.ui.treevirtual.MNode);			
		
			// manager is our conduit to the server
			var manager = new com.zenesis.qx.remote.ProxyManager("/explorerServlet/ajax");
			var boot = manager.getBootstrapObject();
			var fileApi = boot.getFileApi();
		
			// create the tree
			var tree = new qx.ui.treevirtual.TreeVirtual("Tree");
			tree.setColumnWidth(0, 400);
			tree.setAlwaysShowOpenCloseSymbol(true);
			tree.setWidth(400);
			this.getRoot().add(tree, {left: 10, top: 100, bottom: 100});
			var dataModel = tree.getDataModel();
			
			// Add a listener to get nodes on demand
			tree.addListener("treeOpenWhileEmpty", function(evt) {
					var node = evt.getData();
					var children = fileApi.listFileInfos(node.serverPath);
					for (var i = 0; i < children.length; i++) {
						var fileInfo = children[i];
						var nodeId;
						if (fileInfo.type == "folder")
							nodeId = dataModel.addBranch(node.nodeId, fileInfo.name, null);
						else
							nodeId = dataModel.addLeaf(node.nodeId, fileInfo.name, null);
						tree.nodeGet(nodeId).serverPath = fileInfo.absolutePath;
					};
				}, this);
			
			// Create the root node
			var rootNode = tree.nodeGet(dataModel.addBranch(null, "Desktop", false));
			rootNode.serverPath = "/";
			tree.nodeSetOpened(rootNode, true);
			
      		var btn = new com.zenesis.qx.upload.UploadButton("Upload File(s)");
      		var uploader = new com.zenesis.qx.upload.UploadMgr(btn, manager.getProxyUrl());
      		uploader.addListener("addFile", function(evt) {
      			var file = evt.getData();

      			// On modern browsers (ie not IE) we will get progress updates
      			var progressListenerId = file.addListener("changeProgress", function(evt) {
      				this.debug("Upload " + file.getFilename() + ": " + evt.getData() + " / " + file.getSize() + " - " +
      						Math.round(evt.getData() / file.getSize() * 100) + "%");
      			}, this);
      			
      			// All browsers can at least get changes in state (ie "uploading", "cancelled", and "uploaded")
      			var stateListenerId = file.addListener("changeState", function(evt) {
      				var state = evt.getData();
      				
      				this.debug(file.getFilename() + ": state=" + state + ", file size=" + file.getSize() + ", progress=" + file.getProgress());
      				
      				if (state == "uploaded" || state == "cancelled") {
          				file.removeListenerById(stateListenerId);
          				file.removeListenerById(progressListenerId);
      				}
      				
      			}, this);
      			
      			this.debug("Uploaded file " + file.getFilename());
      		}, this);
      		
      		doc.add(btn, { left: 50, top: 50 });
		}
	}
});

