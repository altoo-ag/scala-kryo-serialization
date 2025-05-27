package io.altoo.serialization.kryo

import io.altoo.serialization.kryo.scala.ScalaKryoSerializer
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

object LazyValSpec {
case class Message(content: String) {
  lazy val mkContent: String = {
    Thread.sleep(200)
    s"Test string of LazyValSpec is $content."
  }
}

  val ser = new ScalaKryoSerializer(ConfigFactory.defaultReference(), getClass.getClassLoader)

def serialize(obj: Message): Array[Byte] =
  ser.serialize(obj).get

def deserialize(bytes: Array[Byte]): Message =
  ser.deserialize[Message](bytes).get
}

class LazyValSpec extends AnyFlatSpec with Matchers {
  val testMessage: String = "Test if lazy val is safe with intermediate states"

  behavior of "Lazy val serialization"

  it should "be safe with Scala 3 `lazy val` intermediate states (`Evaluating` / `Waiting`)" in {
    val serializedWaitingStateMessage = locally {
      val msg = LazyValSpec.Message(testMessage)

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
    Thread.sleep(1000)

    if (content.isBlank) {
      read.interrupt()
    }

    assert(isStarted)
    assert(content.nonEmpty)
  }
}

