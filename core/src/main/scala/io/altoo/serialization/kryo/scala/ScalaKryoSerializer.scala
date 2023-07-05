package io.altoo.serialization.kryo.scala

import com.typesafe.config.Config

import java.nio.ByteBuffer
import scala.util.Try

/**
 * Plain Scala serializer backed by Kryo.
 * Implements pooling of serialization backends and only one instance should be held.
 */
class ScalaKryoSerializer(config: Config, classLoader: ClassLoader) extends KryoSerializer(config, classLoader) {
  override protected def configKey: String = "scala-kryo-serialization"
  override protected[kryo] final def useManifest: Boolean = false

  protected[kryo] def prepareKryoInitializer(initializer: DefaultKryoInitializer): Unit = ()

  // serialization api
  def serialize(obj: AnyRef): Try[Array[Byte]] = Try(toBinary(obj))

  def serialize(obj: AnyRef, buf: ByteBuffer): Try[Unit] = Try(toBinary(obj, buf))

  def deserialize[T](bytes: Array[Byte]): Try[T] = Try(fromBinary(bytes, None).asInstanceOf[T])

  def deserialize[T](buf: ByteBuffer): Try[T] = Try(fromBinary(buf, None).asInstanceOf[T])

}
