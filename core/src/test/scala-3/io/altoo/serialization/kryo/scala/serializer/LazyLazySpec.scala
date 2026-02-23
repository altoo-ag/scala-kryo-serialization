package io.altoo.serialization.kryo.scala.serializer

import io.altoo.serialization.kryo.scala.ScalaKryoSerializer
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.util.concurrent.CountDownLatch

object LazyLazySpec {
  object Gates {
    val entered = new CountDownLatch(2)
    val release = new CountDownLatch(1)
  }

  case class Victim() {
    lazy val x: String = {
      Gates.entered.countDown()
      Gates.release.await()
      "ok"
    }
  }

  final case class Wrapper(a: Victim, b: Victim)

  val ser = new ScalaKryoSerializer(ConfigFactory.defaultReference(), getClass.getClassLoader)

  def serialize(obj: Wrapper): Array[Byte] =
    ser.serialize(obj).get

  def deserialize(bytes: Array[Byte]): Wrapper =
    ser.deserialize[Wrapper](bytes).get
}

class LazyLazySpec extends AnyFlatSpec with Matchers {
  import LazyLazySpec.*

  behavior of "Lazy val with multiple references"

  it should "safely serialize and deserialize wrapper with shared Victim instances during lazy evaluation" in {
    val v1 = Victim()
    val v2 = Victim()
    val wrapper = Wrapper(v1, v2)

    // Start threads that will trigger lazy val evaluation
    val t1 = new Thread(() => {
      v1.x
      ()
    })
    val t2 = new Thread(() => {
      v2.x
      ()
    })
    t1.start()
    t2.start()

    // Wait for both threads to enter lazy val evaluation
    Gates.entered.await()

    // Serialize while lazy vals are being evaluated
    val bytes = serialize(wrapper)
    bytes should not be empty

    // Release the threads to complete lazy val evaluation
    Gates.release.countDown()
    t1.join(5000)
    t2.join(5000)

    // Deserialize and verify
    val decoded = deserialize(bytes)
    decoded should not be null
    decoded.a should not be null
    decoded.b should not be null

    // Verify lazy val can be accessed on deserialized objects
    decoded.b.x shouldBe "ok"
  }
}
