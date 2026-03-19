/**
 * *****************************************************************************
 * Copyright 2012 Roman Levenstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */

package io.altoo.serialization.kryo.scala.serializer

import com.esotericsoftware.kryo.kryo5.io.{Input, Output}
import com.esotericsoftware.kryo.kryo5.{Kryo, Serializer}

import java.lang.reflect.Constructor
import scala.collection.immutable.{SortedMap, Map as IMap}
import scala.collection.mutable
import scala.collection.mutable.Map as MMap

/**
 * Module with specialized serializers for Scala Maps.
 * They are split into 3 different serializers in order:
 * 1. To not need reflection at runtime (find if it is SortedMap)
 * 2. Use inplace updates with mutable Maps
 */
class ScalaMutableMapSerializer extends Serializer[MMap[?, ?]] {

  override def read(kryo: Kryo, input: Input, typ: Class[? <: MMap[?, ?]]): MMap[?, ?] = {
    val len = input.readInt(true)
    val coll = kryo.newInstance(typ).empty.asInstanceOf[MMap[Any, Any]]
    coll.sizeHint(len)
    var i = 0
    while (i < len) {
      coll(kryo.readClassAndObject(input)) = kryo.readClassAndObject(input)
      i += 1
    }
    coll
  }

  override def write(kryo: Kryo, output: Output, collection: MMap[?, ?]): Unit = {
    val len = collection.size
    output.writeInt(len, true)
    if (len != 0) {
      val it = collection.iterator
      while (it.hasNext) {
        val t = it.next()
        kryo.writeClassAndObject(output, t._1)
        kryo.writeClassAndObject(output, t._2)
      }
    }
  }
}

class ScalaImmutableMapSerializer extends Serializer[IMap[?, ?]](false, true) {
  private var emptyMapCache = IMap[Class[?], IMap[Any, Any]]()

  private def emptyMapOf(typ: Class[? <: IMap[?, ?]], kryo: Kryo): IMap[Any, Any] = {
    emptyMapCache.getOrElse(typ, {
      val empty = kryo.newInstance(typ).asInstanceOf[IMap[Any, Any]].empty
      emptyMapCache += typ -> empty
      empty
    })
  }

  override def read(kryo: Kryo, input: Input, typ: Class[? <: IMap[?, ?]]): IMap[?, ?] = {
    val len = input.readInt(true)

    val emptyMap = emptyMapOf(typ, kryo)
    if (len == 0) {
      emptyMap
    } else {
      val builder: mutable.Builder[(Any, Any), IMap[Any, Any]] = emptyMap.mapFactory.newBuilder
      builder.sizeHint(len)
      var i = 0
      while (i < len) {
        builder.addOne((kryo.readClassAndObject(input), kryo.readClassAndObject(input)))
        i += 1
      }
      builder.result()
    }
  }

  override def write(kryo: Kryo, output: Output, collection: IMap[?, ?]): Unit = {
    val len = collection.size
    output.writeInt(len, true)
    if (len != 0) {
      val it = collection.iterator
      while (it.hasNext) {
        val t = it.next()
        kryo.writeClassAndObject(output, t._1)
        kryo.writeClassAndObject(output, t._2)
      }
    }
  }
}

class ScalaImmutableAbstractMapSerializer extends Serializer[IMap[?, ?]](false, true) {
  override def read(kryo: Kryo, input: Input, typ: Class[? <: IMap[?, ?]]): IMap[?, ?] = {
    val len = input.readInt(true)

    if (len == 0) {
      IMap.empty
    } else {
      val builder: mutable.Builder[(Any, Any), IMap[Any, Any]] = IMap.newBuilder
      builder.sizeHint(len)
      var i = 0
      while (i < len) {
        builder.addOne((kryo.readClassAndObject(input), kryo.readClassAndObject(input)))
        i += 1
      }
      builder.result()
    }
  }

  override def write(kryo: Kryo, output: Output, collection: IMap[?, ?]): Unit = {
    val len = collection.size
    output.writeInt(len, true)
    if (len != 0) {
      val it = collection.iterator
      while (it.hasNext) {
        val t = it.next()
        kryo.writeClassAndObject(output, t._1)
        kryo.writeClassAndObject(output, t._2)
      }
    }
  }
}

// All sorted maps are immutable
class ScalaSortedMapSerializer extends Serializer[SortedMap[?, ?]](false, true) {
  private var constructorCache = IMap[Class[?], Constructor[?]]()

  override def read(kryo: Kryo, input: Input, typ: Class[? <: SortedMap[?, ?]]): SortedMap[?, ?] = {
    val len = input.readInt(true)
    implicit val mapOrdering: Ordering[Any] = kryo.readClassAndObject(input).asInstanceOf[scala.math.Ordering[Any]]
    val emptyMap: SortedMap[Any, Any] =
      try {
        val constructor = constructorCache.getOrElse(typ, {
          val constr = typ.getDeclaredConstructor(classOf[scala.math.Ordering[?]])
          constructorCache += typ -> constr
          constr
        })
        constructor.newInstance(mapOrdering).asInstanceOf[SortedMap[Any, Any]].empty
      } catch {
        case _: Throwable => kryo.newInstance(typ).asInstanceOf[SortedMap[Any, Any]].empty
      }

    if (len == 0) {
      emptyMap
    } else {
      var i = 0
      val builder: mutable.Builder[(Any, Any), SortedMap[Any, Any]] = emptyMap.sortedMapFactory.newBuilder
      builder.sizeHint(len)
      while (i < len) {
        builder.addOne((kryo.readClassAndObject(input), kryo.readClassAndObject(input)))
        i += 1
      }
      builder.result()
    }
  }

  override def write(kryo: Kryo, output: Output, collection: SortedMap[?, ?]): Unit = {
    val len = collection.size
    output.writeInt(len, true)

    kryo.writeClassAndObject(output, collection.ordering)

    val it = collection.iterator
    while (it.hasNext) {
      val t = it.next()
      kryo.writeClassAndObject(output, t._1)
      kryo.writeClassAndObject(output, t._2)
    }
  }
}
