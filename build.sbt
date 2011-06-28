sbtPlugin := true

name := "sbt-dist-plugin"

organization := "com.gu"

version := "1.4-SNAPSHOT"

// don't bother publishing javadoc
publishArtifact in (Compile, packageDoc) := false

publishTo <<= (version) { version: String =>
    val publishType = if (version.endsWith("SNAPSHOT")) "snapshots" else "releases"
    Some(
        Resolver.file(
            "guardian github " + publishType,
            file(System.getProperty("user.home") + "/guardian.github.com/maven/repo-" + publishType)
        )
    )
}
