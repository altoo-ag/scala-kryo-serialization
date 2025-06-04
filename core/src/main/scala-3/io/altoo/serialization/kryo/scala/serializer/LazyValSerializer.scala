package io.altoo.serialization.kryo.scala.serializer

import scala.runtime.LazyVals.LazyValControlState

import com.esotericsoftware.kryo.kryo5.{Kryo, Serializer}
import com.esotericsoftware.kryo.kryo5.io.{Input, Output}

class LazyValSerializer extends Serializer[LazyValControlState] {
  override def write(kryo: Kryo, output: Output, obj: LazyValControlState): Unit =
    kryo.writeClassAndObject(output, null)

  override def read(kryo: Kryo, input: Input, `type`: Class[? <: LazyValControlState]): LazyValControlState =
    kryo.readClassAndObject(input).asInstanceOf[LazyValControlState]
}
