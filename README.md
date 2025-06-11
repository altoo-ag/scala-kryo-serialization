scala-kryo-serialization - kryo-based serializers for Scala
=====================================================================

Scala Kryo Serialization provides a convenient way of using Kryo with Scala and is the base for [Pekko Kryo Serialization](https://github.com/altoo-ag/pekko-kryo-serialization) providing the same functionality to pekko.

=====================================================================
[![Full test prior to release](https://github.com/altoo-ag/scala-kryo-serialization/actions/workflows/fullTest.yml/badge.svg)](https://github.com/altoo-ag/scala-kryo-serialization/actions/workflows/fullTest.yml)
[![Latest version](https://index.scala-lang.org/altoo-ag/scala-kryo-serialization/scala-kryo-serialization/latest.svg)](https://index.scala-lang.org/altoo-ag/scala-kryo-serialization/scala-kryo-serialization)

This library provides custom [Kryo](https://github.com/EsotericSoftware/kryo)-based serializers for Scala. See [Pekko Kryo Serialization](https://github.com/altoo-ag/pekko-kryo-serialization) for serialization in Pekko.

It can also be used for a general purpose and very efficient Kryo-based serialization
of such Scala types like Option, Tuple, Enumeration and most of Scala's collection types.


Features
--------

* It is more efficient than Java serialization - both in size and speed
* Does not require any additional build steps like compiling proto files, when using protobuf serialization
* Almost any Scala and Java class can be serialized using it without any additional configuration or code changes
* Efficient serialization of such Scala types like Option, Tuple, Enumeration, most of Scala's collection types
* Supports transparent AES encryption and different modes of compression
* Apache 2.0 license

Note that this serializer is mainly intended to be used for remoting and not for (long term) persisted data.
The underlying kryo serializer does not guarantee compatibility between major versions.


How to use this library in your project
---------------------------------------

To use this serializer, you need to do two things:

* Include a dependency on this library into your project:
  `libraryDependencies += "io.altoo" %% "scala-kryo-serialization" % "? not yet released"`

* Register and configure the serializer in your Typesafe Config configuration file, e.g. `application.conf`.

We provide several versions of the library:

| Version | Kryo Compatibility | Available Scala Versions | Tested with                                                         |
|---------|--------------------|--------------------------|---------------------------------------------------------------------|
| v1.3.x  | Kryo-5.6           | 2.12,2.13,3              | JDK: OpenJdk11,OpenJdk17,OpenJdk21     Scala: 2.12.20,2.13.16,3.3.5 |
| v1.2.x  | Kryo-5.6           | 2.12,2.13,3              | JDK: OpenJdk11,OpenJdk17,OpenJdk21     Scala: 2.12.20,2.13.16,3.3.4 |
| v1.1.x  | Kryo-5.5           | 2.12,2.13,3              | JDK: OpenJdk11,OpenJdk17,OpenJdk21     Scala: 2.12.18,2.13.11,3.3.1 |
| v1.0.x  | Kryo-5.4           | 2.12,2.13,3              | JDK: OpenJdk11,OpenJdk17               Scala: 2.12.18,2.13.11,3.3.1 |


Note that we use semantic versioning - see [semver.org](https://semver.org/).


#### sbt projects

To use the latest stable release of scala-kryo-serialization in sbt projects you just need to add
this dependency:

`libraryDependencies += "io.altoo" %% "scala-kryo-serialization" % "1.2.0"`

#### maven projects

To use the official release of scala-kryo-serialization in Maven projects, please use the following snippet in your pom.xml

```xml
    <repository>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
        <id>central</id>
        <name>Maven Central Repository</name>
        <url>https://repo1.maven.org/maven2</url>
    </repository>

    <dependency>
        <groupId>io.altoo</groupId>
        <artifactId>scala-kryo-serialization_2.13</artifactId>
        <version>1.2.0</version>
    </dependency>
```

For snapshots see [Snapshots.md](Snapshots.md)


Configuration of scala-kryo-serialization
----------------------------------------------

The following options are available for configuring this serializer:

* You can add a new `scala-kryo-serialization` section to the configuration to customize the serializer.
  Consult the supplied [reference.conf](https://github.com/altoo-ag/scala-kryo-serialization/blob/master/core/src/main/resources/reference.conf) for a detailed explanation of all the options available.

* Then you can create an instance of `ScalaKryoSerializer` and use it to serialize data.
  The serializer implements pooling to perform serialization across multiple threads.


How do you create mappings or classes sections with proper content?
-------------------------------------------------------------------

One of the easiest ways to understand which classes you need to register in those
sections is to leave both sections first empty and then set

    implicit-registration-logging = true

As a result, you'll eventually see log messages about implicit registration of
some classes. By default, they will receive some random default ids. Once you see
the names of implicitly registered classes, you can copy them into your mappings
or classes sections and assign an id of your choice to each of those classes.

You may need to repeat the process several times until you see no further log
messages about implicitly registered classes.

Another useful trick is to provide your own custom initializer for Kryo (see
below) and inside it, you registerclasses of a few objects that are typically
used by your application, for example:

```scala
    kryo.register(myObj1.getClass)
    kryo.register(myObj2.getClass)
```

Obviously, you can also explicitly assign IDs to your classes in the initializer,
if you wish:

```scala
    kryo.register(myObj3.getClass, 123)
```

If you use this library as an alternative serialization method when sending messages
between actors, it is extremely important that the order of class registration and
the assigned class IDs are the same for senders and for receivers!


How to customize kryo initialization
------------------------------------

To further customize kryo you can extend the `io.altoo.serialization.kryo.scala.DefaultKryoInitializer` and
configure the FQCN under `scala-kryo-serialization.kryo-initializer`.

#### Configuring default field serializers
In `preInit` a different default serializer can be configured
as it will be picked up by serializers added afterward.
By default, the `com.esotericsoftware.kryo.serializers.FieldSerializer` will be used.

The available options are:
* `com.esotericsoftware.kryo.serializers.FieldSerializer`<br/>
  Serializes objects using direct field assignment. FieldSerializer is generic
  and can serialize most classes without any configuration. It is efficient
  and writes only the field data, without any extra information. It does not
  support adding, removing, or changing the type of fields without invalidating
  previously serialized bytes. This can be acceptable in many situations,
  such as when sending data over a network, but may not be a good choice for
  long term data storage because the Java classes cannot evolve.

* `com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer`<br/>
  Serializes objects using direct field assignment, providing both forward and
  backward compatibility. This means fields can be added or removed without
  invalidating previously serialized bytes. Changing the type of a field
  is not supported. The forward and backward compatibility comes at a cost: the
  first time the class is encountered in the serialized bytes, a simple
  schema is written containing the field name strings.

* `com.esotericsoftware.kryo.serializers.VersionFieldSerializer`<br/>
  Serializes objects using direct field assignment, with versioning backward
  compatibility. Allows fields to have a @Since(int) annotation to indicate
  the version they were added. For a particular field, the value in @Since
  should never change once created. This is less flexible than FieldSerializer,
  which can handle most classes without needing annotations, but it provides
  backward compatibility. This means that new fields can be added, but
  removing, renaming or changing the type of any field will invalidate
  previous serialized bytes. VersionFieldSerializer has very little overhead
  (a single additional varint) compared to FieldSerializer. Forward
  compatibility is not supported.

* `com.esotericsoftware.kryo.serializers.TaggedFieldSerializer`<br/>
  Serializes objects using direct field assignment for fields that have
  a @Tag(int) annotation. This provides backward compatibility so new
  fields can be added. TaggedFieldSerializer has two advantages over
  VersionFieldSerializer:
    1) fields can be renamed
    2) fields marked with the @Deprecated annotation will be ignored when
       reading old bytes and won't be written to new bytes.

  Deprecation effectively removes the field from serialization, though
  the field and @Tag annotation must remain in the class. The downside is that
  it has a small amount of additional overhead compared to
  VersionFieldSerializer (additional per field variant). Forward compatibility
  is not supported.

### Example for configuring a different field serializer

Create a custom initializer

```scala
class XyzKryoInitializer extends DefaultKryoInitializer {
  def preInit(kryo: ScalaKryo): Unit = {
    kryo.setDefaultSerializer(classOf[com.esotericsoftware.kryo.serializers.TaggedFieldSerializer[_]])
  }
}
```

And register the custom initializer in your `application.conf` by overriding

    scala-kryo-serialization.kryo-initializer = "com.example.XyzKryoInitializer"

To configure the field serializer a serializer factory can be used as described here: https://github.com/EsotericSoftware/kryo#serializer-factories

How to configure and customize encryption
-----------------------------------------

Using the `DefaultKeyProvider` an encryption key can statically be set by defining `encryption.aes.password` and `encryption.aes.salt`.
Refere to the [reference.conf](https://github.com/altoo-ag/scala-kryo-serialization/blob/master/scala-kryo-serialization/src/main/resources/reference.conf) for an example configuration.

Sometimes you need to pass a custom aes key, depending on the context you are in,
instead of having a static key. For example, you might have the key in a data
store, or provided by some other application. In such instances, you might want
to provide the key dynamically to kryo serializer.

You can override the
```hocon
  encryption.aes.key-provider = "CustomKeyProviderFQCN"
```
Where `CustomKeyProviderFQCN` is a fully qualified class name of your custom aes key
provider class. The key provider must extend the `DefaultKeyProvider` and can override the `aesKey` method.

An example of such a custom aes-key supplier class could be something like this:

```scala
class CustomKeyProvider extends DefaultKeyProvider {
  override def aesKey(config: Config): String = "ThisIsASecretKey"
}
```

The encryption transformer (selected for `aes` in post serialization transformations) only
supports GCM modes (currently recommended default mode is `AES/GCM/NoPadding`).

Important: The old encryption transformer only supported CBC modes without manual authentication which is
deemed problematic. It is currently available for backwards compatibility by specifying `aesLegacy` in
post serialization transformations instead of `aes`. Its usage is deprecated and will be removed in future versions.


Resolving Subclasses
--------------------

If you are using `id-strategy="explicit"`, you may find that some of the standard Scala types are a bit hard to register properly.
This is because these types are exposed in the API as simple traits or abstract classes, but they are actually implemented as many
specialized subclasses that are used as necessary. Examples include:

* scala.collection.immutable.Map
* scala.collection.immutable.Set

The problem is that Kryo thinks in terms of the *exact* class being serialized, but you are
rarely working with the actual implementation class -- the application code only cares about
the more abstract trait. The implementation class often isn't obvious, and is sometimes
private to the library it comes from. This isn't an issue for idstrategies that add registrations
when needed, or which use the class name, but in `explicit` you must register every class to be
serialized, and that may turn out to be more than you expect.

For cases like these, you can use the `SubclassResolver`. This is a variant of the standard
Kryo ClassResolver, which is able to deal with subclasses of the registered types. You turn it
on by setting
```hocon
  resolve-subclasses = true
```
With that turned on, unregistered subclasses of a registered supertype are serialized as that
supertype. So for example, if you have registered `immutable.Set`, and the object being serialized
is actually an `immutable.Set.Set3` (the subclass used for Sets of 3 elements), it will serialize and
deserialize that as an `immutable.Set`.

If you register `immutable.Map`, you should use the `ScalaImmutableAbstractMapSerializer` with it.
If you register `immutable.Set`, you should use the `ScalaImmutableAbstractSetSerializer`. These
serializers are specifically designed to work with those traits.

The `SubclassResolver` approach should only be used in cases where the implementation types are completely
opaque, chosen by the implementation library, and not used explicitly in application code. If you have
subclasses that have their own distinct semantics, such as `immutable.ListMap`, you should register
those separately. You can register both a higher-level class like `immutable.Map` and a subclass
like `immutable.ListMap` -- the resolver will choose the more-specific one when appropriate.

`SubclassResolver` should be used with care -- even when it is turned on, you should define and
register most of your classes explicitly, as usual. But it is a helpful way to tame the complexity
of some class hierarchies, when that complexity can be treated as an implementation detail and all 
the subclasses can be serialized and deserialized identically.


Using serializers with different configurations
-----------------------------------------------

There may be the need to use different configurations for different use cases.
To support this the `KryoSerializer` can be extended to use a different configuration path.

Define a custom configuration:
```hocon
scala-kryo-serialization-xyz = ${scala-kryo-serialization} {
  # configuration overrides like...
  # id-strategy = "explicit"
}
```

Create new serializer subclass overriding the config key to the matching config section.
```scala
package xyz

class XyzKryoSerializer(config: Config, classLoader: ClassLoader) extends ScalaKryoSerializer(config, classLoader) {
  override def configKey: String = "scala-kryo-serialization-xyz"
}
```


Enum Serialization
------------------

Serialization of Java and Scala 3 enums is done by name (and not by index) to avoid having reordering of enum values breaking serialization.

Scala 3 `lazy val` Serialization Notice
---------------------------------------

When serializing objects that contain `lazy val`s in Scala 3, please be aware of the following behavior:

- Scala 3 implements `lazy val` using an internal state machine (`Uninitialized`, `Evaluating`, `Waiting`, `Initialized`).
- Kryo will only serialize the `lazy val` value if it has been fully initialized. Intermediate states (`Evaluating` or `Waiting`) will be treated as uninitialized (`null`) during serialization.
- As a result, after deserialization, the `lazy val` will be recomputed if it was not fully initialized during serialization.

**Implication:** If your object contains expensive or side-effecting `lazy val`s, they might be re-evaluated after deserialization unless they were fully initialized before serialization.

If this behavior is undesirable, consider explicitly evaluating such values before serialization, or avoid relying on `lazy val` in serialized objects.


Using Kryo on JDK 17 and later
------------------------------

Kryo needs modules to be opened for reflection when serializing basic JDK classes.
Those options have to be passed to the JVM, for example in sbt:
```sbt
javaOptions ++= Seq("--add-opens", "java.base/java.util=ALL-UNNAMED", "--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED", "--add-opens", "java.base/java.math=ALL-UNNAMED")
```

To use unsafe transformations, the following access must be granted:
```sbt
javaOptions ++= Seq("--add-opens", "java.base/java.nio=ALL-UNNAMED", "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
```

How do I build this library on my own?
--------------------------------------
If you wish to build the library on your own, you need to check out the project from GitHub and do
```
sbt compile publishM2
```

