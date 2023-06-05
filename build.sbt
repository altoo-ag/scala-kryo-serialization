import sbt._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._


// Basics
val apacheSnapshot = "Apache Snapshots".at("https://repository.apache.org/content/repositories/snapshots/")
val sonatypeSnapshot = "Sonatype Snapshots Repository" at "https://oss.sonatype.org/content/repositories/snapshots/"
//todo resolvers += Resolver.jcenterRepo
//todo resolvers += Resolver.ApacheMavenSnapshotsRepo

// note: keep in sync to pekko https://github.com/apache/incubator-pekko/blob/main/project/Dependencies.scala
val mainScalaVersion = "3.3.0"
val secondayScalaVersions = Seq("2.12.18", "2.13.11")

val kryoVersion = "5.4.0"
val defaultPekkoVersion = "0.0.0+26656-898c6970-SNAPSHOT"
val pekkoVersion =
  System.getProperty("pekko.build.version", defaultPekkoVersion) match {
    case "default" => defaultPekkoVersion
    case x => x
  }

enablePlugins(SbtOsgi, ReleasePlugin)
addCommandAlias("validatePullRequest", ";+test")


// Projects
lazy val root: Project = project.in(file("."))
    .settings(Test / parallelExecution := false)
    .settings(commonSettings)
    .settings(name := "pekko-kryo-serialization")
    .settings(releaseProcess := releaseSettings)
    .settings(publish / skip := true)
    .settings(OsgiKeys.privatePackage := Nil)
    .settings(OsgiKeys.exportPackage := Seq("io.altoo.*"))
    .aggregate(core, typed)

lazy val core: Project = Project("pekko-kryo-serialization", file("pekko-kryo-serialization"))
    .settings(moduleSettings)
    .settings(description := "pekko-serialization implementation using kryo - core implementation")
    .settings(libraryDependencies ++= coreDeps ++ testingDeps)
    .settings(Compile / unmanagedSourceDirectories += {
      scalaBinaryVersion.value match {
        case "2.12" => baseDirectory.value / "src" / "main" / "scala-2.12"
        case "2.13" => baseDirectory.value / "src" / "main" / "scala-2.13"
        case _ => baseDirectory.value / "src" / "main" / "scala-3"
      }
    })
    .settings(Test / unmanagedSourceDirectories += {
      scalaBinaryVersion.value match {
        case "2.12" => baseDirectory.value / "src" / "test" / "scala-2.12"
        case "2.13" => baseDirectory.value / "src" / "test" / "scala-2.13"
        case _ => baseDirectory.value / "src" / "test" / "scala-3"
      }
    })

lazy val typed: Project = Project("pekko-kryo-serialization-typed", file("pekko-kryo-serialization-typed"))
    .settings(moduleSettings)
    .settings(description := "pekko-serialization implementation using kryo - extension including serialization for pekko-typed")
    .settings(libraryDependencies ++= typedDeps ++ testingDeps)
    .dependsOn(core)


// Dependencies
lazy val coreDeps = Seq(
  "com.esotericsoftware" % "kryo" % kryoVersion,
  ("org.apache.pekko" %% "pekko-actor" % pekkoVersion).cross(CrossVersion.for3Use2_13),
  "org.agrona" % "agrona" % "1.15.1", // should match pekko-remote/aeron inherited version
  "org.lz4" % "lz4-java" % "1.8.0",
  "commons-io" % "commons-io" % "2.11.0" % Test,
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.9.0"
)
lazy val typedDeps = Seq(
  "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
  "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test
).map(_.cross(CrossVersion.for3Use2_13))

lazy val testingDeps = Seq(
  "org.scalatest" %% "scalatest" % "3.2.14" % Test,
  "ch.qos.logback" % "logback-classic" % "1.2.11" % Test,
  ("org.apache.pekko" %% "pekko-testkit" % pekkoVersion % Test).cross(CrossVersion.for3Use2_13),
  ("org.apache.pekko" %% "pekko-persistence" % pekkoVersion % Test).cross(CrossVersion.for3Use2_13)
)


// Settings
lazy val commonSettings: Seq[Setting[_]] = Seq(
  organization := "io.altoo",
  resolvers += apacheSnapshot,
  resolvers += sonatypeSnapshot
)

