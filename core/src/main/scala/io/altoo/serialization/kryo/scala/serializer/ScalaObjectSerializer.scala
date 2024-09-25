/**
 * *****************************************************************************
 * Copyright 2014 Roman Levenstein
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

import _root_.java.lang.reflect.Field
import scala.collection.mutable.Map as MMap
import scala.util.control.Exception.allCatch

// Stolen with pride from Chill ;-)
class ScalaObjectSerializer[T] extends Serializer[T] {
  private val cachedObj = MMap[Class[?], Option[T]]()

  // NOTE: even if a standalone or companion Scala object contains mutable
  // fields, the fact that there is only one of them in a process means that
  // we don't want to make a copy, so this serializer's type is treated as
  // always being immutable.
  override def isImmutable: Boolean = true

  // Does nothing
  override def write(kser: Kryo, out: Output, obj: T): Unit = ()

  protected def createSingleton(cls: Class[?]): Option[T] = {
    moduleField(cls).map { _.get(null).asInstanceOf[T] }
  }

  protected def cachedRead(cls: Class[?]): Option[T] = {
    cachedObj.synchronized { cachedObj.getOrElseUpdate(cls, createSingleton(cls)) }
  }

  override def read(kser: Kryo, in: Input, cls: Class[? <: T]): T = cachedRead(cls).get

  def accepts(cls: Class[?]): Boolean = cachedRead(cls).isDefined

  protected def moduleField(klass: Class[?]): Option[Field] =
    Some(klass)
      .filter { _.getName.last == '$' }
      .flatMap { k => allCatch.opt(k.getDeclaredField("MODULE$")) }
}
