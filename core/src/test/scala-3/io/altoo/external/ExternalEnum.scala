package io.altoo.external

import io.altoo.serialization.kryo.scala.serializer.scala.ScalaEnumSerializationTest.Sample

enum ExternalEnum(val name: String) {
  case A extends ExternalEnum("a")
}