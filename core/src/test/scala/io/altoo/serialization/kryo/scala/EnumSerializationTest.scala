package io.altoo.serialization.kryo.scala

import com.typesafe.config.ConfigFactory
import io.altoo.serialization.kryo.scala.performance.Time
import io.altoo.serialization.kryo.scala.performance.Time.Time
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object EnumSerializationTest {
  private val config = {
    """
      |pekko-kryo-serialization {
      |  id-strategy = "default"
      |}
      |""".stripMargin
  }
}

class EnumSerializationTest extends AnyFlatSpec with Matchers {
  private val serializer = new ScalaKryoSerializer(ConfigFactory.parseString(EnumSerializationTest.config).withFallback(ConfigFactory.defaultReference()), getClass.getClassLoader)

  behavior of "Enumeration serialization"

  it should "be threadsafe" in {
    import scala.concurrent.ExecutionContext.Implicits.global

    val listOfTimes = Time.values.toList
    val bytes = serializer.serialize(listOfTimes).get
    val futures = (1 to 2).map(_ =>
      Future[List[Time]] {
        serializer.deserialize[List[Time]](bytes).get
      })

    val result = Await.result(Future.sequence(futures), Duration.Inf)

    assert(result.forall { res => res == listOfTimes })
  }
}
