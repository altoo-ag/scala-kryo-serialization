package io.altoo.serialization.kryo.scala

import com.typesafe.config.ConfigFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.ByteBuffer
import scala.concurrent.{Await, Future}
import scala.util.{Success, Try}

object ParallelActorSystemSerializationTest {
  private val config =
    s"""
       |scala-kryo-serialization {
       |  use-unsafe = false
       |  trace = true
       |  id-strategy = "automatic"
       |  implicit-registration-logging = true
       |  post-serialization-transformations = off
       |}
       |""".stripMargin
}

final case class Sample(value: Option[String]) {
  override def toString: String = s"Sample()"
}
object Sample {
  def apply(value: String) = new Sample(Some(value))
}

class ParallelActorSystemSerializationTest extends AnyFlatSpec with Matchers with Inside {
  import scala.concurrent.ExecutionContext.Implicits.global

  private val config = ConfigFactory.parseString(ParallelActorSystemSerializationTest.config).withFallback(ConfigFactory.defaultReference())
  private val serializer1 = new ScalaKryoSerializer(config, getClass.getClassLoader)
  private val serializer2 = new ScalaKryoSerializer(config, getClass.getClassLoader)

  // regression test against https://github.com/altoo-ag/pekko-kryo-serialization/issues/237
  it should "be able to serialize/deserialize in highly concurrent load" in {
    val testClass = Sample("auth-store-syncer")

    val results: List[Future[Unit]] = (for (ser <- List(serializer1, serializer2))
      yield List(
        Future(testSerialization(testClass, ser)),
        Future(testSerialization(testClass, ser)),
        Future(testSerialization(testClass, ser)),
        Future(testSerialization(testClass, ser)),
        Future(testSerialization(testClass, ser)),
        Future(testSerialization(testClass, ser)))).flatten

    import scala.concurrent.duration.*
    Await.result(Future.sequence(results), 10.seconds)
  }

  private def testSerialization(testClass: Sample, serializer: ScalaKryoSerializer): Unit = {
    // find the Serializer for it
    val serialized = serializer.serialize(testClass).get

    // check serialization/deserialization
    val deserialized = serializer.deserialize[AnyRef](serialized)
    deserialized shouldBe util.Success(testClass)

    // check buffer serialization/deserialization
    val bb = ByteBuffer.allocate(serialized.length * 2)
    serializer.serialize(testClass, bb)
    bb.flip()
    val bufferDeserialized = serializer.deserialize[AnyRef](bb)
    bufferDeserialized shouldBe util.Success(testClass)
  }
}
