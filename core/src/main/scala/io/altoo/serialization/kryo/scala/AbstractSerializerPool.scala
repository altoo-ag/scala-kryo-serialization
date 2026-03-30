package io.altoo.serialization.kryo.scala

/**
 * Maintains kryo serializer instances since they are expensive to create.
 * Highly concurrent access to the pool - the client uses [[fetch]] and after usage [[release]] for thread exclusive usage of [[KryoSerializerBackend]]
 */
private[kryo] abstract class AbstractSerializerPool(config: KryoSerializationSettings, classLoader: ClassLoader, newInstance: () => KryoSerializerBackend) {

  def fetch(): KryoSerializerBackend

  def release(o: KryoSerializerBackend): Unit
}
