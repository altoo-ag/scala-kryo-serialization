package io.altoo.serialization.kryo.scala.serializer

import io.altoo.serialization.kryo.scala.testkit.AbstractKryoTest

object standalone

object ScalaObjectSerializerTest

class ScalaObjectSerializerTest extends AbstractKryoTest {
  private def configureKryo(): Unit = {
    kryo.setRegistrationRequired(false)
    kryo.addDefaultSerializer(classOf[standalone.type], classOf[ScalaObjectSerializer[Any]])
    kryo.addDefaultSerializer(classOf[ScalaObjectSerializerTest.type], classOf[ScalaObjectSerializer[Any]])
  }

  behavior of "ScalaObjectSerializer"

  it should "round trip standalone and companion objects" in {
    configureKryo()

    testSerializationOf(standalone)

    testSerializationOf(ScalaObjectSerializerTest)
  }

  it should "support copying of standalone and companion objects" in {
    configureKryo()

    testCopyingOf(standalone)

    testCopyingOf(ScalaObjectSerializerTest)
  }
}
