package io.altoo.serialization.kryo.scala.serializer

import com.esotericsoftware.kryo.util.{DefaultClassResolver, ListReferenceResolver}
import io.altoo.serialization.kryo.scala.serializer.scala.ScalaKryo
import io.altoo.serialization.kryo.scala.testkit.KryoSerializationTesting
import org.scalatest.flatspec.AnyFlatSpec

class ScalaKryoTest extends AnyFlatSpec with KryoSerializationTesting {
  val kryo = new ScalaKryo(new DefaultClassResolver(), new ListReferenceResolver())
  kryo.setRegistrationRequired(false)


  behavior of "ScalaKryo"

  it should "preserve Nil equality" in {
    val deserializedNil = testSerializationOf(Nil)
    assert(deserializedNil eq Nil)
  }
}
