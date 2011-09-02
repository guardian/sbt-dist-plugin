package com.gu

import sbt._
import Keys._

object SbtDistPlugin extends Plugin {
  val distConf = config("dist") hide

  lazy val distFiles = TaskKey[Seq[(File, String)]]("dist-files", "Files to include in the distribution zip")
  lazy val distPath = SettingKey[File]("dist-path", "Path to generate the distribution zip to")
  lazy val distTeamCityIntegration = SettingKey[Boolean]("dist-teamcity-integration", "Ask TeamCity to publish zip")
  lazy val dist = TaskKey[File]("dist", "Generate distribution zip file")

  def distTask(src: Seq[(File, String)], dest: File, teamCityIntegration: Boolean, s: TaskStreams) = {
    s.log.info("writing distribution to: " + dest)
    IO.zip(src, dest)

    if (teamCityIntegration) {
      println("##teamcity[publishArtifacts '%s => .']" format dest.getAbsolutePath)
    }

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

    // teamcity sets this environment variable when it runs scripts
    private lazy val teamCityProjectName = Option(System.getenv("TEAMCITY_PROJECT_NAME"))
    private lazy val isRunningUnderTeamCity= teamCityProjectName.isDefined


  val distSettings: Seq[Project.Setting[_]] = Seq(
    ivyConfigurations += distConf,
    distTeamCityIntegration := isRunningUnderTeamCity,
    distFiles <<= deployLibFiles,
		distPath <<= (target) { (target) => target / "dist" / "artifacts.zip" },
    managedClasspath in dist <<= (classpathTypes, update) map { (ct, report) => Classpaths.managedJars(distConf, ct, report) },
    dist <<= (distFiles, distPath, distTeamCityIntegration, streams) map distTask
	)

}