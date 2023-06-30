package io.altoo.serialization.kryo.scala.serializer

import io.altoo.serialization.kryo.scala.serializer.scala.ScalaUnitSerializer
import io.altoo.serialization.kryo.scala.testkit.AbstractKryoTest


class ScalaUnitSerializerTest extends AbstractKryoTest {

  behavior of "ScalaUnitSerializer"

  it should "roundtrip unit " in {
    kryo.setRegistrationRequired(true)
    kryo.addDefaultSerializer(classOf[scala.runtime.BoxedUnit], classOf[ScalaUnitSerializer])
    kryo.register(classOf[scala.runtime.BoxedUnit], 50)
    testSerializationOf(())
  }

  it should "roundtrip boxedUnit " in {
    kryo.setRegistrationRequired(true)
    kryo.addDefaultSerializer(classOf[scala.runtime.BoxedUnit], classOf[ScalaUnitSerializer])
    kryo.register(classOf[scala.runtime.BoxedUnit], 50)
    testSerializationOf(scala.runtime.BoxedUnit.UNIT)
  }

}
