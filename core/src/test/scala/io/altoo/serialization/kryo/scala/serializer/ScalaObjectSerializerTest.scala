package io.altoo.serialization.kryo.scala.serializer

import io.altoo.serialization.kryo.scala.testkit.AbstractKryoTest

object standalone

object ScalaObjectSerializerTest

class ScalaObjectSerializerTest extends AbstractKryoTest {
  private def configureKryo(): Unit = {
    kryo.setRegistrationRequired(false)
    // NOTE: to support building under Scala 2.12, use the Java approach of obtaining
    // a singleton object's class at runtime, rather than `classOf[singleton.type]`
    kryo.addDefaultSerializer(standalone.getClass, classOf[ScalaObjectSerializer[Any]])
    kryo.addDefaultSerializer(ScalaObjectSerializerTest.getClass, classOf[ScalaObjectSerializer[Any]])
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
