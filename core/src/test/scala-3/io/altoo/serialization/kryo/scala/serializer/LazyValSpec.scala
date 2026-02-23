package io.altoo.serialization.kryo.scala.serializer

import io.altoo.serialization.kryo.scala.ScalaKryoSerializer
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.util.concurrent.CountDownLatch

object LazyValSpec {
  case class Message(content: String) {
    lazy val mkContent: String = {
      Thread.sleep(200)
      s"Test string of LazyValSpec is $content."
    }
  }

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

  def serialize(obj: Message): Array[Byte] =
    ser.serialize(obj).get

  def deserialize(bytes: Array[Byte]): Message =
    ser.deserialize[Message](bytes).get

  def serializeWrapper(obj: Wrapper): Array[Byte] =
    ser.serialize(obj).get

  def deserializeWrapper(bytes: Array[Byte]): Wrapper =
    ser.deserialize[Wrapper](bytes).get
}

class LazyValSpec extends AnyFlatSpec with Matchers {
  import LazyValSpec.*

  behavior of "Lazy val serialization"

  it should "be safe with Scala 3 `lazy val` intermediate states (`Evaluating` / `Waiting`)" in {
    val serializedWaitingStateMessage = locally {
      val msg = LazyValSpec.Message("Test if lazy val is safe with intermediate states")

      val evaluatingLazyVal = new Thread(() => {
        msg.mkContent // start evaluation before serialization
        ()
      })
      evaluatingLazyVal.start()

      Thread.sleep(50) // give some time for the fork to start lazy val rhs eval

      LazyValSpec.serialize(msg) // serialize in the meantime so that we capture Waiting state
    }

    val deserializedWaitingStateMsg = LazyValSpec.deserialize(serializedWaitingStateMessage)

    @volatile var content = ""
    @volatile var isStarted = false

    val read = new Thread(() => {
      isStarted = true
      content = deserializedWaitingStateMsg.mkContent
      ()
    })

    read.start()
    read.join(1000)

    assert(isStarted, "Lazy val was never accessed in the thread")
    assert(!content.isBlank, s"Lazy val content was blank")
  }

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
    val bytes = serializeWrapper(wrapper)
    bytes should not be empty

    // Release the threads to complete lazy val evaluation
    Gates.release.countDown()
    t1.join(5000)
    t2.join(5000)

    // Deserialize and verify
    val decoded = deserializeWrapper(bytes)
    decoded should not be null
    decoded.a should not be null
    decoded.b should not be null

    // Verify lazy val can be accessed on deserialized objects
    decoded.b.x shouldBe "ok"
  }
}
