package io.altoo.serialization.kryo.scala.serializer

import io.altoo.serialization.kryo.scala.serializer
import io.altoo.serialization.kryo.scala.testkit.AbstractKryoTest

object standalone

object ScalaObjectSerializerTest

class ScalaObjectSerializerTest extends AbstractKryoTest {
  behavior of "ScalaObjectSerializer"

  it should "round trip standalone and companion objects" in {
    kryo.setRegistrationRequired(false)
    kryo.addDefaultSerializer(classOf[standalone.type], classOf[ScalaObjectSerializer[Any]])
    kryo.addDefaultSerializer(classOf[serializer.ScalaObjectSerializerTest.type], classOf[ScalaObjectSerializer[Any]])

    testSerializationOf(standalone)

    testSerializationOf(ScalaObjectSerializerTest)
  }
}
