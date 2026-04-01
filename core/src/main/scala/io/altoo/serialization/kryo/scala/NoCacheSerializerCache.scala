package io.altoo.serialization.kryo.scala

/**
 * Always create a new serializer for every operation.
 */
private[kryo] class NoCacheSerializerCache(settings: KryoSerializationSettings, classLoader: ClassLoader, newInstance: () => KryoSerializerBackend)
    extends AbstractSerializerCache(settings, classLoader, newInstance) {

  override def fetch(): KryoSerializerBackend = newInstance()

  override def release(o: KryoSerializerBackend): Unit = {
    // no op - the serializer is disposed
  }
}
