package io.altoo.serialization.kryo.scala.performance

import com.typesafe.config.ConfigFactory
import io.altoo.serialization.kryo.scala.ScalaKryoSerializer
import org.scalatest.*
import org.scalatest.flatspec.AnyFlatSpec

object Time extends Enumeration {
  type Time = Value
  val Second, Minute, Hour, Day, Month, Year = Value
}

object EnumPerformanceTests {

  def main(args: Array[String]): Unit = {
    (new PerformanceTests).execute()
  }

  private val defaultConfig =
    """
      |scala-kryo-serialization {
      |    id-strategy = "default"
      |  }
      |""".stripMargin

  class PerformanceTests extends AnyFlatSpec with BeforeAndAfterAllConfigMap {
    import Time.*

    private val serializer = new ScalaKryoSerializer(ConfigFactory.parseString(EnumPerformanceTests.defaultConfig).withFallback(ConfigFactory.defaultReference()), getClass.getClassLoader)

    private def timeIt[A](name: String, loops: Int)(a: () => A): Unit = {
      val now = System.nanoTime
      var i = 0
      while (i < loops) {
        a()
        i += 1
      }
      val ms = (System.nanoTime - now) / 1000000.0
      println(f"$name%s:\t$ms%.1f\tms\t=\t${loops * 1000 / ms}%.0f\tops/s")
    }

    behavior of "Enumeration serialization"

    it should "be fast" in {
      val iterations = 10000

      val listOfTimes = (1 to 1000).flatMap { _ => Time.values.toList }
      timeIt("Enum Serialize:   ", iterations) { () => serializer.serialize(listOfTimes).get }
      timeIt("Enum Serialize:   ", iterations) { () => serializer.serialize(listOfTimes).get }
      timeIt("Enum Serialize:   ", iterations) { () => serializer.serialize(listOfTimes).get }

      val bytes = serializer.serialize(listOfTimes).get

      timeIt("Enum Deserialize: ", iterations)(() => serializer.deserialize[List[Time]](bytes))
      timeIt("Enum Deserialize: ", iterations)(() => serializer.deserialize[List[Time]](bytes))
      timeIt("Enum Deserialize: ", iterations)(() => serializer.deserialize[List[Time]](bytes))
    }
  }
}
