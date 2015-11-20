# Issues, Roadmap, & Dependencies

## Invalidating Instances

It is possible to invalidate a cached property value, forcing the client to fetch the value on demand next time it is used but it is not possible to do that for entire server objects.

## Roadmap

  * Add statics to class definition
  * Generate class definitions for compilation by the generator

## Dependencies
These dependencies are included in the distribution archive
  * Jackson JSON (http://jackson.codehaus.org/)
  * O’Reilly COS
  * Java Activation Framework (activation.jar, required for MIME types); this is included but not needed for JDK 6 and beyond.
  * log4j2
