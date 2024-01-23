package io.altoo.serialization.kryo.scala.serializer

import com.esotericsoftware.kryo.kryo5.util.{DefaultClassResolver, ListReferenceResolver}
import io.altoo.serialization.kryo.scala.testkit.KryoSerializationTesting
import org.scalatest.flatspec.AnyFlatSpec

class ScalaKryoTest extends AnyFlatSpec with KryoSerializationTesting {

  protected override val kryo: ScalaKryo = new ScalaKryo(new DefaultClassResolver(), new ListReferenceResolver())
  kryo.setRegistrationRequired(false)

  behavior of "ScalaKryo"

  it should "preserve Nil equality" in {
    val deserializedNil = testSerializationOf(Nil)
    assert(deserializedNil eq Nil)
  }
}
