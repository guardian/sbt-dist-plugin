sbt-artifact-publish-plugin
===========================

This sbt 0.10 plugin genarates a .zip file (commonly artifacts.zip) that includes all the deployable
artifacts along with the deployment scripts needed to deploy them. The task to generate the .zip archive
is typically invoked by teamcity and the archive is added in to the artifact repository (I.E a known directory).


To include the plugin:
----------------------

1. Work out what released version you want to use by going to <https://github.com/guardian/guardian.github.com/tree/master/maven/repo-releases/com/gu/sbt-artifact-publish-plugin>

2. Add the sbt-artifact-publish-plugin to your sbt build, by creating project/plugins/build.sbt that looks like:

        resolvers ++= Seq(
            "Guardian Github Releases" at "http://guardian.github.com/maven/repo-releases",
            "Guardian Github Snapshots" at "http://guardian.github.com/maven/repo-snapshots"
            )

        libraryDependencies += "com.gu" %% "sbt-artifact-publish-plugin" % "2.0-SNAPSHOT"


To configure the plugin:
------------------------

you need to define 2 things to get the artifacts zip file you desire:

1. The output path for the zip file. When publishing to the artifact repository this might look something like:

	object MyProject extends Build {
	  lazy val root = Project(
	    "root",
	    file("."),
	    settings = Defaults.defaultSettings ++ WebPlugin.webSettings ++ SbtArtifactPublishPlugin.defaultSettings)
	    .aggregate(subWebProject1, subWebProject2, projectWithDeployFiles)
	    .settings(SbtArtifactPublishPlugin.artifactPublishPath := artifactFileName)
	
	...

	def artifactFileName = {
	    file("/r2/ArtifactRepository/my-project/trunk") / ("trunk-build." + System.getProperty("build.number", "DEV")) / "artifacts.zip"
	}

If not publishing to the artifact repository you can specify the output path in terms of the project target directory:

	object MyProject extends Build {
	  lazy val root = Project(
	    "root",
	    file("."),
	    settings = Defaults.defaultSettings ++ WebPlugin.webSettings ++ SbtArtifactPublishPlugin.defaultSettings)
	    .aggregate(subWebProject1, subWebProject2, projectWithDeployFiles)
	    .settings(SbtArtifactPublishPlugin.artifactPublishPath <<= artifactFileName)
	
	...

	def artifactFileName = {
	    (target){ (target) => target / "dist" / "artifacts.zip" }
	}

2. The contents of the zip file. This is defined as a Seq[(File, String)], I.E a bunch of tuples with a file to include in the zip and a string declaring the path you want that file to appear in the zip.

By default the zip includes the deployment framework, you need to define all the other files by adding to the SbtArtifactPublishPlugin.sourcefile setting.

To add the war file that is output by a web project (that uses the WebPlugin):

	object MyProject extends Build {
	  lazy val root = Project(
	    "root",
	    file("."),
	    settings = Defaults.defaultSettings ++ WebPlugin.webSettings ++ SbtArtifactPublishPlugin.defaultSettings)
	    .aggregate(subWebProject1, subWebProject2, projectWithDeployFiles)
	    .settings(SbtArtifactPublishPlugin.assembleSourceFiles <+= webappProject(subWebProject1, "foo/webapps/app-one.war"))
	    .settings(SbtArtifactPublishPlugin.assembleSourceFiles <+= webappProject(subWebProject2, "foo/webapps/app-two.war"))

	...

	def webappProject(project: Project, outputPath: String) = {
	  packageWar in (project, Compile) map { war => (war -> outputPath)}
	}

To add arbitrary files within a project:

	object MyProject extends Build {
	  lazy val root = Project(
	    "root",
	    file("."),
	    settings = Defaults.defaultSettings ++ WebPlugin.webSettings ++ SbtArtifactPublishPlugin.defaultSettings)
	    .aggregate(subWebProject1, subWebProject2, projectWithDeployFiles)
	    .settings(SbtArtifactPublishPlugin.assembleSourceFiles <++= deployScripts)

	...

	def deployScripts = {
	  sourceDirectory in projectWithDeployFiles map { r =>
	    val deployRoot = r / "main" / "deploy"
	    val deployFiles: Seq[(File, String)] = (deployRoot ***) x rebase (deployRoot, "deploy")
	    deployFiles
	  }
	}

To run the plugin:
------------------

To produce the zip file invoke the publish-artifacts target.

Typically this will only be invoked by the CI server.
