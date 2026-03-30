package io.altoo.serialization.kryo.scala

import org.slf4j.LoggerFactory

import java.util
import scala.util.{Failure, Success}

/**
 * Fixed pool of serializer instances.
 * Choose the [[SerializerPool]] when the number of threads accessing is high (to save memory) especially with high numbers of different classes to serialize
 * otherwise prefer the [[ThreadLocalSerializerPool]].
 */
private[kryo] class SerializerPool(settings: KryoSerializationSettings, classLoader: ClassLoader, newInstance: () => KryoSerializerBackend)
    extends AbstractSerializerPool(settings, classLoader, newInstance) {
  private val log = LoggerFactory.getLogger(getClass)

  private val pool: util.Queue[KryoSerializerBackend] = {
    val queueBuilder = queueBuilderClass.getDeclaredConstructor().newInstance()
    queueBuilder.configure(settings)
    queueBuilder.build[KryoSerializerBackend]
  }

  override def fetch(): KryoSerializerBackend = {
    pool.poll() match {
      case null =>
        log.debug("Create new serializer since no serializer in pool")
        newInstance()
      case o => o
    }
  }

  override def release(o: KryoSerializerBackend): Unit = {
    val stored = pool.offer(o)
    if (!stored) {
      log.debug("Dispose serializer since pool full")
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
