# Server Objects

  * [Tutorial / Getting Started By Example](docs/tutorial.md)
  * [File Handling and Uploads](docs/uploads.md)
  * [Installing / Setting Up Eclipse](docs/eclipse.md)
  * [Advanced Usage](docs/advanced.md)
  * [Issues, Roadmap, and Dependencies](docs/issues.md)

The Qooxdoo ServerObjects contrib (aka QSO) is an alternative to the existing RPC mechanisms which are available as part of Qooxdoo and/or as other contribs; the main difference is that QSO is focused on mirroring objects and classes, including the class and interface hierarchy.  The goal is to allow a unified object model to exist on the client and the server, where property values are kept synchronized and events and method invocations are transparently transferred.  

## Benefits

* **Automatic (and configurable)**: Class/interface definitions are discovered automatically on the server via reflection, and fine tuned by the developer using annotations; QSO will transfer class definitions to the Qooxdoo client automatically and on demand.
* **Low impact**: The impact on server classes is minimal - the only requirement for the server class is that they implement an interface.  It is a requirement of QSO that it can be added to existing server classes without having to change the API of those classes (apart from adding the interface at some point in the class hierarchy).  All other definitions or customisations are implemented using annotations.
* **Transparent**: The objects and hierarchy are transferred automatically – there is no separate API to learn/use in order to invoke server methods, you just use the API you wrote on the server.
* **Custom Serialization**: property values can be serialized/deserialized using custom pre/post methods defined on a per-property basis (defined using annotations).
* **Bi-directional objects**: The client implementation of server classes (ie those classes defined in Java that are automatically transferred to the client as Qooxdoo classes) is complete – new instances can be created and used on the client and then sent to the server with no effort from the developer.
* **Exceptions**: Exceptions on the server are transferred to the client too.
* **Java Properties and Events**: adds rich property definitions to the server and an event model that can be used anywhere.
* **Established Code**: QSO is used by over 100+ classes and interfaces for client-server apps on 8 different websites (so far) where it is used for end-user apps and control-panel style server or website management apps; the clients include a large corporate extranet, SMEs, and a charity.
* **Performance**: Based on the Jackson JSON parser/serializer which is significantly faster than the competition and which provides support for lots of low-level parsing/serialization customisation (performance test results: http://www.cowtowncoder.com/blog/archives/01-01-2011_01-31-2011.html#437, or Jackson home page: http://jackson.codehaus.org/)
* **Support**: free support is provided via the mailing list, commercial support is available on request to <john.spackman@zenesis.com> or by visiting www.zenesis.com

Note: QSO uses a protocol based on JSON to communicate with the server that, while easy to read and understand, is not compliant with any other JSON based protocol such as JSON-RPC. 

## Properties and Events

Property values and method return values can be cached or on-demand (configured via annotations in the Java classes), and events triggered by property changes are fired on both the server and the client.

This implies that code on the server can listen to events and fire events when property values have been changed; while Java has the concept of properties by convention (ie “javabeans”), it is only basic (ie there are seXxx and getXxx methods) and not as rich as the the Qooxdoo property and event models.

QSO gives your Java objects a rich property definition and event model that mirror Qooxdoo, plus a few extra features to support client/server interaction, eg whether properties are on-demand or how arrays are copied.  Properties can be defined as getXxx/setXxxx methods or as fields.

The event model can be used on any object – ie it can be used independently from QSO and does not require objects to implement an interface.
