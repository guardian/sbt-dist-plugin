package com.gu

import sbt._
import Keys._
import Defaults._
import Project.Initialize

object SbtArtifactPublishPlugin extends Plugin {

  val sourcefiles = TaskKey[Seq[(File, String)]]("assemble-source-files")
  val artifactPublishPath = SettingKey[File]("artifact-publish-path")
  val publishArtifacts = TaskKey[File]("publish-artifacts")


  def publishArtifactsTask(src: Seq[(File, String)], dest: File) = {

    println("source >> " + src)
    println("dest is >> " + dest)
    IO.zip(src, dest)

    dest
  }

  val defaultSettings: Seq[Project.Setting[_]] = Seq(
    sourcefiles <<= deployLibFiles,
		artifactPublishPath <<= (target){ (target) => target / "dist" / "artifacts.zip"},
    publishArtifacts <<= (sourcefiles, artifactPublishPath) map {publishArtifactsTask},
    libraryDependencies += "com.gu" % "gu-deploy-libs" % "1.70"
	)

  def deployLibFiles = {
    (managedClasspath in Compile, target) map { (cp, t) =>
      println("classpath is:")
      cp foreach println

      val guDeployJar = cp.filter(_.data.getName.startsWith("gu-deploy-libs")).head.data
      val deployUnpackDir = t / "deployfiles"
      IO.unzip(guDeployJar, deployUnpackDir)
      val files: Seq[(File, String)] = (deployUnpackDir ***) x rebase (deployUnpackDir, "deploy")
      files
    }
  }
}