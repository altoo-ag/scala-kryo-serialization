package io.altoo.serialization.kryo.scala

import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DefaultKeyProviderTest extends AnyFlatSpec with Matchers {

  behavior of "DefaultKeyProvider"

  private val keyProvider = new DefaultKeyProvider

  private def config(password: String, salt: String) =
    ConfigFactory.parseString(
      s"""
         |encryption.aes.password = "$password"
         |encryption.aes.salt = "$salt"
         |""".stripMargin,
    )

  it should "derive the same key for the same password and salt" in {
    val conf = config("test-password", "test-salt")

    val key1 = keyProvider.aesKey(conf)
    val key2 = keyProvider.aesKey(conf)

    key1 shouldEqual key2
  }

  it should "derive different keys for different salts" in {
    val key1 = keyProvider.aesKey(config("test-password", "salt-1"))
    val key2 = keyProvider.aesKey(config("test-password", "salt-2"))

    (key1 should not).equal(key2)
  }

  it should "derive different keys for different passwords" in {
    val key1 = keyProvider.aesKey(config("password-1", "test-salt"))
    val key2 = keyProvider.aesKey(config("password-2", "test-salt"))

    (key1 should not).equal(key2)
  }

  it should "derive a 256-bit AES key" in {
    val key = keyProvider.aesKey(config("test-password", "test-salt"))

    key.length shouldBe 32
  }
}
