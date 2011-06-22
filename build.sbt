sbtPlugin := true

name := "sbt-artifact-publish-plugin"

organization := "com.gu"

version := "1.1-SNAPSHOT"

credentials += Credentials(file("/usr/local/bin/sbt.nexus.credentials"))

publishTo <<= (version) { version: String =>
  val nexus = "http://nexus.gudev.gnl:8081/nexus/content/repositories/"
  if (version.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus+"snapshots/")
  else                                   Some("releases" at nexus+"releases/")
}
