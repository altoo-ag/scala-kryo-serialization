package io.altoo.external

import io.altoo.pekko.serialization.kryo.serializer.scala.ScalaEnumSerializationTest.Sample

enum ExternalEnum(val name: String) {
  case A extends ExternalEnum("a")
}