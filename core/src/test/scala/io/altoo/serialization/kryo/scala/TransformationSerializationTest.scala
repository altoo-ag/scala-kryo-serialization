package io.altoo.serialization.kryo.scala

import com.typesafe.config.ConfigFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.ByteBuffer
import scala.collection.{immutable, mutable}
import scala.collection.immutable.{HashMap, TreeMap}
import scala.collection.mutable.AnyRefMap

object TransformationserializerTest {
  private val defaultConfig =
    """
      |scala-kryo-serializer {
      |  type = "nograph"
      |  id-strategy = "incremental"
      |  kryo-reference-map = false
      |  buffer-size = 65536
      |  post-serializer-transformations = off
      |  implicit-registration-logging = true
      |  encryption {
      |    aes {
      |      key-provider = "io.altoo.serializer.kryo.scala.DefaultKeyProvider"
      |      mode = "AES/GCM/NoPadding"
      |      iv-length = 12
      |      password = "j68KkRjq21ykRGAQ"
      |      salt = "pepper"
      |    }
      |  }
      |}
      |""".stripMargin
}

class ZipTransformationserializerTest extends TransformationserializerTest("Zip", "scala-kryo-serializer.post-serializer-transformations = deflate")
class Lz4TransformationserializerTest extends TransformationserializerTest("LZ4", "scala-kryo-serializer.post-serializer-transformations = lz4")
class AESTransformationserializerTest extends TransformationserializerTest("AES", "scala-kryo-serializer.post-serializer-transformations = aes")
class ZipAESTransformationserializerTest extends TransformationserializerTest("ZipAES", """scala-kryo-serializer.post-serializer-transformations = "deflate,aes"""")
class LZ4AESTransformationserializerTest extends TransformationserializerTest("LZ4AES", """scala-kryo-serializer.post-serializer-transformations = "lz4,aes"""")
class OffTransformationserializerTest extends TransformationserializerTest("Off", "")
class UnsafeTransformationserializerTest extends TransformationserializerTest("Unsafe", "scala-kryo-serializer.use-unsafe = true")
class UnsafeLZ4TransformationserializerTest extends TransformationserializerTest("UnsafeLZ4",
      """
    |scala-kryo-serializer.use-unsafe = true
    |scala-kryo-serializer.post-serializer-transformations = lz4
    """.stripMargin)

abstract class TransformationserializerTest(name: String, testConfig: String) extends AnyFlatSpec with Matchers with Inside {
  private val config = ConfigFactory.parseString(testConfig)
    .withFallback(ConfigFactory.parseString(TransformationserializerTest.defaultConfig))
    .withFallback(ConfigFactory.defaultReference())

  private val serializer = new ScalaKryoSerializer(config, getClass.getClassLoader)

  behavior of s"$name transformation serializer"

  it should "serialize and deserialize immutable TreeMap[String,Any] successfully" in {
    val tm = TreeMap[String, Any](
      "foo" -> 123.3,
      "bar" -> "something as a text",
      "baz" -> null,
      "boom" -> true,
      "hash" -> HashMap[Int, Int](1 -> 200, 2 -> 300, 500 -> 3))

    val serialized = serializer.serialize(tm).get
    val deserialized = serializer.deserialize[TreeMap[String, Any]](serialized)
    deserialized shouldBe util.Success(tm)

    val bb = ByteBuffer.allocate(serialized.length * 2)

    serializer.serialize(tm, bb)
    bb.flip()

    val bufferDeserialized = serializer.deserialize[TreeMap[String, Any]](bb)
    bufferDeserialized shouldBe util.Success(tm)
  }

  it should "serialize and deserialize immutable HashMap[String,Any] successfully" in {
    val tm = HashMap[String, Any](
      "foo" -> 123.3,
      "bar" -> "something as a text",
      "baz" -> null,
      "boom" -> true,
      "hash" -> HashMap[Int, Int](1 -> 200, 2 -> 300, 500 -> 3))

    val serialized = serializer.serialize(tm).get
    val deserialized = serializer.deserialize[HashMap[String, Any]](serialized)
    deserialized shouldBe util.Success(tm)

    val bb = ByteBuffer.allocate(serialized.length * 2)

    serializer.serialize(tm, bb) shouldBe a[util.Success[?]]
    bb.flip()

    val bufferDeserialized = serializer.deserialize[HashMap[String, Any]](bb)
    bufferDeserialized shouldBe util.Success(tm)
  }

  it should "serialize and deserialize mutable AnyRefMap[String,Any] successfully" in {
    val r = new scala.util.Random(0L)
    val tm = AnyRefMap[String, Any](
      "foo" -> r.nextDouble(),
      "bar" -> "foo,bar,baz",
      "baz" -> 124L,
      "hash" -> HashMap[Int, Int](r.nextInt() -> r.nextInt(), 5 -> 500, 10 -> r.nextInt()))

    val serialized = serializer.serialize(tm).get
    val deserialized = serializer.deserialize[AnyRefMap[String, Any]](serialized)
    deserialized shouldBe util.Success(tm)

    val bb = ByteBuffer.allocate(serialized.length * 2)

    serializer.serialize(tm, bb) shouldBe a[util.Success[?]]
    bb.flip()

    val bufferDeserialized = serializer.deserialize[AnyRefMap[String, Any]](bb)
    bufferDeserialized shouldBe util.Success(tm)
  }

  it should "serialize and deserialize mutable HashMap[String,Any] successfully" in {
    val tm = scala.collection.mutable.HashMap[String, Any](
      "foo" -> 123.3,
      "bar" -> "something as a text",
      "baz" -> null,
      "boom" -> true,
      "hash" -> HashMap[Int, Int](1 -> 200, 2 -> 300, 500 -> 3))

    val serialized = serializer.serialize(tm).get
    val deserialized = serializer.deserialize[mutable.HashMap[String, Any]](serialized)
    deserialized shouldBe util.Success(tm)

    val bb = ByteBuffer.allocate(serialized.length * 2)

    serializer.serialize(tm, bb) shouldBe a[util.Success[?]]
    bb.flip()

    val bufferDeserialized = serializer.deserialize[mutable.HashMap[String, Any]](bb)
    bufferDeserialized shouldBe util.Success(tm)
  }

  // Sets
  it should "serialize and deserialize immutable HashSet[String] successfully" in {
    val tm = scala.collection.immutable.HashSet[String]("foo", "bar", "baz", "boom")

    val serialized = serializer.serialize(tm).get
    val deserialized = serializer.deserialize[immutable.HashSet[String]](serialized)
    deserialized shouldBe util.Success(tm)

    val bb = ByteBuffer.allocate(serialized.length * 2)

    serializer.serialize(tm, bb) shouldBe a[util.Success[?]]
    bb.flip()

    val bufferDeserialized = serializer.deserialize[immutable.HashSet[String]](bb)
    bufferDeserialized shouldBe util.Success(tm)
  }

  it should "serialize and deserialize immutable TreeSet[String] successfully" in {
    val tm = scala.collection.immutable.TreeSet[String]("foo", "bar", "baz", "boom")

    val serialized = serializer.serialize(tm).get
    val deserialized = serializer.deserialize[immutable.TreeSet[String]](serialized)
    deserialized shouldBe util.Success(tm)

    val bb = ByteBuffer.allocate(serialized.length * 2)

    serializer.serialize(tm, bb) shouldBe a[util.Success[?]]
    bb.flip()

    val bufferDeserialized = serializer.deserialize[immutable.TreeSet[String]](bb)
    bufferDeserialized shouldBe util.Success(tm)
  }

  it should "serialize and deserialize mutable HashSet[String] successfully" in {
    val tm = scala.collection.mutable.HashSet[String]("foo", "bar", "baz", "boom")

    val serialized = serializer.serialize(tm).get
    val deserialized = serializer.deserialize[mutable.HashSet[String]](serialized)
    deserialized shouldBe util.Success(tm)

    val bb = ByteBuffer.allocate(serialized.length * 2)

    serializer.serialize(tm, bb) shouldBe a[util.Success[?]]
    bb.flip()

    val bufferDeserialized = serializer.deserialize[mutable.HashSet[String]](bb)
    bufferDeserialized shouldBe util.Success(tm)
  }

  it should "serialize and deserialize mutable TreeSet[String] successfully" in {
    val tm = scala.collection.mutable.TreeSet[String]("foo", "bar", "baz", "boom")

    val serialized = serializer.serialize(tm).get
    val deserialized = serializer.deserialize[mutable.TreeSet[String]](serialized)
    deserialized shouldBe util.Success(tm)

    val bb = ByteBuffer.allocate(serialized.length * 2)

    serializer.serialize(tm, bb) shouldBe a[util.Success[?]]
    bb.flip()

    val bufferDeserialized = serializer.deserialize[mutable.TreeSet[String]](bb)
    bufferDeserialized shouldBe util.Success(tm)
  }
}
