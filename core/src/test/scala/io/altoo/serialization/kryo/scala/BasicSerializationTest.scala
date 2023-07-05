package io.altoo.serialization.kryo.scala

import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.ByteBuffer

object BasicSerializationTest {

  private val config =
    s"""
       |scala-kryo-serialization {
       |  trace = true
       |  id-strategy = "incremental"
       |  implicit-registration-logging = true
       |  post-serialization-transformations = off
       |}
       |""".stripMargin
}

class BasicSerializationTest extends AnyFlatSpec with Matchers {
  private val config = ConfigFactory.parseString(BasicSerializationTest.config).withFallback(ConfigFactory.defaultReference())
  private val serializer = new ScalaKryoSerializer(config, getClass.getClassLoader)

  private val testList: List[Int] = List(1 to 40: _*)

  behavior of "KryoSerializer"

  it should "be selected for lists" in {
    // Check serialization/deserialization
    val serialized = serializer.serialize(testList).get

    val deserialized = serializer.deserialize[List[Int]](serialized)
    deserialized shouldBe util.Success(testList)

    // Check buffer serialization/deserialization
    val bb = ByteBuffer.allocate(testList.length * 8)

    serializer.serialize(testList, bb)
    bb.flip()
    val bufferDeserialized = serializer.deserialize[List[Int]](bb)
    bufferDeserialized shouldBe util.Success(testList)
  }
}
