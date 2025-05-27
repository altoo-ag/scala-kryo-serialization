package io.altoo.serialization.kryo.scala

import com.esotericsoftware.kryo.kryo5.Kryo
import io.altoo.serialization.kryo.scala.serializer.{ScalaCollectionSerializer, ScalaImmutableMapSerializer}

private[kryo] object ScalaVersionSerializers {
  def mapAndSet(kryo: Kryo): Unit = {
    kryo.addDefaultSerializer(classOf[scala.collection.MapFactory[_root_.scala.collection.Map]], classOf[ScalaImmutableMapSerializer])
  }

  def iterable(kryo: Kryo): Unit = {
    kryo.addDefaultSerializer(classOf[scala.collection.Iterable[_]], classOf[ScalaCollectionSerializer])
  }

  def enums(kryo: Kryo): Unit = () // Scala 3 only

  def lazyVal(kryo: Kryo): Unit = () // Scala 3 only
}
