package com.gu

import sbt._
import Keys._
import Defaults._
import Project.Initialize

object SbtArtifactPublishPlugin extends Plugin {

  lazy val assembleSourceFiles = TaskKey[Seq[(File, String)]]("assemble-source-files")
  lazy val artifactPublishPath = SettingKey[File]("artifact-publish-path")
  lazy val publishArtifacts = TaskKey[File]("publish-artifacts")


  def publishArtifactsTask(src: Seq[(File, String)], dest: File, s: TaskStreams) = {

    s.log.info("writing deployable artifacts to: " + dest)
    IO.zip(src, dest)
    dest
  }
  def deployLibFiles = {
    (managedClasspath in Compile, target) map { (cp, t) =>
      val guDeployJar = cp.filter(_.data.getName.startsWith("gu-deploy-libs")).head.data
      val deployUnpackDir = t / "deployfiles"
      IO.unzip(guDeployJar, deployUnpackDir)
      val files: Seq[(File, String)] = (deployUnpackDir ***) x rebase (deployUnpackDir, "deploy")
      files
    }
  }


  val defaultSettings: Seq[Project.Setting[_]] = Seq(
    assembleSourceFiles <<= deployLibFiles,
		artifactPublishPath <<= (target){ (target) => target / "dist" / "artifacts.zip"},
    publishArtifacts <<= (assembleSourceFiles, artifactPublishPath, streams) map {publishArtifactsTask},
    libraryDependencies += "com.gu" % "gu-deploy-libs" % "1.70"
	)

}