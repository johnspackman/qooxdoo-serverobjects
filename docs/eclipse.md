# Installing / Setting up Eclipse

QSO includes an Eclipse project for a sample Tomcat 6 based webapp, and another for developing QSO itself.

## Easier Eclipse Development

Before you get started with Eclipse and Qooxdoo it’s important to understand one major annoyance with Tomcat/Qooxdoo during development: you have to include the Qooxdoo installation directory within the root of your webapp.  Eclipse’s web development tools then publish your webapp to a private folder within the workspace, which means that it will copy 120Mb and 11,000+ Qooxdoo file and then keep that duplicate directory tree synchronized.  This can be really slow and annoying!

Another problem is that because files are copied by Eclipse from your project’s webapp root into the separate webapp deployment, you’d better be sure that Eclipe sees you editing a file or it will not be synchronized.  For example, if you edit config.json and re-run “generate.py”, the changes by the generator will not be seen by Eclipse unless you refresh the project – and the project now includes 11,000+ files that will also be checked for updates.

## A Solution

The easiest way to fix this is to not include Qooxdoo in the Eclipse project (or at least, not in the project’s webapp root) and instead use a servlet filter that rewrites the path so that the files are loaded from your “real” qooxdoo install dir.  This is simple and easy to do and QSO includes a filter for this purpose.

You only need to do this during development – on your production server you can just deploy the build version of your app. 
In your web app’s web.xml, add the following lines:

``` xml
	<filter>
		<filter-name>QxUrlRewriteFilter</filter-name>
		<filter-class>
             com.zenesis.qx.utils.QxUrlRewriteFilter
          </filter-class>
		<init-param>
			<param-name>rewrite-root</param-name>
			<param-value>/some/path</param-value>
		</init-param>
		<init-param>
			<param-name>rewrite-1</param-name>
			<param-value>/qooxdoo=/some/path/qooxdoo</param-value>
		</init-param>
	</filter>
	
	<filter-mapping>
		<filter-name>QxUrlRewriteFilter</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>
```

The init-param’s that are “rewrite-1”, “rewrite-2”, …”rewrite-n” all define paths to rewrite; in the example above, a path (for example) “/qooxdoo/framework/Bootstrap.js” will be redirected to “/some/path/qooxdoo/framework/Bootstrap.js”.

Every “init-param” must have a unique name, ie “rewrite-1”, “rewrite-2”, etc.
If you have several URLs to rewrite that are within a common parent folder, you can set the init-param “rewrite-root” to the common parent, after which all “rewrite-n” rules which are not absolute paths are considered relative to the rewrite-root.  In the example above, you could change “rewrite-1” to be this:

``` xml
		<init-param>
			<param-name>rewrite-1</param-name>
			<param-value>/qooxdoo=qooxdoo</param-value>
		</init-param>
```

and achieve the same effect as the previous example.

## Creating the Demo WebApp

If you want to experiment with the Demo WebApp and the tests/demos contained within it, start by downloading the QSO tree from SourceForge’s SVN archive at: https://github.com/johnspackman/qx-serverobjects

In this part of the documentation, when I refer to the “SVN root” I mean the folder on your local PC that you downloaded the SVN archive into; it should contain the folders “client”, “server”, “demo-webapp” and some other files.

  - In Eclipse, choose “File” -> “Import” -> “Existing projects into workspace” and select the “demo-webapp” folder inside your QSO SVN root and accept all the projects Eclipse finds.
  - Once the projects are imported, right-click “QSO Demo Webapp” and choose “Properties”; goto the “Resource” -> “Linked Resources” section, and edit the Path Variable called “QSO” (create one if it doesn’t exist).  Choose “Folder” and navigate to your QSO SVN root, click OK then click OK.
  - Copy the *.jar files from the server/lib folder inside QSO SVN root into the QSO Demo WebApp’s WebContent/WEB-INF/lib folder (create it if it does not exist).
  - Download the latest Qooxdoo (version 1.1 should be work too) and unpack it into the QSO SVN root; rename the Qooxdoo folder from “qooxdoo-1.3-sdk” to “qooxdoo” (“qooxdoo” is set as svn:ignore).  Note: if you have already installed qooxdoo somewhere, you can skip this step provided that you modify web.xml to use your installation directory in the “rewrite-1” and “rewrite-2” rules.
  - Back in Eclipse, edit the web.xml in WebContent/WEB-INF and change the value of “rewrite-root” to be the absolute path to your QSO SVN root.  Note: If you’ve used your own Qooxdoo installation directory you’ll also need to change “rewrite-1” and “rewrite-2”.
  - You need to compile the demo apps before you can use them; at the command line, go to the “client/demoapp” folder and type “./generate.py source”.  Then go to the “client/explorer” folder and do the same.
 - Right click the project and choose Refresh.
 - Deploy to your Apache Tomcat server instance and start it up. You can browse to http://localhost:8080/ to get started and see the demos.

## A New WebApp - Required Files

For your WebApp that’s going to incorporate QSO & Qooxdoo, download the qso-1.0.jar and qso-1.0-deps.zip files from Sourceforge: https://github.com/johnspackman/qx-serverobjects

You need to add the qso-1.0.jar file to your webapp and any jar’s from qso-1.-deps.zip that you do not already have.

Inside your WebContent folder, create a new skeleton app as normal and add the qso-lib contrib; make sure that the URI for the contrib is ServerObjects/client/qso-lib.

Update your web.xml to include the URL rewriting (see “Creating the Demo WebApp”, above), refresh your project and you’re ready to deploy onto your Tomcat server.
