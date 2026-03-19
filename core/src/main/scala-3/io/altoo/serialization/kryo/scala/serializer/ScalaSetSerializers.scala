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
import scala.collection.immutable.{Set as ISet, SortedSet as ISSet}
import scala.collection.mutable
import scala.collection.mutable.{Set as MSet, SortedSet as MSSet}

class ScalaImmutableSortedSetSerializer extends Serializer[ISSet[?]](false, true) {
  private var class2constuctor = Map[Class[?], Constructor[?]]()

  override def read(kryo: Kryo, input: Input, typ: Class[? <: ISSet[?]]): ISSet[?] = {
    val len = input.readInt(true)

    implicit val setOrdering: Ordering[Any] = kryo.readClassAndObject(input).asInstanceOf[scala.math.Ordering[Any]]
    val emptySet: ISSet[Any] = {
      // Read ordering and set it for this collection
      try {
        val constructor =
          class2constuctor.getOrElse(typ, {
              val constr = typ.getDeclaredConstructor(classOf[scala.math.Ordering[?]])
              class2constuctor += typ -> constr
              constr
            })
        constructor.newInstance(setOrdering).asInstanceOf[ISSet[Any]].empty
      } catch {
        case _: Throwable => kryo.newInstance(typ).asInstanceOf[ISSet[Any]].empty
      }
    }

    if (len == 0) {
      emptySet
    } else {
      val builder = emptySet.sortedIterableFactory.newBuilder
      builder.sizeHint(len)
      var i = 0
      while (i < len) {
        builder.addOne(kryo.readClassAndObject(input))
        i += 1
      }
      builder.result()
    }
  }

  override def write(kryo: Kryo, output: Output, collection: ISSet[?]): Unit = {
    val len = collection.size
    output.writeInt(len, true)

    kryo.writeClassAndObject(output, collection.ordering)

    val it = collection.iterator
    while (it.hasNext) {
      kryo.writeClassAndObject(output, it.next())
    }
  }
}

class ScalaImmutableSetSerializer extends Serializer[ISet[?]](false, true) {

  override def read(kryo: Kryo, input: Input, typ: Class[? <: ISet[?]]): ISet[?] = {
    val len = input.readInt(true)
    val emptySet: ISet[Any] = kryo.newInstance(typ).asInstanceOf[ISet[Any]].empty
    if (len == 0) {
      emptySet
    } else {
      val builder: mutable.Builder[Any, ISet[Any]] = emptySet.iterableFactory.newBuilder
      builder.sizeHint(len)
      var i = 0
      while (i < len) {
        builder.addOne(kryo.readClassAndObject(input))
        i += 1
      }
      builder.result()
    }
  }

  override def write(kryo: Kryo, output: Output, collection: ISet[?]): Unit = {
    output.writeInt(collection.size, true)
    val it = collection.iterator
    while (it.hasNext) {
      kryo.writeClassAndObject(output, it.next())
    }
  }
}

class ScalaImmutableAbstractSetSerializer extends Serializer[ISet[?]](false, true) {

  override def read(kryo: Kryo, input: Input, typ: Class[? <: ISet[?]]): ISet[?] = {
    val len = input.readInt(true)
    if (len == 0) {
      Set.empty
    } else {
      val builder: mutable.Builder[Any, ISet[Any]] = Set.empty.iterableFactory.newBuilder
      builder.sizeHint(len)
      var i = 0
      while (i < len) {
        builder.addOne(kryo.readClassAndObject(input))
        i += 1
      }
      builder.result()
    }
  }

  override def write(kryo: Kryo, output: Output, collection: ISet[?]): Unit = {
    output.writeInt(collection.size, true)
    val it = collection.iterator
    while (it.hasNext) {
      kryo.writeClassAndObject(output, it.next())
    }
  }
}

class ScalaMutableSortedSetSerializer extends Serializer[MSSet[?]] {
  private var class2constuctor = Map[Class[?], Constructor[?]]()

  override def read(kryo: Kryo, input: Input, typ: Class[? <: MSSet[?]]): MSSet[?] = {
    val len = input.readInt(true)
    // Read ordering and set it for this collection
    implicit val setOrdering: Ordering[Any] = kryo.readClassAndObject(input).asInstanceOf[scala.math.Ordering[Any]]
    val emptySet: MSSet[Any] = {
      try {
        val constructor =
          class2constuctor.getOrElse(typ, {
              val constr = typ.getDeclaredConstructor(classOf[scala.math.Ordering[?]])
              class2constuctor += typ -> constr
              constr
            })
        constructor.newInstance(setOrdering).asInstanceOf[MSSet[Any]].empty
      } catch {
        case _: Throwable => kryo.newInstance(typ).asInstanceOf[MSSet[Any]].empty
      }
    }

    if (len == 0) {
      emptySet
    } else {
      val builder = emptySet.sortedIterableFactory.newBuilder
      builder.sizeHint(len)
      var i = 0
      while (i < len) {
        builder.addOne(kryo.readClassAndObject(input))
        i += 1
      }
      builder.result()
    }
  }

  override def write(kryo: Kryo, output: Output, collection: MSSet[?]): Unit = {
    val len = collection.size
    output.writeInt(len, true)

    kryo.writeClassAndObject(output, collection.ordering)

    val it = collection.iterator
    while (it.hasNext) {
      kryo.writeClassAndObject(output, it.next())
    }
  }
}

class ScalaMutableSetSerializer extends Serializer[MSet[?]] {

  override def read(kryo: Kryo, input: Input, typ: Class[? <: MSet[?]]): MSet[?] = {
    val len = input.readInt(true)
    val set: MSet[Any] = kryo.newInstance(typ).asInstanceOf[MSet[Any]].empty
    set.sizeHint(len)
    var i = 0
    while (i < len) {
      set += kryo.readClassAndObject(input)
      i += 1
    }
    set
  }

  override def write(kryo: Kryo, output: Output, collection: MSet[?]): Unit = {
    output.writeInt(collection.size, true)
    val it = collection.iterator
    while (it.hasNext) {
      kryo.writeClassAndObject(output, it.next())
    }
  }
}