lazy val moduleSettings: Seq[Setting[_]] = commonSettings ++ noReleaseInSubmoduleSettings ++ scalacBasicOptions ++ scalacStrictOptions ++ scalacLintOptions ++ Seq(
  scalaVersion := mainScalaVersion,
  versionScheme := Some("early-semver"),
  crossScalaVersions := (scalaVersion.value +: secondayScalaVersions),
  fork := true,
  testForkedParallel := false,
  classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.ScalaLibrary,
  run / javaOptions += "-XX:+UseAES -XX:+UseAESIntrinsics", //Enabling hardware AES support if available
  // required to run serialization with JDK 17
  Test / javaOptions ++= Seq("--add-opens", "java.base/java.util=ALL-UNNAMED", "--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED", "--add-opens", "java.base/java.math=ALL-UNNAMED"),
  // required to run unsafe with JDK 17
  Test / javaOptions ++= Seq("--add-opens", "java.base/java.nio=ALL-UNNAMED", "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED"),
  pomExtra := pomExtras,
  publishTo := sonatypePublishToBundle.value,
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ => false }
)

lazy val scalacBasicOptions = Seq(
  scalacOptions ++= {
    scalaBinaryVersion.value match {
      case "2.12" | "2.13" =>
        Seq(
          "-encoding", "utf8",
          "-feature",
          "-unchecked",
          "-deprecation",
          "-language:existentials",
          "-Xlog-reflective-calls",
          "-Ywarn-unused:-nowarn",
          "-opt:l:inline",
          "-opt-inline-from:io.altoo.pekko.serialization.kryo.*"
        )
      case "3" =>
        Seq(
          "-encoding", "utf8",
          "-feature",
          "-unchecked",
          "-deprecation",
          "-language:existentials"
        )
    }
  }
)


// strict options
lazy val scalacStrictOptions = Seq(
  scalacOptions ++= {
    scalaBinaryVersion.value match {
      case "2.12" =>
        Seq(
          "-Xfatal-warnings",
          "-Yno-adapted-args",
          "-Ywarn-adapted-args",
          "-Ywarn-dead-code",
          "-Ywarn-extra-implicit",
          "-Ywarn-inaccessible",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit",
          "-Ywarn-unused:-explicits,-implicits,_"
        )
      case "2.13" =>
        Seq(
          "-Werror",
          "-Wdead-code",
          "-Wextra-implicit",
          "-Wunused:imports",
          "-Wunused:patvars",
          "-Wunused:privates",
          "-Wunused:locals",
          //"-Wunused:params", enable once 2.12 support is dropped
          "-Wunused:nowarn",
        )
      case "3" =>
        Seq(
          //"-Xfatal-warnings", enable once dotty supports @nowarn
          "-Ycheck-all-patmat"
        )
    }
  }
)

// lint options
lazy val scalacLintOptions = Seq(
  scalacOptions ++= {
    scalaBinaryVersion.value match {
      case "2.12" =>
        Seq(
          "-Xlint:private-shadow",
          "-Xlint:type-parameter-shadow",
          "-Xlint:adapted-args",
          "-Xlint:unsound-match",
          "-Xlint:option-implicit"
        )
      case "2.13" =>
        Seq(
          "-Xlint:inaccessible",
          "-Xlint:nullary-unit",
          "-Xlint:private-shadow",
          "-Xlint:type-parameter-shadow",
          "-Xlint:adapted-args",
          "-Xlint:option-implicit",
          "-Xlint:missing-interpolator",
          "-Xlint:poly-implicit-overload",
          "-Xlint:option-implicit",
          "-Xlint:package-object-classes",
          "-Xlint:constant",
          "-Xlint:nonlocal-return",
          "-Xlint:valpattern",
          "-Xlint:eta-zero",
          "-Xlint:deprecation"
        )
      case "3" =>
        Seq()
    }
  }
)

lazy val noReleaseInSubmoduleSettings: Seq[Setting[_]] = Seq(
  releaseProcess := Seq[ReleaseStep](ReleaseStep(_ => sys.error("cannot release a submodule!")))
)


// Configure cross builds.
lazy val releaseSettings = Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  //do these manually on checked out tag... verify on https://oss.sonatype.org/#stagingRepositories
  //  releaseStepCommandAndRemaining("+publishSigned"),
  //  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
releaseCrossBuild := true

lazy val pomExtras = <url>https://github.com/altoo-ag/pekko-kryo-serialization</url>
    <licenses>
      <license>
        <name>The Apache Software License, Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:altoo-ag/pekko-kryo-serialization.git</url>
      <connection>scm:git:git@github.com:altoo-ag/pekko-kryo-serialization.git</connection>
    </scm>
    <developers>
      <developer>
        <id>danischroeter</id>
        <name>Daniel Schröter</name>
        <email>dsc@scaling.ch</email>
      </developer>
      <developer>
        <id>nvollmar</id>
        <name>Nicolas Vollmar</name>
        <email>nvo@scaling.ch</email>
      </developer>
    </developers>
