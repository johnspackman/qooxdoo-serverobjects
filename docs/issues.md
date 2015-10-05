# Issues, Roadmap, & Dependencies

## Arrays
Arrays are wrapped on the client with qx.data.Array so that changes can be detected, but unfortunately the information in the event is not always enough to reconstruct the changes.  The Java ArrayList does not provide event notifications at all, so changes on the server are not replicated anyway.

## Invalidating Instances

It is possible to invalidate a cached property value, forcing the client to fetch the value on demand next time it is used but it is not possible to do that for entire server objects.  EG a common use case for this would be to get an array (i.e. an ArrayList on the server) and then for that array to be invalidated when changes are made to it.

## Roadmap

  * Fix arrays for efficient updates between client and server (both directions)
  * Better support for collections (i.e. not just ArrayLists)
  * * Remove Proxied interface in exchange for an annotation?
  * Define default values for initialisation; this allows server objects to be created by the client with predefined initial values – remember to test with wrapped arrays
  * Add statics to class definition
  * Generate class definitions for compilation by the generator

## Dependencies
These dependencies are included in the distribution archive
  * Jackson JSON (http://jackson.codehaus.org/)
  * O’Reilly COS
  * Java Activation Framework (activation.jar, required for MIME types); this is included but not needed for JDK 6 and beyond.
  * log4j
