package io.altoo.serialization.kryo.scala

import scala.util.Try

object ReflectionHelper {
  def getClassFor(fqcn: String, classLoader: ClassLoader): Try[Class[? <: AnyRef]] =
    Try[Class[? <: AnyRef]] {
      Class.forName(fqcn, false, classLoader).asInstanceOf[Class[? <: AnyRef]]
    }
}
