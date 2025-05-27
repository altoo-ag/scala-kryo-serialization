package io.altoo.serialization.kryo.scala

import com.esotericsoftware.kryo.kryo5.Kryo
import io.altoo.serialization.kryo.scala.serializer.{ScalaCollectionSerializer, ScalaImmutableMapSerializer, ScalaImmutableSetSerializer}

private[kryo] object ScalaVersionSerializers {
  def mapAndSet(kryo: Kryo) = {
    kryo.addDefaultSerializer(classOf[scala.collection.generic.MapFactory[scala.collection.Map]], classOf[ScalaImmutableMapSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.generic.SetFactory[scala.collection.Set]], classOf[ScalaImmutableSetSerializer])
  }

  def iterable(kryo: Kryo) = {
    kryo.addDefaultSerializer(classOf[scala.collection.Traversable[_]], classOf[ScalaCollectionSerializer])
  }

  def enums(kryo: Kryo): Unit = () // Scala 3 only

  def lazyVal(kryo: Kryo): Unit = () // Scala 3 only
}
