package io.altoo.serialization.kryo.scala

import com.typesafe.config.Config

/**
 * Returns a SerializerPool, useful to reduce GC overhead.
 *
 * @param queueBuilder queue builder.
 * @param newInstance  Serializer instance builder.
 */
private[kryo] class SerializerPool(config: Config, queueBuilder: DefaultQueueBuilder, newInstance: () => KryoSerializerBackend) {

  private val pool = {
    queueBuilder.configure(config)
    queueBuilder.build[KryoSerializerBackend]
  }

  def fetch(): KryoSerializerBackend = {
    pool.poll() match {
      case null => newInstance()
      case o    => o
    }
  }

  def release(o: KryoSerializerBackend): Unit = {
    pool.offer(o)
  }
}