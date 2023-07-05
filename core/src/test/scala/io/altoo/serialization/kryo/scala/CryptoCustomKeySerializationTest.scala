package io.altoo.serialization.kryo.scala

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.ByteBuffer
import scala.collection.immutable.HashMap

class KryoCryptoTestKey extends DefaultKeyProvider {
  override def aesKey(config: Config): Array[Byte] = "TheTestSecretKey".getBytes("UTF-8")
}

object CryptoCustomKeySerializationTest {
  private val config = {
    s"""
       |scala-kryo-serialization {
       |  post-serialization-transformations = aes
       |  encryption {
       |    aes {
       |      key-provider = "io.altoo.serialization.kryo.scala.KryoCryptoTestKey"
       |      mode = "AES/GCM/NoPadding"
       |      iv-length = 12
       |    }
       |  }
       |}
       |""".stripMargin
  }
}

class CryptoCustomKeySerializationTest extends AnyFlatSpec with Matchers {
  private val config = ConfigFactory.parseString(CryptoCustomKeySerializationTest.config)
    .withFallback(ConfigFactory.defaultReference())

  private val encryptedSerializer = new ScalaKryoSerializer(config, getClass.getClassLoader)
  private val unencryptedSerializer = new ScalaKryoSerializer(ConfigFactory.defaultReference(), getClass.getClassLoader)

  private val crypto = new KryoCryptographer("TheTestSecretKey".getBytes("UTF-8"), "AES/GCM/NoPadding", 12)

  behavior of "Custom key encrypted serialization"

  it should "encrypt with custom aes key" in {
    val atm = List {
      HashMap[String, Any](
        "foo" -> "foo",
        "bar" -> "foo,bar,baz",
        "baz" -> 124L)
    }.toArray

    val serialized = encryptedSerializer.serialize(atm).get

    val decrypted = crypto.fromBinary(serialized)
    val deserialized = unencryptedSerializer.deserialize[Array[HashMap[String, Any]]](decrypted)
    deserialized.get should contain theSameElementsInOrderAs atm

    val bb = ByteBuffer.allocate(serialized.length * 8)
    encryptedSerializer.serialize(atm, bb) shouldBe a[util.Success[?]]
    bb.flip()
    val unencrypted = crypto.fromBinary(bb)
    val bufferDeserialized = unencryptedSerializer.deserialize[Array[HashMap[String, Any]]](unencrypted)
    bufferDeserialized.get should contain theSameElementsInOrderAs atm
  }

  it should "decrypt with custom aes key" in {
    val atm = List {
      HashMap[String, Any](
        "foo" -> "foo",
        "bar" -> "foo,bar,baz",
        "baz" -> 124L)
    }.toArray

    val serialized = unencryptedSerializer.serialize(atm).get
    val encrypted = crypto.toBinary(serialized)

    val deserialized = encryptedSerializer.deserialize[Array[HashMap[String, Any]]](encrypted)
    deserialized.get should contain theSameElementsInOrderAs atm

    val bufferDeserialized = encryptedSerializer.deserialize[Array[HashMap[String, Any]]](ByteBuffer.wrap(encrypted))
    bufferDeserialized.get should contain theSameElementsInOrderAs atm
  }
}
