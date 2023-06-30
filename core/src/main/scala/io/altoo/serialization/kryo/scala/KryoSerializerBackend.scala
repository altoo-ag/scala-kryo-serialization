package io.altoo.serialization.kryo.scala

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{ByteBufferInput, ByteBufferOutput, Input, Output}
import com.esotericsoftware.kryo.unsafe.{UnsafeInput, UnsafeOutput}
import org.slf4j.Logger

import java.nio.ByteBuffer
import scala.util.Success

private[kryo] class KryoSerializerBackend(val kryo: Kryo, val bufferSize: Int, val maxBufferSize: Int, val useManifest: Boolean, val useUnsafe: Boolean)(log: Logger,
                                                                                                                                                         classLoader: ClassLoader) {

  // "toBinary" serializes the given object to an Array of Bytes
  // Implements Serializer
  def toBinary(obj: AnyRef): Array[Byte] = {
    val buffer = output
    try {
      if (useManifest)
        kryo.writeObject(buffer, obj)
      else
        kryo.writeClassAndObject(buffer, obj)
      buffer.toBytes
    } catch {
      case e: StackOverflowError if !kryo.getReferences => // when configured with "nograph" serialization can fail with stack overflow
        log.error(s"Could not serialize class with potentially circular references: $classLoader", e)
        throw new RuntimeException("Could not serialize class with potential circular references: " + obj)
    } finally {
      buffer.reset()
    }
  }

  // Implements ByteBufferSerializer
  def toBinary(obj: AnyRef, buf: ByteBuffer): Unit = {
    val buffer = getOutput(buf)
    try {
      if (useManifest)
        kryo.writeObject(buffer, obj)
      else
        kryo.writeClassAndObject(buffer, obj)
      buffer.toBytes
    } catch {
      case e: StackOverflowError if !kryo.getReferences => // when configured with "nograph" serialization can fail with stack overflow
        log.error(s"Could not serialize class with potentially circular references: $obj", e)
        throw new RuntimeException("Could not serialize class with potential circular references: " + obj)
    }
  }

  // "fromBinary" deserializes the given array,
  // using the type hint (if any, see "includeManifest" above)
  // into the optionally provided classLoader.
  // Implements Serializer
  def fromBinary(bytes: Array[Byte], clazz: Option[Class[?]]): AnyRef = {
    val buffer = getInput(bytes)
    try {
      if (useManifest)
        clazz match {
          case Some(c) => kryo.readObject(buffer, c).asInstanceOf[AnyRef]
          case _       => throw new RuntimeException("Object of unknown class cannot be deserialized")
        }
      else
        kryo.readClassAndObject(buffer)
    } finally {
      buffer.close()
    }
  }

  // Implements ByteBufferSerializer
  def fromBinary(buf: ByteBuffer, manifest: Option[String]): AnyRef = {
    val buffer = getInput(buf)
    if (useManifest) {
      val clazz = manifest.flatMap(ReflectionHelper.getClassFor(_, classLoader).toOption)
      clazz match {
        case Some(c) => kryo.readObject(buffer, c).asInstanceOf[AnyRef]
        case _       => throw new RuntimeException("Object of unknown class cannot be deserialized")
      }
    } else
      kryo.readClassAndObject(buffer)
  }

  // Used by Serializer implementation
  private val output =
    if (useUnsafe)
      new UnsafeOutput(bufferSize, maxBufferSize)
    else
      new Output(bufferSize, maxBufferSize)

  // Used by ByteBufferSerializer implementation
  private def getOutput(buffer: ByteBuffer): Output =
    new ByteBufferOutput(buffer)

  // Used by Serializer implementation
  private def getInput(bytes: Array[Byte]): Input =
    if (useUnsafe)
      new UnsafeInput(bytes)
    else
      new Input(bytes)

  // Used by ByteBufferSerializer implementation
  private def getInput(buffer: ByteBuffer): Input =
    new ByteBufferInput(buffer)

}
