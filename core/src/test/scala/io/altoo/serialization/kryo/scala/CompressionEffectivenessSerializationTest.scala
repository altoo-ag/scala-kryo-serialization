/*
Copyright 2014 Roman Levenstein.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package io.altoo.serialization.kryo.scala

import com.esotericsoftware.minlog.Log
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, Inside}

import java.nio.ByteBuffer

object CompressionEffectivenessSerializationTest {

  private val config =
    s"""
       |scala-kryo-serialization {
       |  trace = true
       |  id-strategy = "incremental"
       |  implicit-registration-logging = true
       |  post-serialization-transformations = "off"
       |}
       |""".stripMargin

  private val compressionConfig =
    s"""
       |scala-kryo-serialization {
       |  post-serialization-transformations = "lz4"
       |}
       |""".stripMargin
}

class CompressionEffectivenessSerializationTest extends AnyFlatSpec with Matchers with ScalaFutures with Inside with BeforeAndAfterAll {
  Log.ERROR()

  private val hugeCollectionSize = 500

  // Long list for testing serializers and compression
  private val testList =
    List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
      1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
      1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
      1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40)

  private val testSeq = Seq(
    "Rome", "Italy", "London", "England", "Paris", "France", "New York", "USA", "Tokio", "Japan", "Peking", "China", "Brussels", "Belgium",
    "Rome", "Italy", "London", "England", "Paris", "France", "New York", "USA", "Tokio", "Japan", "Peking", "China", "Brussels", "Belgium",
    "Rome", "Italy", "London", "England", "Paris", "France", "New York", "USA", "Tokio", "Japan", "Peking", "China", "Brussels", "Belgium",
    "Rome", "Italy", "London", "England", "Paris", "France", "New York", "USA", "Tokio", "Japan", "Peking", "China", "Brussels", "Belgium")

  // test systems
  private val serializer = new ScalaKryoSerializer(
    ConfigFactory.parseString(CompressionEffectivenessSerializationTest.config)
      .withFallback(ConfigFactory.defaultReference()), getClass.getClassLoader)

  private val serializerWithCompression = new ScalaKryoSerializer(
    ConfigFactory.parseString(CompressionEffectivenessSerializationTest.compressionConfig)
      .withFallback(ConfigFactory.parseString(CompressionEffectivenessSerializationTest.config))
      .withFallback(ConfigFactory.defaultReference()), getClass.getClassLoader)

  behavior of "KryoSerializer compression"

  it should "produce smaller serialized List representation when compression is enabled" in {
    val uncompressedSize = serializeDeserialize(serializer, testList)
    val compressedSize = serializeDeserialize(serializerWithCompression, testList)
    (compressedSize.doubleValue() / uncompressedSize) should be < 0.4
    Console.println("Compressed Size = " + compressedSize)
    Console.println("Non-compressed Size = " + uncompressedSize)
  }

  it should "produce smaller serialized huge List representation when compression is enabled" in {
    var testList = List.empty[String]
    (0 until hugeCollectionSize).foreach { i => testList = ("k" + i) :: testList }
    val uncompressedSize = serializeDeserialize(serializer, testList)
    val compressedSize = serializeDeserialize(serializerWithCompression, testList)
    (compressedSize.doubleValue() / uncompressedSize) should be < 0.7
    Console.println("Compressed Size = " + compressedSize)
    Console.println("Non-compressed Size = " + uncompressedSize)
  }

  it should "produce smaller serialized huge Map representation when compression is enabled" in {
    var testMap: Map[String, String] = Map.empty[String, String]
    (0 until hugeCollectionSize).foreach { i => testMap += ("k" + i) -> ("v" + i) }
    val uncompressedSize = serializeDeserialize(serializer, testMap)
    val compressedSize = serializeDeserialize(serializerWithCompression, testMap)
    (compressedSize.doubleValue() / uncompressedSize) should be < 0.8
    Console.println("Compressed Size = " + compressedSize)
    Console.println("Non-compressed Size = " + uncompressedSize)
  }

  it should "produce smaller serialized Seq representation when compression is enabled" in {
    val uncompressedSize = serializeDeserialize(serializer, testSeq)
    val compressedSize = serializeDeserialize(serializerWithCompression, testSeq)
    (compressedSize.doubleValue() / uncompressedSize) should be < 0.8
    Console.println("Compressed Size = " + compressedSize)
    Console.println("Non-compressed Size = " + uncompressedSize)
  }

  it should "produce smaller serialized huge Seq representation when compression is enabled" in {
    var testSeq = Seq[String]()
    (0 until hugeCollectionSize).foreach { i => testSeq = testSeq :+ ("k" + i) }
    val uncompressedSize = serializeDeserialize(serializer, testSeq)
    val compressedSize = serializeDeserialize(serializerWithCompression, testSeq)
    (compressedSize.doubleValue() / uncompressedSize) should be < 0.8
    Console.println("Compressed Size = " + compressedSize)
    Console.println("Non-compressed Size = " + uncompressedSize)
  }

  it should "produce smaller serialized huge Set representation when compression is enabled" in {
    var testSet = Set.empty[String]
    (0 until hugeCollectionSize).foreach { i => testSet += ("k" + i) }
    val uncompressedSize = serializeDeserialize(serializer, testSet)
    val compressedSize = serializeDeserialize(serializerWithCompression, testSet)
    (compressedSize.doubleValue() / uncompressedSize) should be < 0.7
    Console.println("Compressed Size = " + compressedSize)
    Console.println("Non-compressed Size = " + uncompressedSize)
  }

  private def serializeDeserialize(serializer: ScalaKryoSerializer, obj: AnyRef): Int = {
    // Check serializer/deserializer
    val serialized = serializer.serialize(obj).get
    val deserialized = serializer.deserialize[AnyRef](serialized).get

    deserialized.equals(obj) shouldBe true

    // Check buffer serializer/deserializer
    val bb = ByteBuffer.allocate(2 * serialized.length)
    serializer.serialize(obj, bb) shouldBe a[util.Success[?]]
    bb.position() shouldBe serialized.length

    bb.flip()

    val bufferDeserialized = serializer.deserialize[AnyRef](bb).get
    bufferDeserialized.equals(obj) shouldBe true

    serialized.length
  }
}
