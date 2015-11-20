# Advanced Topics

## Custom Serialisation

If you want to customise how property values are serialized/deserialized at the server, you can define the “serialize” and “deserialize” attributes to the @Property annotation.  Each method is called with the ProxyProperty object that describes the property and an Object which is the native Java value to convert in or out.
For example:

``` Java
@Properties({
	@Property(value="file", serialize="serializeFile",
    deserialize="deserializeFile")
})
public class Media implements Proxied {
   public File file;
   public void serialize(ProxyProperty property, Object value) {
       return value != null ? ((File)value).toString() : null;
   }
   public void deserialize(ProxyProperty property, Object value) {
       return value != null ? new File((String)value) : null;
   }
```

In the example above, the property “file” is an instance of java.io.File and is converted to a literal string and back when going to and from the client.

## Arrays and Maps

QSO recognised two types of array – native arrays (i.e. Object[]) and wrapped arrays (i.e. ArrayList on the server and qx.data.Array on the client) – and will transparently switch between either if required.

By default, QSO will create arrays on the client in the same form as the server – i.e. if the server has a native array so will the client and if the server has ArrayList, then the client will have qx.data.Array.  You can change this behaviour with the “array” attribute of Property:

``` java
@Property(value="myNativeArray", array=Remote.Array.WRAP),
@Property(value="myArrayList", array=Remote.Array.NATIVE)
...
String[] myNativeArray;
ArrayList<String> myArrayList;
```

In the above example, myNativeArray will be created on the client by wrapping it with qx.data.Array, whereas the myArrayList will be implemented as a native Javascript array.

Maps are similar, except that there is obviously no such thing as a native "map" in Java; by default, QSO will translate a Java Map into an instance of com.zenesis.qx.remote.Map on the client.  If you would prefer that the client see this as a normal javascript map (ie a native
object), use @Property(array=Remote.Array.WRAP).

### Synchronising changes to Arrays and Maps between Client and Server

Changes to ArrayList and HashMap classes can be synchronised automatically if you use the implementations included with QSO on the server - the Java classes called com.zenesis.qx.remote.collections.ArrayList and com.zenesis.qx.remote.collections.HashMap are derived from java.util.ArrayList and java.util.HashMap and are one-for-one replacements.  

## Events and Detecting Changes on the Server

QSO keeps properties values synchronised between client and server in as efficient a manner as possible but this means being able to add event listeners to detect changes.  Qooxdoo does this already on the client and so we need an equivalent for Java on the server.

The ProxyManager class provides static methods to simplify notifying property changes and firing events, which will be relayed to the client.
Here’s an excerpt from ServerFile.rename() which changes the “name” property:

``` java
public boolean rename(String name) {
	if (name.equals(this.name))
		return true;

	// Validation code removed....

	// Change the property
	String oldName = name;
	this.name = name;
	ProxyManager.propertyChanged(this, "name", oldName, name);
	return true;
}
```

ProxyManager also supports fireEvent() and fireDataEvent()

## Creating Server Objects From The Client

When the server app passes a Proxied object to the client, it sends a definition of the class to the client and qx.Class.define() is used to define a parallel class on the client.  Because these are normal Qooxdoo classes your client application can create an instance of that class as and use it as any other object.

When you pass one of these client objects to the server (e.g. the client passes the object as a parameter to a method) the QSO library will create a corresponding object on the server and set its properties to match the client.  

Thereafter, the object is kept synchronised on both sides, just like any other server object.

## Interfaces and Class Heirarchy

From the examples above you’ll see that when you send a POJO which has no super class or interfaces (except Proxied), then QSO will look to that class to get the list of properties, events and methods to create on the client.

QSO also supports arbitrarily complex class hierarchies with interfaces and/or superclasses.  The only requirements are that each class or interface must implement Proxied, and there must be no conflicting property or method names (a conflict is where two or more properties/methods have the same name but a different definition, e.g. two properties with the same name but different type).  Each interface and class will be replicated on the client so that when objects are copied from one side to the other all the normal inheritance properties apply.

By default, if a class implements an interface that extends Proxied, then only properties/methods from the interface will be copied; you can change this by adding the AlwaysProxy annotation.

## Injecting code into the client

The classes generated by QSO on the client have no functionality, but you can add your own code by writing mixins and telling ProxyManager
which classes you want the mixin applied to.  You must do this *before* the ProxyManager defines the class, so the best place to this
is immediately after creating the ProxyManager instance.

For example:

``` JavaScript
var manager = new com.zenesis.qx.remote.ProxyManager("/myServlet/ajax");	
manager.addMixin("mypackage.MyServerClass", myclientapp.MMyServerClassMixin);
```

