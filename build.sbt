import com.jsuereth.sbtpgp.PgpKeys
import sbtrelease.ReleasePlugin.autoImport._

val scala_2_11             = "2.11.12"
val scala_2_12             = "2.12.8"
val scala_2_13             = "2.13.1"
val mainScalaVersion       = scala_2_13
val supportedScalaVersions = Seq(scala_2_11, scala_2_12, scala_2_13)

lazy val baseSettings = Seq(
  organization := "pl.iterators",
  organizationName := "Iterators",
  organizationHomepage := Some(url("https://iterato.rs")),
  homepage := Some(url("https://github.com/theiterators/sealed-monad")),
  scalaVersion := mainScalaVersion,
  scalacOptions := Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8"),
  scalafmtOnCompile := true
)

val catsVersion = "2.0.0"
val castsTestkitScalatestVersion = "1.0.1"

libraryDependencies ++= Seq (
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-laws" % catsVersion % Test,
  "org.typelevel" %% "cats-testkit" % catsVersion % Test,
  "org.typelevel" %% "cats-testkit-scalatest" % castsTestkitScalatestVersion % Test
)

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)

lazy val publishToNexus = publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

lazy val crossBuildSettings = Seq(crossScalaVersions := supportedScalaVersions, releaseCrossBuild := true)

lazy val publishSettings = Seq(
  publishToNexus,
  publishMavenStyle := true,
  pomIncludeRepository := const(true),
  licenses := Seq("MIT License" -> url("http://opensource.org/licenses/MIT")),
  scmInfo := Some(
    ScmInfo(browseUrl = url("https://github.com/theiterators/sealed-monad"),
            connection = "scm:git:https://github.com/theiterators/sealed-monad.git")),
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
) ++ crossBuildSettings

lazy val noPublishSettings =
  Seq(
    publishToNexus /*must be set for sbt-release*/,
    publishArtifact := false,
    releaseCrossBuild := false,
    releasePublishArtifactsAction := {
      val projectName = name.value
      streams.value.log.warn(s"Publishing for $projectName is turned off")
    }
  )

lazy val examples = project
  .in(file("examples"))
  .dependsOn(LocalRootProject % "compile->compile;test->test")
  .settings(baseSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    name := "examples",
    description := "Sealed monad - snippets of example code",
    moduleName := "sealed-examples",
    skip in publish := true
  )

lazy val benchmarks = project
  .in(file("benchmarks"))
  .dependsOn(LocalRootProject)
  .enablePlugins(JmhPlugin)
  .settings(baseSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    name := "benchmarks",
    description := "Sealed monad benchmarks",
    moduleName := "sealed-benchmarks",
    skip in publish := true
  )

addCommandAlias("flame", "benchmarks/jmh:run -p tokens=64 -prof jmh.extras.Async:dir=target/flamegraphs;flameGraphOpts=--width,1900")

lazy val sealedMonad = project
  .in(file("."))
  .settings(baseSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "sealed-monad",
    description := "Library to eliminate the boilerplate code",
    publishToNexus, /*must be set for sbt-release*/
    releaseCrossBuild := false,
    publishArtifact := false,
    crossScalaVersions := Nil
  )
