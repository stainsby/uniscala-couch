name := "Uniscala Couch"

organization := "net.uniscala"

version := "0.2-SNAPSHOT"

description := "A couchdb driver for Scala."

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

scalacOptions := List("-deprecation", "-feature")

libraryDependencies ++= Seq(
  "io.netty" % "netty" % "4.0.0.Alpha8",
  "net.uniscala" %% "uniscala-json" % "0.3",
  "org.specs2" %% "specs2" % "1.13" % "test"
)

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

publishMavenStyle := true

publishArtifact in Test := false
