package io.altoo.serialization.kryo.scala.testkit

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.testkit.TestKit
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, Inside}
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class AbstractPekkoTest(config: Config = ConfigFactory.empty) extends TestKit(ActorSystem("testSystem", config)) with AnyFlatSpecLike with Matchers with Inside with BeforeAndAfterAll {

  override protected def afterAll(): Unit = shutdown(system)
}
