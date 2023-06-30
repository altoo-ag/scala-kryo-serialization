package io.altoo.pekko.serialization.kryo.serializer.pekko

import org.apache.pekko.actor.{Actor, ActorRef, Props}
import org.apache.pekko.serialization.SerializationExtension
import com.typesafe.config.ConfigFactory
import io.altoo.pekko.serialization.kryo.KryoSerializer
import io.altoo.pekko.serialization.kryo.testkit.AbstractPekkoTest

object ActorRefSerializerTest {
  private val testConfig =
    """
      |pekko {
      |  actor {
      |    serializers {
      |      kryo = "io.altoo.pekko.serialization.kryo.KryoSerializer"
      |    }
      |    serialization-bindings {
      |      "org.apache.pekko.actor.ActorRef" = kryo
      |    }
      |  }
      |}
      |pekko-kryo-serialization {
      |  trace = true
      |  id-strategy = "default"
      |  implicit-registration-logging = true
      |  post-serialization-transformations = off
      |}
      |""".stripMargin
}

class ActorRefSerializerTest extends AbstractPekkoTest(ConfigFactory.parseString(ActorRefSerializerTest.testConfig)) {
  private val serialization = SerializationExtension(system)


  behavior of "ActorRefSerializer"

  it should "serialize and deserialize actorRef" in {
    val value: ActorRef = system.actorOf(Props(new Actor {def receive: Receive = PartialFunction.empty}))

    // serialize
    val serializer = serialization.findSerializerFor(value)
    serializer shouldBe a[KryoSerializer]

    val serialized = serialization.serialize(value)
    serialized shouldBe a[util.Success[_]]

    // deserialize
    val deserialized = serialization.deserialize(serialized.get, classOf[ActorRef])
    deserialized shouldBe util.Success(value)
  }
}
