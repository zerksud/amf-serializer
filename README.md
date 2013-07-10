# Exadel AMF-serializer

Library for AMF0/AMF3 messages serialization/deserialization. Part of [Exadel Flamingo](http://exadel.org/flamingo) project.

Changes made for AMF3 serialization/deserialization:

* item order is maintained for deserialized associative arrays
* `java.util.Map` instances are serialized as `AMF3_ARRAY` instead of `AMF3_OBJECT`
