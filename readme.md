sbt-dist-plugin
===========================

This sbt 0.10 plugin genarates a .zip file (commonly artifacts.zip) that includes all the deployable
artifacts along with the deployment scripts needed to deploy them. The task to generate the .zip archive
is typically invoked by teamcity and the archive is added in to the artifact repository (I.E a known directory).


To include the plugin:
----------------------

1. Work out what released version you want to use by going to
<https://github.com/guardian/guardian.github.com/tree/master/maven/repo-releases/com/gu/sbt-dist-plugin_2.8.1>

2. Add the sbt-artifact-publish-plugin to your sbt build, by creating project/plugins/build.sbt that looks like:

        resolvers += "Guardian Github Releases" at "http://guardian.github.com/maven/repo-releases"

        libraryDependencies += "com.gu" %% "sbt-dist-plugin" % "<version>"

3. Decide which version of the deployment library (gu-deploy-libs) you are using by going
to <http://nexus.gudev.gnl:8081/nexus/content/repositories/releases/com/gu/gu-deploy-libs>

4. Add the deployment library as a dependency for the "dist" plugin in your root sbt build file (build.sbt). (Any ivy / maven
dependency that you add to the "dist" configuration will be unzipped into the root of your artifact zip.)

        resolvers in ThisBuild ++= Seq(
          "Guardian Github Releases" at "http://guardian.github.com/maven/repo-releases",
          "Guardian Internal Releases" at "http://nexus.gudev.gnl:8081/nexus/content/repositories/releases"
        )

        libraryDependencies += "com.gu" % "gu-deploy-libs" % "1.70" % "dist"


To configure the plugin
========================

For multi-module builds
-----------------------

For multi-module builds, where you want to include outputs from multiple projects into your artifact zip, you need to
define 3 things to get the distribution zip file you desire:

1. Default settings for the dist plugin. Because not all projects want to produce a distribution file, you need to manually
register the plugin's tasks with the project that needs it:

        import com.gu._

        object MyProject extends Build {
          lazy val root = Project("root", file("."),
            settings = Defaults.defaultSettings ++ WebPlugin.webSettings ++ SbtDistPlugin.defaultSettings)

          ...
        }

2. The output path for the zip file. When publishing to the artifact repository this might look something like:

        import com.gu._

        object MyProject extends Build {
          lazy val root = Project("root", file("."),
            settings = Defaults.defaultSettings ++ WebPlugin.webSettings ++ SbtDistPlugin.defaultSettings)
            .settings(SbtArtifactPublishPlugin.distPath := artifactFileName)

         ...
         }

        def artifactFileName =
            file("/r2/ArtifactRepository/my-project/trunk") / ("trunk-build." + System.getProperty("build.number", "DEV")) / "artifacts.zip"

3. The contents of the zip file. This is defined as a Seq[(File, String)], i.e. a bunch of tuples with a file to include
in the zip and a string declaring the path you want that file to appear in the zip.

  To add the war file that is output by a web project (that uses the WebPlugin):

	object MyProject extends Build {
	  lazy val root = Project("root", file("."),
	    settings = Defaults.defaultSettings ++ WebPlugin.webSettings ++ SbtArtifactPublishPlugin.defaultSettings)
	    .aggregate(subWebProject1, subWebProject2, projectWithDeployFiles)
	    .settings(SbtArtifactPublishPlugin.distFiles <+= webappProject(subWebProject1, "foo/webapps/app-one.war"))
	    .settings(SbtArtifactPublishPlugin.distFiles <+= webappProject(subWebProject2, "foo/webapps/app-two.war"))

	...
	}

	def webappProject(project: Project, outputPath: String) = packageWar in (project, Compile) map { _ -> outputPath }

To add arbitrary files within a project:

	object MyProject extends Build {
	  lazy val root = Project(
	    "root",
	    file("."),
	    settings = Defaults.defaultSettings ++ WebPlugin.webSettings ++ SbtArtifactPublishPlugin.defaultSettings)
	    .aggregate(subWebProject1, subWebProject2, projectWithDeployFiles)
	    .settings(SbtArtifactPublishPlugin.distFiles <++= deployScripts)

	...

	def deployScripts = {
	  sourceDirectory in projectWithDeployFiles map { src =>
	    val deployRoot = src / "main" / "deploy"
	    (deployRoot ***) x rebase (deployRoot, "deploy")
	  }
	}

Consult the content api for an example of this in use.


For single-module builds
------------------------

For single-module builds, you can define all of this stuff more concisely directly in your build.sbt file:

    import com.gu._

    // artifact generation stuff
    seq(SbtDistPlugin.defaultSettings :_*)

    resolvers ++= Seq(
      "Guardian Github Releases" at "http://guardian.github.com/maven/repo-releases",
      "Guardian Internal Releases" at "http://nexus.gudev.gnl:8081/nexus/content/repositories/releases"
    )

    libraryDependencies += "com.gu" % "gu-deploy-libs" % "1.70" % "dist"

    distPath := file("/r2/ArtifactRepository/microapp-cache/trunk") /
      ("trunk-build." + System.getProperty("build.number", "DEV")) / "artifacts.zip"

    // include the war itself (this is the location you want the war to save to)
    distFiles <+= (packageWar in Compile) map { _ -> "microapp-cache/webapps/microapp-cache.war" }

    // and include custom scripts in src/main/deploy
    distFiles <++= (sourceDirectory in Compile) map { src => (src / "deploy" ***) x relativeTo(src) }

Consult the microapp cache for an example of this in use.


To run the plugin:
------------------

To produce the zip file invoke the dist target.

Typically this will only be invoked by the CI server.
