package io.altoo.external

import io.altoo.serialization.kryo.scala.serializer.ScalaEnumSerializationTest.Sample

enum ExternalEnum(val name: String) {
  case A extends ExternalEnum("a")
}