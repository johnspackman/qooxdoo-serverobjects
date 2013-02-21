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

 #asset(explorer/*)

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
			if (qx.core.Variant.isSet("qx.debug", "on")) {
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
					var children = node.serverFile.getChildren();
					for (var i = 0; i < children.getLength(); i++) {
						var file = children.getItem(i);
						var nodeId;
						if (file.getFolder())
							nodeId = dataModel.addBranch(node.nodeId, file.getName(), null);
						else
							nodeId = dataModel.addLeaf(node.nodeId, file.getName(), null);
						tree.nodeGet(nodeId).serverFile = file;
					};
				}, this);
			
			// Create the root node
			var rootNode = tree.nodeGet(dataModel.addBranch(null, "Desktop", false));
			rootNode.serverFile = boot.getAppFilesRoot();
			tree.nodeSetOpened(rootNode, true);
			
    		var uploadForm = new uploadwidget.UploadForm("uploadForm", manager.getProxyUrl());
    		uploadForm.setLayout(new qx.ui.layout.Basic());
			uploadForm.addListener("completed", function(evt) {
	            var strResponse = uploadForm.getIframeHtmlContent();
	            var appFiles = manager.uploadResponse(strResponse);
	            this.debug("Response after uploading to server: " + strResponse);
			}, this);
    		
            var uploadButton = new uploadwidget.UploadButton('uploadfile', 'Upload a File');
            uploadButton.addListener("changeFieldValue", function(evt) {
                if (evt.getData() == "")
                	return;
                
                uploadForm.setParameter("myParam", "testParam");
               	uploadForm.send();
            }, this);
            uploadForm.add(uploadButton);
            this.getRoot().add(uploadForm, { left: 10, top: 50 });
		}
	}
});

