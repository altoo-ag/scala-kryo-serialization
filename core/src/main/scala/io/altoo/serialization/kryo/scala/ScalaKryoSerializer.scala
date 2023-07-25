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
  def serialize(obj: Any): Try[Array[Byte]] = Try(toBinaryInternal(obj))

  def serialize(obj: Any, buf: ByteBuffer): Try[Unit] = Try(toBinaryInternal(obj, buf))

  def deserialize[T](bytes: Array[Byte]): Try[T] = Try(fromBinaryInternal(bytes, None).asInstanceOf[T])

  def deserialize[T](buf: ByteBuffer): Try[T] = Try(fromBinaryInternal(buf, None).asInstanceOf[T])
}
