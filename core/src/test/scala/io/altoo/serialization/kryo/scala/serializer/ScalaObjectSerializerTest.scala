package io.altoo.serialization.kryo.scala.serializer

import io.altoo.serialization.kryo.scala.testkit.AbstractKryoTest

import java.util.UUID

trait Snowflake {
  val state: UUID = UUID.randomUUID()

  override def hashCode(): Int = state.hashCode()

  override def equals(another: Any): Boolean = another match {
    case anotherSnowflake: Snowflake =>
      // NOTE: don't worry about respecting different flavours of
      // subclass, as all snowflakes are constructed different from
      // each other to start with. Only copies can be equal!
      this.state == anotherSnowflake.state
    case _ => false
  }
}

object standalone extends Snowflake

object ScalaObjectSerializerTest extends Snowflake

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

    (testSerializationOf(standalone) should be).theSameInstanceAs(standalone)

    (testSerializationOf(ScalaObjectSerializerTest) should be).theSameInstanceAs(ScalaObjectSerializerTest)
  }

  it should "support copying of standalone and companion objects" in {
    configureKryo()

    (testCopyingOf(standalone) should be).theSameInstanceAs(standalone)

    (testCopyingOf(ScalaObjectSerializerTest) should be).theSameInstanceAs(ScalaObjectSerializerTest)
  }
}
