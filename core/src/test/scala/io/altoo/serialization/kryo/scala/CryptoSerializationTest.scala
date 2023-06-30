package io.altoo.serialization.kryo.scala

import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.ByteBuffer
import scala.collection.immutable.HashMap

object CryptoSerializationTest {
  private val config =
    """
      |scala-kryo-serialization {
      |  post-serialization-transformations = aes
      |  encryption {
      |    aes {
      |      key-provider = "io.altoo.serialization.kryo.scala.DefaultKeyProvider"
      |      mode = "AES/GCM/NoPadding"
      |      iv-length = 12
      |      password = "j68KkRjq21ykRGAQ"
      |      salt = "pepper"
      |    }
      |  }
      |}
      |""".stripMargin
}

class CryptoSerializationTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {
  private val config = ConfigFactory.parseString(CryptoSerializationTest.config)
    .withFallback(ConfigFactory.defaultReference())

  private val sourceSerializer = new ScalaKryoSerializer(config, getClass.getClassLoader)
  private val targetSerializer = new ScalaKryoSerializer(config, getClass.getClassLoader)

  behavior of "Encrypted serialization"

  it should "serialize and deserialize with encryption accross actor systems" in {
    val atm = List {
      HashMap[String, Any](
        "foo" -> "foo",
        "bar" -> "foo,bar,baz",
        "baz" -> 124L)
    }.toArray

    val serialized = sourceSerializer.serialize(atm).get
    val deserialized = targetSerializer.deserialize[Array[HashMap[String, Any]]](serialized)
    deserialized.get should contain theSameElementsInOrderAs atm

    val bb = ByteBuffer.allocate(serialized.length * 2)
    sourceSerializer.serialize(atm, bb)
    bb.flip()
    val bufferDeserialized = targetSerializer.deserialize[Array[HashMap[String, Any]]](bb)
    bufferDeserialized.get should contain theSameElementsInOrderAs atm
  }
}
