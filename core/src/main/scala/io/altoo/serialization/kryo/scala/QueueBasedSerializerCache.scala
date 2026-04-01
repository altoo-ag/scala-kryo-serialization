package io.altoo.serialization.kryo.scala

import org.slf4j.LoggerFactory

import java.util
import java.util.concurrent.atomic.AtomicInteger
import scala.util.{Failure, Success}

/**
 * Fixed pool of serializer instances.
 * Choose the [[QueueBasedSerializerCache]] when the number of threads accessing is high (to save memory) especially with high numbers of different classes to serialize
 * otherwise prefer the [[ThreadLocalSerializerCache]].
 * Can temporarily create more serializers than the underlying queue can store so that this never blocks.
 */
private[kryo] class QueueBasedSerializerCache(settings: KryoSerializationSettings, classLoader: ClassLoader, newInstance: () => KryoSerializerBackend)
    extends AbstractSerializerCache(settings, classLoader, newInstance) {
  private val log = LoggerFactory.getLogger(getClass)
  private val serializersAliveCount = new AtomicInteger(0) // counting here is much more efficient than traversing queue...

  private val pool: util.Queue[KryoSerializerBackend] = {
    val queueBuilder = queueBuilderClass.getDeclaredConstructor().newInstance()
    queueBuilder.configure(settings)
    queueBuilder.build[KryoSerializerBackend]
  }

  override def fetch(): KryoSerializerBackend = {
    pool.poll() match {
      case null =>
        if (log.isDebugEnabled()) {
          val serializersAlive = serializersAliveCount.incrementAndGet()
          log.debug("Create new serializer since no serializer in pool. Serializers alive:{}", serializersAlive)
        }
        newInstance()
      case o => o
    }
  }

  override def release(o: KryoSerializerBackend): Unit = {
    val stored = pool.offer(o)
    if (!stored && log.isDebugEnabled()) {
      val serializersAlive = serializersAliveCount.decrementAndGet()
      log.debug("Dispose serializer since pool is full/not accepting. Serializers alive:{}", serializersAlive)
    }
  }

  private def queueBuilderClass: Class[? <: DefaultQueueBuilder] =
    ReflectionHelper.getClassFor(settings.queueBuilder, classLoader) match {
      case Success(clazz) if classOf[DefaultQueueBuilder].isAssignableFrom(clazz) => clazz.asSubclass(classOf[DefaultQueueBuilder])
      case Success(clazz)                                                         =>
        log.error("Configured class {} does not extend DefaultQueueBuilder", clazz)
        throw new IllegalStateException(s"Configured class $clazz does not extend DefaultQueueBuilder")
      case Failure(e) =>
        log.error("Class could not be loaded: {} ", settings.queueBuilder)
        throw e
    }
}
