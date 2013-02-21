Qooxdoo Server Objects
======================

:: PLEASE NOTE ::
=================

Starting with the release on github (in Feb 2013), the project structure has been reorganised
so that there is only one Eclipse project required during development.

Any problems please contact me through the Qooxdoo mailing list, although feel free
to CC me at john.spackman@zenesis.com to make sure I don't miss your mail.


Getting QxServerObjects
-----------------------

The best way to try out QxServerObjects is to clone the Git repo and open the project into Eclipse.
I'm using Eclipse Juno (4.2) but Eclipse 3.7 should work just fine too.

The project uses Apache Tomcat as it's server runtime - please make sure you have that installed in
Eclipse before continuing.

Clone the repo:
	git clone git://github.com/johnspackman/qx-serverobjects.git

To add the project in Eclipse, choose File -> Import... -> "Existing projects into workspace" and select
the qx-serverobjects repo that Git created for you.

Go to your Servers view, right-click the background and choose New -> Server; choose your server type 
(eg Apache Tomcat 6) and click Finish.  Right click on your new server and choose "Add and Remove..." and
add the QxServerObjects project to your new server.


Configuring your local Qooxdoo installation
-------------------------------------------
In the QxServerObjects project, you need to edit WebContent/demoapp/config.json and WebContent/WEB-INF/web.xml
and change the references to "/Users/john/os/qooxdoo" to be your own local installation of Qooxdoo.

Open a command prompt or terminal window and go to your qx-serverobjects directory; cd to WebContent/demoapp
and run ./generate source.

Start the server and then browse to http://localhost:8080/ and click on the link to run the demoapp Source
version.  You should get an alert message saying "All tests passed!"
	
