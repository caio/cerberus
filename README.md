# Cerberus

Cerberus is the Search and Metadata Retrieval library for [gula.recipes][gula]
and makes use of [Lucene][lucene] for searching and [Chronicle-Map][cm] for
metadata persistence / memory-mapping. Metadata is stored as [FlatBuffers][].

[gula]: https://gula.recipes
[cm]: https://github.com/OpenHFT/Chronicle-Map/
[lucene]: http://lucene.apache.org/core/
[FlatBuffers]: https://google.github.io/flatbuffers/

## Build

    mvn install

## Test

    mvn test

