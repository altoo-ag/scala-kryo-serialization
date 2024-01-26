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
import scala.collection.immutable.{Set as imSet, SortedSet as imSSet}
import scala.collection.mutable.{Set as mSet, SortedSet as mSSet}

class ScalaImmutableSortedSetSerializer() extends Serializer[imSSet[?]] {

  setImmutable(true)

  private var class2constuctor = Map[Class[?], Constructor[?]]()

  override def read(kryo: Kryo, input: Input, typ: Class[? <: imSSet[?]]): imSSet[?] = {
    val len = input.readInt(true)

    var coll: imSSet[Any] = {
      // Read ordering and set it for this collection
      implicit val setOrdering: Ordering[Any] = kryo.readClassAndObject(input).asInstanceOf[scala.math.Ordering[Any]]
      try {
        val constructor =
          class2constuctor.getOrElse(typ, {
              val constr = typ.getDeclaredConstructor(classOf[scala.math.Ordering[?]])
              class2constuctor += typ -> constr
              constr
            })
        constructor.newInstance(setOrdering).asInstanceOf[imSSet[Any]].empty
      } catch {
        case _: Throwable => kryo.newInstance(typ).asInstanceOf[imSSet[Any]].empty
      }
    }

    var i = 0
    while (i < len) {
      coll += kryo.readClassAndObject(input)
      i += 1
    }
    coll
  }

  override def write(kryo: Kryo, output: Output, collection: imSSet[?]): Unit = {
    val len = collection.size
    output.writeInt(len, true)

    kryo.writeClassAndObject(output, collection.ordering)

    val it = collection.iterator
    while (it.hasNext) {
      kryo.writeClassAndObject(output, it.next())
    }
  }
}

class ScalaImmutableSetSerializer() extends Serializer[imSet[?]] {

  setImmutable(true)

  override def read(kryo: Kryo, input: Input, typ: Class[? <: imSet[?]]): imSet[?] = {
    val len = input.readInt(true)
    var coll: imSet[Any] = kryo.newInstance(typ).asInstanceOf[imSet[Any]].empty
    var i = 0
    while (i < len) {
      coll += kryo.readClassAndObject(input)
      i += 1
    }
    coll
  }

  override def write(kryo: Kryo, output: Output, collection: imSet[?]): Unit = {
    output.writeInt(collection.size, true)
    val it = collection.iterator
    while (it.hasNext) {
      kryo.writeClassAndObject(output, it.next())
    }
  }
}

class ScalaImmutableAbstractSetSerializer() extends Serializer[imSet[?]] {

  setImmutable(true)

  override def read(kryo: Kryo, input: Input, typ: Class[? <: imSet[?]]): imSet[?] = {
    val len = input.readInt(true)
    var coll: imSet[Any] = Set.empty
    var i = 0
    while (i < len) {
      coll += kryo.readClassAndObject(input)
      i += 1
    }
    coll
  }

  override def write(kryo: Kryo, output: Output, collection: imSet[?]): Unit = {
    output.writeInt(collection.size, true)
    val it = collection.iterator
    while (it.hasNext) {
      kryo.writeClassAndObject(output, it.next())
    }
  }
}

class ScalaMutableSortedSetSerializer() extends Serializer[mSSet[?]] {
  private var class2constuctor = Map[Class[?], Constructor[?]]()

  override def read(kryo: Kryo, input: Input, typ: Class[? <: mSSet[?]]): mSSet[?] = {
    val len = input.readInt(true)

    val coll: mSSet[Any] = {
      // Read ordering and set it for this collection
      implicit val setOrdering: Ordering[Any] = kryo.readClassAndObject(input).asInstanceOf[scala.math.Ordering[Any]]
      try {
        val constructor =
          class2constuctor.getOrElse(typ, {
              val constr = typ.getDeclaredConstructor(classOf[scala.math.Ordering[?]])
              class2constuctor += typ -> constr
              constr
            })
        constructor.newInstance(setOrdering).asInstanceOf[mSSet[Any]].empty
      } catch {
        case _: Throwable => kryo.newInstance(typ).asInstanceOf[mSSet[Any]].empty
      }
    }

    var i = 0
    while (i < len) {
      coll += kryo.readClassAndObject(input)
      i += 1
    }
    coll
  }

  override def write(kryo: Kryo, output: Output, collection: mSSet[?]): Unit = {
    val len = collection.size
    output.writeInt(len, true)

    kryo.writeClassAndObject(output, collection.ordering)

    val it = collection.iterator
    while (it.hasNext) {
      kryo.writeClassAndObject(output, it.next())
    }
  }
}

class ScalaMutableSetSerializer() extends Serializer[mSet[?]] {

  override def read(kryo: Kryo, input: Input, typ: Class[? <: mSet[?]]): mSet[?] = {
    val len = input.readInt(true)
    val coll: mSet[Any] = kryo.newInstance(typ).asInstanceOf[mSet[Any]].empty
    var i = 0
    while (i < len) {
      coll += kryo.readClassAndObject(input)
      i += 1
    }
    coll
  }

  override def write(kryo: Kryo, output: Output, collection: mSet[?]): Unit = {
    output.writeInt(collection.size, true)
    val it = collection.iterator
    while (it.hasNext) {
      kryo.writeClassAndObject(output, it.next())
    }
  }
}
