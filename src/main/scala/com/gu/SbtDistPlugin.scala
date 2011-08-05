package com.gu

import sbt._
import Keys._

object SbtDistPlugin extends Plugin {
  val distConf = config("dist") hide

  lazy val distFiles = TaskKey[Seq[(File, String)]]("dist-files")
  lazy val distPath = SettingKey[File]("dist-path")
  lazy val dist = TaskKey[File]("dist")

  def distTask(src: Seq[(File, String)], dest: File, s: TaskStreams) = {
    s.log.info("writing distribution to: " + dest)
    IO.zip(src, dest)
    dest
  }

  def deployLibFiles = {
    (managedClasspath in dist, target, streams) map { (cp, t, s) =>
      val deployUnpackDir = t / "deployfiles"

      for (attributedjar <- cp) {
        val jar = attributedjar.data
        s.log.debug("unzipping %s..." format jar)
        IO.unzip(jar, deployUnpackDir)
      }

      val files: Seq[(File, String)] = (deployUnpackDir ***) x rebase (deployUnpackDir, "deploy")
      s.log.debug("including in zip: " + files.mkString(", "))
      files
    }
  }


  val distSettings: Seq[Project.Setting[_]] = Seq(
    ivyConfigurations += distConf,
    distFiles <<= deployLibFiles,
		distPath <<= (target) { (target) => target / "dist" / "artifacts.zip" },
    managedClasspath in dist <<= (classpathTypes, update) map { (ct, report) => Classpaths.managedJars(distConf, ct, report) },
    dist <<= (distFiles, distPath, streams) map distTask
	)

}