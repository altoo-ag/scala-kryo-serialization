package io.altoo.serialization.kryo.scala

import org.agrona.concurrent.ManyToManyConcurrentArrayQueue

import java.util
import scala.reflect.ClassTag

/**
 * Default queue builder that can be extended to use another type of queue.
 * Notice that it must be a multiple producer and multiple consumer queue type,
 * you could use for example a bounded non-blocking queue.
 */
class DefaultQueueBuilder {

  /**
   * Override to use a different queue.
   */
  def build[T: ClassTag]: util.Queue[T] = new ManyToManyConcurrentArrayQueue[T](Runtime.getRuntime.availableProcessors * 4)
}