## Annotations Reference Guide

### AlwaysPresent

This annotation tells QSO that the class or interface already exists on the client – perhaps because it has been hand coded so it can support a more sophisticated range of methods and properties that are only applicable to the client.  When QSO needs to create an instance of the class on the client it will be using your hand written class rather than one copied from the server.

### AlwaysProxy

By default methods are proxied or not depending on context, e.g. if a class implements an interface that extends Proxied the classes method is not copied.  The AlwaysProxy annotation can be added to a class or individual methods to override the default.

### DoNotProxy

This will prevent a method from being proxied


### Method

This is applied to a method to customise how the method is proxied.  If the method returns an array, the array property says whether to wrap the array at the client with qx.data.Array or to have a native array.  The “arrayType” property gives the component type of the array but is only necessary if the method returns a collection (if the method returns a native array then the component type is used).

### Properties

Used only to collect a list of Property annotations.

### Property

Used to declare a property that can be copied between client and server and to customise how that it done.
  * value (the default) – specifies the name of the property; reflection is used to find either a public field or getXxxx/setXxxx/isXxxx methods to read and write the property value.
  * array – if the property is an array, this specifies whether to wrap the array on the client with qx.data.Array or whether to provide it as a native array; by default it follows the server definition.
  * arrayType – if the property is a Collection the arrayType value must be used to set the component type; this is only used when deserialising on the server.
  * sync – specifies whether changes to the property should be relayed immediately (i.e. synchronously) or queued until the next trip to the server is required; the default is to queue the property change.
  * onDemand – specifies whether the property is delivered with the object or whether to wait and only get the value when the client app first requires it and then cached on the client; the default is false.
  * readOnly – specifies whether the property is read only, even if there is a setXxxx() method; the default is to auto-detect.
  * event – the name of the event that will be fired when the property changes; the default is that no event is fired.
  * nullable – the value used for the “nullable” attribute when defining the Qooxdoo property, i.e. whether the property allows null values; the default is true.

## How it works

By default, QSO only proxies classes and interfaces which directly extend the marker interface Proxied; when you try and serialise a Proxied object to JSON, the serialiser writes a unique ID and a class name to the output instead of a normal JSON serialisation.  
The first time an object of a given class is sent to the server, the serialiser will also send a definition of the class, including its methods and the Proxied-derived interfaces and superclasses.  The class definition is also sent if a method returns objects of that class or accepts them as parameters.

The serialiser uses a class called ProxySessionTracker to identify which classes have previously been defined in this session, and outputs the definition if necessary.  

Even though the object model is kept identical between the client and the server, the use case will often vary and one way to do this is to implement the client class by hand; implementing remote method calls is a single line of Javascript.  You then declare server classes as already available on the client by specifying the @AlwaysPresent annotation on the server class and the library will take care of the rest.

## Is it really ANY kind of object?

Yes – in theory.  For security concerns it is usually unwise to expose native server objects, at least not fully (you might want to show a list of files but you probably wouldn’t want to expose the whole of java.io.File :-)).

QSO automatically exposes classes and interfaces which extend the Proxied interface, and it only considers the methods defined as part of that interface/class as suitable for proxying to the client.  You can override this behaviour if you want to by creating your own instance of ProxyType and registering it with ProxyTypeManager.

## ProxySessionTracker

There is exactly one ProxySessionTracker for each session for each Qooxdoo Application and the ProxyManager.handleRequest static method takes care of this provided you give it a unique name for your application.

## ProxyManager

ProxyManager is used to hook up the Jackson JSON processor to the ProxySessionTracker for the application; it must be able to identify the ProxySessionTracker instance for the application.  The current ProxySessionTracker is stored in a ThreadLocal variable in ProxyManager and if you manager your own tracker you must surround calls to QSO with ProxyManager.selectTracker and ProxyManager.deselectTracker.

## Dynamic vs Static Class Definitions

QSO uses reflection to get the class definition and assumes that very few (or none) classes already exist on the client and each time an object is serialised it checks whether the class definition has already been shipped and if necessary sends that along with the object.

What’s good about this is that it gives a very fluid approach to developing an application because there is only one class definition to maintain, therefore it is always up to date and does not depend on an Ant task or similar (which is good if you use an IDE like Eclipse instead of Ant).

What’s bad is that class definitions are being serialised in every session and then sent down the wire in an un-optimised form.  It should be trivial to generate class definitions to disk and have them compiled by the generator for a production build, although there is currently no tool to do this.
