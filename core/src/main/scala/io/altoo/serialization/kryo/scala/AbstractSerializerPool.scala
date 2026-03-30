package io.altoo.serialization.kryo.scala

import com.typesafe.config.Config
import org.slf4j.LoggerFactory

/**
 * Maintains kryo serializer instances since they are expensive to create.
 * Highly concurrent access to the pool - the client uses [[fetch]] and after usage [[release]] for thread exclusive usage of [[KryoSerializerBackend]]
 */
abstract class AbstractSerializerPool(config: KryoSerializationSettings, classLoader: ClassLoader, newInstance: () => KryoSerializerBackend) {
  protected val log = LoggerFactory.getLogger(getClass)

  def fetch(): KryoSerializerBackend

  def release(o: KryoSerializerBackend): Unit
}
