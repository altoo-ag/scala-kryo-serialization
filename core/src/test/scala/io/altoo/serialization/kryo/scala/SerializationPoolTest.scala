package io.altoo.serialization.kryo.scala

import com.typesafe.config.ConfigFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object SerializationPoolTest {
  case class Sample(name: String, firstName: String, tags: Seq[String], address: Address)
  case class Address(street: String, plz: Integer)
}

class QueueSerializerPoolTest extends SerializationPoolTest("queue")
class ThreadLocalSerializerPoolTest extends SerializationPoolTest("threadlocal")

abstract class SerializationPoolTest(poolType: String) extends AnyFlatSpec with Matchers with Inside {
  import SerializationPoolTest.*

  import scala.concurrent.ExecutionContext.Implicits.global

  private val config = ConfigFactory.parseString(s"scala-kryo-serialization.pool-type=$poolType").withFallback(ConfigFactory.defaultReference())
  private val serializer = new ScalaKryoSerializer(config, getClass.getClassLoader)

  it should "provide a separate instance per thread/accessor" in {
    def getSerializer() = serializer.serializerPool.fetch()

    val refA = new AtomicReference[KryoSerializerBackend]()
    val refB = new AtomicReference[KryoSerializerBackend]()

    val a = new Thread(() => refA.set(getSerializer()))
    val b = new Thread(() => refB.set(getSerializer()))

    a.start()
    b.start()

    a.join()
    b.join()

    val sa = refA.get()
    val sb = refB.get()

    (sa shouldNot be).theSameInstanceAs(sb)
  }

  it should "work under multithreaded load" in {
    val testClass = Sample("Foo", "Bar", (1 to 1000).map(_.toString), Address("RandomStreet", 1234))

    val parallelism = 200 // number of concurrent workers
    val iterations = 100 // work per worker

    val tasks = List.fill(parallelism) {
      Future {
        for (_ <- 1 to iterations) {
          testSerialization(testClass)
        }
      }
    }

    Await.result(Future.sequence(tasks), 10.seconds)
  }

  private def testSerialization(testClass: Sample): Unit = {
    // check normal serialization/deserialization
    val serialized = serializer.serialize(testClass).get
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