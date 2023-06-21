
//see pekko https://github.com/apache/incubator-pekko/blob/bd1af56fcead435cb3d730e759223dcb2b0fb4c6/project/plugins.sbt#L18
// sbt-osgi 0.9.5 is available but breaks including jdk9-only classes
// sbt-osgi 0.9.6 is available but breaks populating pekko-protobuf-v3
addSbtPlugin("com.typesafe.sbt" % "sbt-osgi" % "0.9.4")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.21")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")
