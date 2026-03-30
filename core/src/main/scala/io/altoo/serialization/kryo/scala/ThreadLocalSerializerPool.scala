package io.altoo.serialization.kryo.scala

import org.slf4j.LoggerFactory

/**
 * Thread local based serializer pool.
 */
private[kryo] class ThreadLocalSerializerPool(settings: KryoSerializationSettings, classLoader: ClassLoader, newInstance: () => KryoSerializerBackend)
    extends AbstractSerializerPool(settings, classLoader, newInstance) {
  private val log = LoggerFactory.getLogger(getClass)

  private val serializers = new ThreadLocal[KryoSerializerBackend]() {
    override def initialValue(): KryoSerializerBackend = {
      log.debug("Create new serializer instance for thread")
      newInstance()
    }
  }

  override def fetch(): KryoSerializerBackend = serializers.get()

  override def release(o: KryoSerializerBackend): Unit = {
    // no op - the serializer is kept assigned to this thread
  }
}
