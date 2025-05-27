package io.altoo.serialization.kryo.scala

import com.esotericsoftware.kryo.kryo5.{ClassResolver, ReferenceResolver}
import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer
import com.esotericsoftware.kryo.kryo5.util.{DefaultClassResolver, ListReferenceResolver, MapReferenceResolver}
import io.altoo.serialization.kryo.scala.serializer.*

import scala.util.{Failure, Success}

/**
 * Extensible strategy to configure and customize kryo instance.
 * Create a subclass of [[DefaultKryoInitializer]] and configure the FQCN under key kryo-initializer.
 */
class DefaultKryoInitializer {

  /**
   * Can be overridden to provide a custom reference resolver - override only if you know what you are doing!
   */
  def createReferenceResolver(settings: KryoSerializationSettings): ReferenceResolver = {
    if (settings.kryoReferenceMap) new MapReferenceResolver() else new ListReferenceResolver()
  }

  /**
   * Can be overridden to provide a custom class resolver - override only if you know what you are doing!
   */
  def createClassResolver(settings: KryoSerializationSettings): ClassResolver = {
    if (settings.idStrategy == "incremental") new KryoClassResolver(settings.implicitRegistrationLogging)
    else if (settings.resolveSubclasses) new SubclassResolver()
    else new DefaultClassResolver()
  }

  /**
   * Can be overridden to set a different field serializer before other serializer are initialized.
   * Note: register custom classes/serializer in `postInit`, otherwise default order might break.
   */
  def preInit(kryo: ScalaKryo): Unit = {
    kryo.setDefaultSerializer(classOf[com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer[?]])
  }

  /**
   * Registers serializer for standard/often used scala classes - override only if you know what you are doing!
   */
  def init(kryo: ScalaKryo): Unit = {
    initScalaSerializer(kryo)
  }

  /**
   * Can be overridden to register additional serializer and classes explicitly or reconfigure kryo.
   */
  def postInit(kryo: ScalaKryo): Unit = ()

  protected def initScalaSerializer(kryo: ScalaKryo): Unit = {
    // Support serialization of some standard or often used Scala classes
    kryo.addDefaultSerializer(classOf[scala.Enumeration#Value], classOf[EnumerationNameSerializer])
    ReflectionHelper.getClassFor("scala.Enumeration$Val", classOf[Enumeration].getClassLoader) match {
      case Success(clazz) => kryo.register(clazz)
      case Failure(e)     => throw e
    }
    kryo.register(classOf[scala.Enumeration#Value])

    // identity preserving serializers for Unit and BoxedUnit
    kryo.addDefaultSerializer(classOf[scala.runtime.BoxedUnit], classOf[ScalaUnitSerializer])

    // mutable maps
    kryo.addDefaultSerializer(classOf[scala.collection.mutable.Map[?, ?]], classOf[ScalaMutableMapSerializer])

    // immutable maps - specialized by mutable, immutable and sortable
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.SortedMap[?, ?]], classOf[ScalaSortedMapSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.Map[?, ?]], classOf[ScalaImmutableMapSerializer])

    // Sets - specialized by mutability and sortability
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.BitSet], classOf[FieldSerializer[scala.collection.immutable.BitSet]])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.SortedSet[?]], classOf[ScalaImmutableSortedSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.immutable.Set[?]], classOf[ScalaImmutableSetSerializer])

    kryo.addDefaultSerializer(classOf[scala.collection.mutable.BitSet], classOf[FieldSerializer[scala.collection.mutable.BitSet]])
    kryo.addDefaultSerializer(classOf[scala.collection.mutable.SortedSet[?]], classOf[ScalaMutableSortedSetSerializer])
    kryo.addDefaultSerializer(classOf[scala.collection.mutable.Set[?]], classOf[ScalaMutableSetSerializer])

    // Map/Set Factories
    ScalaVersionSerializers.mapAndSet(kryo)
    ScalaVersionSerializers.iterable(kryo)
    ScalaVersionSerializers.enums(kryo)
    // Scala 3 LazyVal Serializer
    ScalaVersionSerializers.lazyVal(kryo)
  }
}
