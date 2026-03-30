package io.altoo.serialization.kryo.scala

import com.typesafe.config.Config
import org.slf4j.LoggerFactory

/**
 * Returns a SerializerPool, useful to reduce GC overhead.
 *
 * @param queueBuilder queue builder.
 * @param newInstance  Serializer instance builder.
 */
private[kryo] class SerializerPool(config: Config, queueBuilder: DefaultQueueBuilder, newInstance: () => KryoSerializerBackend) {
  private val log = LoggerFactory.getLogger(getClass)

  private val pool = {
    queueBuilder.configure(config)
    queueBuilder.build[KryoSerializerBackend]
  }

  def fetch(): KryoSerializerBackend = {
    pool.poll() match {
      case null =>
        log.debug("create new serializer since no serializer in pool")
        newInstance()
      case o    => o
    }
  }

  def release(o: KryoSerializerBackend): Unit = {
    val stored = pool.offer(o)
    if (!stored) {
      log.debug("dispose serializer since pool full")
    }
  }
}