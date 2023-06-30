package io.altoo.serialization.kryo.scala

import scala.util.Try

object ReflectionHelper {
  def getClassFor(fqcn: String, classLoader: ClassLoader): Try[Class[_ <: AnyRef]] =
    Try[Class[_ <: AnyRef]] {
      Class.forName(fqcn, false, classLoader).asInstanceOf[Class[_ <: AnyRef]]
    }
}
