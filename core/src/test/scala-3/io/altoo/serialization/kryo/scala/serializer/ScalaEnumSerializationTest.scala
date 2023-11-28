package io.altoo.serialization.kryo.scala.serializer

import com.esotericsoftware.kryo.util.{DefaultClassResolver, ListReferenceResolver}
import io.altoo.serialization.kryo.scala.testkit.KryoSerializationTesting
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

object ScalaEnumSerializationTest {
  enum Sample(val name: String, val value: Int) {
    case A extends Sample("a", 1)
    case B extends Sample("b", 2)
    case C extends Sample("c", 3)
  }

  case class EmbeddedEnum(sample: Sample) {
    def this() = this(null)
  }

  enum SimpleADT {
    case A()
    case B
  }
}

class ScalaEnumSerializationTest extends AnyFlatSpec with Matchers with KryoSerializationTesting {
  import ScalaEnumSerializationTest.*

  val kryo = new ScalaKryo(new DefaultClassResolver(), new ListReferenceResolver())
  kryo.setRegistrationRequired(false)
  kryo.addDefaultSerializer(classOf[scala.runtime.EnumValue], new ScalaEnumNameSerializer[scala.runtime.EnumValue])

  behavior of "Kryo serialization"

  it should "round trip enum" in {
    kryo.setRegistrationRequired(false)

    testSerializationOf(Sample.B)
  }

  it should "round trip external enum" in {
    kryo.setRegistrationRequired(false)

    testSerializationOf(io.altoo.external.ExternalEnum.A)
  }

  it should "round trip embedded enum" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[EmbeddedEnum], 46)

    testSerializationOf(EmbeddedEnum(Sample.C))
  }

  it should "round trip adt enum class using generic field serializer" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[SimpleADT], 47)

    testSerializationOf(SimpleADT.A)
  }

  it should "round trip adt enum object using enum serializer" in {
    kryo.setRegistrationRequired(false)
    kryo.register(classOf[SimpleADT], 47)

    testSerializationOf(SimpleADT.B)
  }
}
