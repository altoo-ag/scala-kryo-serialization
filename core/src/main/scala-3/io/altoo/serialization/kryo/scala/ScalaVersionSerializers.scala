package io.altoo.serialization.kryo.scala

import com.esotericsoftware.kryo.kryo5.Kryo
import io.altoo.serialization.kryo.scala.serializer.{LazyValSerializer, ScalaCollectionSerializer, ScalaEnumNameSerializer, ScalaImmutableMapSerializer}

private[kryo] object ScalaVersionSerializers {
  def mapAndSet(kryo: Kryo): Unit = {
    kryo.addDefaultSerializer(classOf[scala.collection.MapFactory[_root_.scala.collection.Map]], classOf[ScalaImmutableMapSerializer])
  }

  def iterable(kryo: Kryo): Unit = {
    kryo.addDefaultSerializer(classOf[scala.collection.Iterable[?]], classOf[ScalaCollectionSerializer])
  }

  def enums(kryo: Kryo): Unit = {
    kryo.addDefaultSerializer(classOf[scala.runtime.EnumValue], classOf[ScalaEnumNameSerializer[scala.runtime.EnumValue]])
  }

  def lazyVal(kryo: Kryo): Unit = {
    kryo.register(classOf[scala.runtime.LazyVals.Waiting], new LazyValSerializer)
    kryo.register(classOf[scala.runtime.LazyVals.Evaluating.type], new LazyValSerializer)
  }
}
