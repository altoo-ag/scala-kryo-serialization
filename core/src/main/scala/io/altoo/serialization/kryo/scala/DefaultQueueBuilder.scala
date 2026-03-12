package io.altoo.serialization.kryo.scala

import java.util
import java.util.concurrent.ConcurrentLinkedQueue
import scala.reflect.ClassTag

/**
 * Default queue builder that can be extended to use another type of queue.
 * Notice that it must be a multiple producer and multiple consumer queue type,
 * you could use for example a bounded non-blocking queue.
 *
 * Previous versions used org.agrona.concurrent.ManyToManyConcurrentArrayQueue - but hard to measure performance gain over ConcurrentLinkedQueue is marginal and not worth the extra dependency.
 */
class DefaultQueueBuilder {

  /**
   * Override to use a different queue.
   */
  def build[T: ClassTag]: util.Queue[T] = new ConcurrentLinkedQueue[T]()
}
