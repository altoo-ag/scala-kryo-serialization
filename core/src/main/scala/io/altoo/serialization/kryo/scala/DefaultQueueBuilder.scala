package io.altoo.serialization.kryo.scala

import org.slf4j.{Logger, LoggerFactory}

import java.util
import java.util.concurrent.ArrayBlockingQueue
import scala.reflect.ClassTag

/**
 * Default queue builder that can be extended to use another type of queue.
 * Notice that it must be a multiple producer and multiple consumer queue type,
 * you could use for example a bounded non-blocking queue.
 *
 * Previous versions used org.agrona.concurrent.ManyToManyConcurrentArrayQueue - but hard to measure performance gain over ArrayBlockingQueue ->not worth the extra dependency.
 */
class DefaultQueueBuilder {
  // must have empty default constructor for backwards compatability

  protected val log: Logger = LoggerFactory.getLogger(getClass)

  @volatile
  private var _settings: KryoSerializationSettings = null

  /**
   * called before [[build]] to allow to configure the queue creation.
   */
  // TODO replace deferred config with initializing constructor in next major
  def configure(settings: KryoSerializationSettings): Unit = { _settings = settings }

  protected def calculateSize(): Int = {
    val key = "queue-size-limit"
    if (_settings.config.hasPath(key)) _settings.config.getInt(key)
    else Runtime.getRuntime.availableProcessors * 4
  }

  /**
   * Override to use a different queue.
   */
  def build[T: ClassTag]: util.Queue[T] = {
    val poolSize = calculateSize()
    log.debug("Initializing kryo serializer pool with size {}", poolSize)
    new ArrayBlockingQueue[T](poolSize)
  }
}
