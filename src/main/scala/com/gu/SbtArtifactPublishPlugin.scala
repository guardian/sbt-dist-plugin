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
    sourcefiles := Nil,
		artifactPublishPath <<= (target){ (target) => target / "dist" / "artifacts.zip"},
    publishArtifacts <<= (sourcefiles, artifactPublishPath) map {publishArtifactsTask}
	)


}