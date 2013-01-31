name := "Uniscala Couch"

organization := "net.uniscala"

version := "0.4-SNAPSHOT"

description := "A couchdb driver for Scala, but not an object mapper."

startYear := Some(2013)

homepage := Some(url("https://github.com/stainsby/uniscala-couch"))

organizationName := "Sustainable Software Pty Ltd"

organizationHomepage := Some(url("http://www.sustainablesoftware.com.au/"))

licenses := Seq(
  ("The Apache Software License, Version 2.0" ->
    url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
)

scmInfo := Some(
  ScmInfo(
    url("https://github.com/stainsby/uniscala-couch"),
    "git@github.com:stainsby/uniscala-couch.git"
  )
)

pomExtra := (
  <developers>
    <developer>
      <id>stainsby</id>
      <name>Sam Stainsby</name>
      <email>sam@sustainablesoftware.com.au</email>
    </developer>
  </developers>
)

scalaVersion := "2.10.0"

scalacOptions <<= scalaVersion map { v: String =>
  val default = "-deprecation" :: "-unchecked" :: Nil
  if (v.startsWith("2.9.")) default else
    default ++ ("-feature" :: "-language:implicitConversions" :: Nil)
}

libraryDependencies ++= Seq(
  "io.netty" % "netty" % "4.0.0.Alpha8",
  "net.uniscala" %% "uniscala-json" % "0.3",
  "org.specs2" %% "specs2" % "1.13" % "test"
)

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

publishMavenStyle := true

publishArtifact in Test := false

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
