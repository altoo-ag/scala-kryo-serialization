import sbt._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

// Basics

// note: keep in sync to pekko https://github.com/apache/incubator-pekko/blob/main/project/Dependencies.scala
val mainScalaVersion = "3.3.5"
val secondaryScalaVersions = Seq("2.12.20", "2.13.16")

val kryoVersion = "5.6.2"
enablePlugins(ReleasePlugin)
addCommandAlias("validatePullRequest", ";+test")

lazy val root: Project = project.in(file("."))
  .settings(Test / parallelExecution := false)
  .settings(commonSettings)
  .settings(name := "scala-kryo-serialization")
  .settings(releaseProcess := releaseSettings)
  .settings(publish / skip := true)
  .aggregate(core)

lazy val core: Project = project.in(file("core"))
  .settings(moduleSettings)
  .settings(description := "pekko-serialization implementation using kryo - core implementation")
  .settings(name := "scala-kryo-serialization")
  .settings(libraryDependencies ++= coreDeps ++ testingDeps)
  .settings(Compile / unmanagedSourceDirectories += {
    scalaBinaryVersion.value match {
      case "2.12" => baseDirectory.value / "src" / "main" / "scala-2.12"
      case "2.13" => baseDirectory.value / "src" / "main" / "scala-2.13"
      case _      => baseDirectory.value / "src" / "main" / "scala-3"
    }
  })
  .settings(Test / unmanagedSourceDirectories += {
    scalaBinaryVersion.value match {
      case "2.12" => baseDirectory.value / "src" / "test" / "scala-2.12"
      case "2.13" => baseDirectory.value / "src" / "test" / "scala-2.13"
      case _      => baseDirectory.value / "src" / "test" / "scala-3"
    }
  })

// Dependencies
lazy val coreDeps = Seq(
  "com.esotericsoftware.kryo" % "kryo5" % kryoVersion,
  "com.typesafe" % "config" % "1.4.3",
  "org.lz4" % "lz4-java" % "1.8.0",
  "org.agrona" % "agrona" % "1.22.0", // should match pekko-remote/aeron inherited version
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.12.0",
  "org.slf4j" % "slf4j-api" % "2.0.16",
  "org.slf4j" % "log4j-over-slf4j" % "2.0.16")

lazy val testingDeps = Seq(
  "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  "ch.qos.logback" % "logback-classic" % "1.5.16" % Test)

// Settings
lazy val commonSettings: Seq[Setting[?]] = Seq(
  organization := "io.altoo",
)

lazy val moduleSettings: Seq[Setting[?]] = commonSettings ++ noReleaseInSubmoduleSettings ++ scalacBasicOptions ++ scalacStrictOptions ++ scalacLintOptions ++ Seq(
  scalaVersion := mainScalaVersion,
  versionScheme := Some("early-semver"),
  crossScalaVersions := (scalaVersion.value +: secondaryScalaVersions),
  fork := true,
  testForkedParallel := false,
  classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.ScalaLibrary,
  run / javaOptions += "-XX:+UseAES -XX:+UseAESIntrinsics", // Enabling hardware AES support if available
  // required to run serialization with JDK 17
  Test / javaOptions ++= Seq("--add-opens", "java.base/java.util=ALL-UNNAMED", "--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED",
    "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED", "--add-opens", "java.base/java.math=ALL-UNNAMED"),
  // required to run unsafe with JDK 17
  Test / javaOptions ++= Seq("--add-opens", "java.base/java.nio=ALL-UNNAMED", "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED"),
  pomExtra := pomExtras,
  publishTo := sonatypePublishToBundle.value,
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ => false })

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
          "-Xsource:3",
          "-opt:l:inline",
          "-opt-inline-from:io.altoo.pekko.serialization.kryo.*")
      case "3" =>
        Seq(
          "-encoding", "utf8",
          "-feature",
          "-unchecked",
          "-deprecation",
          "-language:existentials")
    }
  })

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
          "-Ywarn-unused:-explicits,-implicits,_")
      case "2.13" =>
        Seq(
          "-Werror",
          "-Wdead-code",
          "-Wextra-implicit",
          "-Wunused:imports",
          "-Wunused:patvars",
          "-Wunused:privates",
          "-Wunused:locals",
          // "-Wunused:params", enable once 2.12 support is dropped
        )
      case "3" =>
        Seq(
          // "-Xfatal-warnings", enable once dotty supports @nowarn
          "-Ycheck-all-patmat"
        )
    }
  })

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
          "-Xlint:option-implicit")
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
          "-Xlint:deprecation")
      case "3" =>
        Seq()
    }
  })

lazy val noReleaseInSubmoduleSettings: Seq[Setting[?]] = Seq(
  releaseProcess := Seq[ReleaseStep](ReleaseStep(_ => sys.error("cannot release a submodule!"))))

// Configure cross builds.
lazy val releaseSettings = Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  // do these manually on checked out tag... verify on https://oss.sonatype.org/#stagingRepositories
  //  releaseStepCommandAndRemaining("+publishSigned"),
  //  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges)
releaseCrossBuild := true

lazy val pomExtras = <url>https://github.com/altoo-ag/scala-kryo-serialization</url>
  <licenses>
    <license>
      <name>The Apache Software License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:altoo-ag/scala-kryo-serialization.git</url>
    <connection>scm:git:git@github.com:altoo-ag/scala-kryo-serialization.git</connection>
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
