# File Handling and Uploads

FileApi provides a means to manipulate (copy, move, delete, rename, etc) files on the server; for security, only files within a given “root” folder can be modified and it is easy to oversee or audit the clients actions.

FileApi works on String paths - ie where previously an AppFile instance was required (which in turned required a parent AppFile), all files are identified by their path within the root folder.  Via the FileApi class, the server can be interrogated for information about the file (eg size, last modified, etc) and for information about MIME types, making it possible to implement an application similar to WIndows Explorer or Mac Finder.

Adding additional functionality (eg image editing or meta data) is trivial because you simply derive your own class from FileApi.

## Using FileApi in your Application

Every ServerObjects application must provide an initial “Bootstrap” object; make your bootstrap object implements com.zenesis.qx.remote.FileApiProvider and implement the getFileApi() method to return an instance of FileApi.

getFileApi() is accessible from the client and is used by the server to handle uploads; the upload handler is compatible with the Qooxdoo Contrib UploadMgr.

For example:
``` javascript
var manager = new com.zenesis.qx.remote.ProxyManager("/myServlet/ajax");
var boot = manager.getBootstrapObject();
var api = boot.getFileApi();

// Returns an array of filenames, eg [“image1.jpg”,”mydoc.pdf”]
var files = api.listFilenames(“/folder”);

// Create a folder
api.createFolder(“/another-folder”);

// Rename and move a file
api.moveTo(“/folder/image1.jpg”, “/another-folder/my-image.jpg”);
```

To get extended information about a file call listFileInfos; this returns an array of plain Javascript objects with properties for size, lastModified, absolutePath, name, etc - note that these are not Qooxdoo objects or ServerObjects - they look like this:

``` javascript
  { name: “image1.jpg”, size: 123456, lastModified: 1353251331437 }
```

## Uploading Files

File uploads are compatible with UploadMgr and can be sent as multi-part form data or octet stream and support upload progress feedback.  When a file has completed uploading, the response from the server is passed to ProxyManager which will fire an event for each uploaded file, or return the result as an array of FileInfo structures.  

In this example, the call to manager.uploadResponse produces the array of FileInfo structures describing each uploaded file.  The ProxyManager will also fire an "uploadComplete" event for each file individually, irrespective of where the upload originated - this is useful if, for example, you support multiple simultaneous uploads from one or more UploadMgr widgets

``` javascript
var uploader = new uploadmgr.UploadMgr();
uploader.addListener("addFile", function(evt) {
  var file = evt.getData();
  var stateListenerId = null;
  stateListenerId = file.addListener("changeState", function(evt) {
    var state = evt.getData();
    if (state == "uploaded") {
      var manager = com.zenesis.qx.remote.ProxyManager.getInstance();
      var appFiles = manager.uploadResponse(file.getResponse());
      if (appFiles) {
        for (var i = 0; i < appFiles.length; i++) {
          console.log(“Uploaded “ + appFiles[i].absolutePath);
        }
      }
      file.removeListenerById(stateListenerId);
    }
  });
});
```

