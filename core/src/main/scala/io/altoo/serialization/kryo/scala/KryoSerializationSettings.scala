package io.altoo.serialization.kryo.scala

import com.typesafe.config.Config

import scala.jdk.CollectionConverters.*

/**
 * INTERNAL API - api may change at any point in time
 * without any warning.
 */
class KryoSerializationSettings(val config: Config) {
  val serializerType: String = config.getString("type")

  val bufferSize: Int = config.getInt("buffer-size")
  val maxBufferSize: Int = config.getInt("max-buffer-size")

  // Each entry should be: FQCN -> integer id
  val classNameMappings: Map[String, String] = configToMap(config.getConfig("mappings"))
  val classNames: java.util.List[String] = config.getStringList("classes")

  // Strategy: default, explicit, incremental, automatic
  val idStrategy: String = config.getString("id-strategy")
  val implicitRegistrationLogging: Boolean = config.getBoolean("implicit-registration-logging")

  val kryoTrace: Boolean = config.getBoolean("kryo-trace")
  val kryoReferenceMap: Boolean = config.getBoolean("kryo-reference-map")
  val kryoInitializer: String = config.getString("kryo-initializer")

  val serializerCache: String = config.getString("serializer-cache")
  val queueBuilder: String = config.getString("queue-builder")

  val useUnsafe: Boolean = config.getBoolean("use-unsafe")

  val encryptionSettings: Option[EncryptionSettings] = if (config.hasPath("encryption")) Some(new EncryptionSettings(config)) else None

  val postSerTransformations: String = config.getString("post-serialization-transformations")
  val resolveSubclasses: Boolean = config.getBoolean("resolve-subclasses")
  val noResolveReferenceClasses: Set[String] = config.getStringList("no-resolve-reference-classes").asScala.toSet

  private def configToMap(cfg: Config): Map[String, String] =
    cfg.root.unwrapped.asScala.toMap.map { case (k, v) => (k, v.toString) }
}

/**
 * INTERNAL API - api may change at any point in time
 * without any warning.
 */
class EncryptionSettings(val config: Config) {
  val keyProvider: String = config.getString("encryption.aes.key-provider")
  val aesMode: String = config.getString("encryption.aes.mode")
  val aesIvLength: Int = config.getInt("encryption.aes.iv-length")
}
