package io.altoo.serialization.kryo.scala

import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import java.util
import java.util.concurrent.ArrayBlockingQueue
import scala.reflect.ClassTag

/**
 * Default queue builder that can be extended to use another type of queue.
 * Notice that it must be a multiple producer and multiple consumer queue type,
 * you could use for example a bounded non-blocking queue.
 *
 * Previous versions used org.agrona.concurrent.ManyToManyConcurrentArrayQueue - but hard to measure performance gain over ConcurrentLinkedQueue is marginal and not worth the extra dependency.
 */
class DefaultQueueBuilder {
  // must have empty default constructor for backwards compatability

  protected val log = LoggerFactory.getLogger(getClass)

  @volatile
  private var _config: Config = null

  /**
   * called before [[build]] to allow to configure the queue creation.
   */
  def configure(config: Config): Unit = { _config = config }

  protected def calculateSize(): Int = {
    val key = "scala-kryo-serialization.queue-size-limit"
    if (_config.hasPath(key)) _config.getInt(key)
    else Runtime.getRuntime.availableProcessors * 4
  }

  /**
   * Override to use a different queue.
   */
  def build[T: ClassTag]: util.Queue[T] = {
    val poolSize = calculateSize()
    log.debug("initializing serializer pool with size {}", poolSize)
    new ArrayBlockingQueue[T](poolSize)
  }
}
