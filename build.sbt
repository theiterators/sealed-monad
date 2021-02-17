import com.jsuereth.sbtpgp.PgpKeys
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._

// Dependencies

val catsVersion                  = "2.4.2"
val castsTestkitScalatestVersion = "2.1.1"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core"              % catsVersion,
  "org.typelevel" %% "cats-laws"              % catsVersion % Test,
  "org.typelevel" %% "cats-testkit"           % catsVersion % Test,
  "org.typelevel" %% "cats-testkit-scalatest" % castsTestkitScalatestVersion % Test
)

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.3" cross CrossVersion.full)

// Multiple Scala versions support

val scala_2_12             = "2.12.13"
val scala_2_13             = "2.13.4"
val mainScalaVersion       = scala_2_13
val supportedScalaVersions = Seq(scala_2_12, scala_2_13)

lazy val baseSettings = Seq(
// Scala settings
  homepage := Some(url("https://github.com/theiterators/sealed-monad")),
  scalaVersion := mainScalaVersion,
  scalacOptions := Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8"),
  scalafmtOnCompile := true,
// Sonatype settings
  publishTo := sonatypePublishTo.value,
  sonatypeProfileName := "pl.iterators",
  publishMavenStyle := true,
  licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
  organization := "pl.iterators",
  organizationName := "Iterators",
  organizationHomepage := Some(url("https://iterato.rs")),
  developers := List(
    Developer(id = "mrzeznicki",
      name = "Marcin Rzeźnicki",
      email = "mrzeznicki@iterato.rs",
      url = url("https://github.com/marcin-rzeznicki"))),
  scmInfo := Some(
    ScmInfo(
      browseUrl = url("https://github.com/theiterators/sealed-monad"),
      connection = "scm:git:https://github.com/theiterators/sealed-monad.git"
    )
  ),
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  crossScalaVersions := supportedScalaVersions,
  releaseCrossBuild := true
)

lazy val noPublishSettings =
  Seq(
    publishArtifact := false,
    releaseCrossBuild := false,
    skip in publish := true,
    releasePublishArtifactsAction := {
      val projectName = name.value
      streams.value.log.warn(s"Publishing for $projectName is turned off")
    }
  )

lazy val examples = project
  .in(file("examples"))
  .dependsOn(sealedMonad % "test->test;compile->compile")
  .settings(baseSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    name := "examples",
    description := "Sealed monad - snippets of example code",
    moduleName := "sealed-examples"
  )

lazy val benchmarks = project
  .in(file("benchmarks"))
  .dependsOn(sealedMonad % "test->test;compile->compile")
  .enablePlugins(JmhPlugin)
  .settings(baseSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    name := "benchmarks",
    description := "Sealed monad benchmarks",
    moduleName := "sealed-benchmarks"
  )

addCommandAlias("flame", "benchmarks/jmh:run -p tokens=64 -prof jmh.extras.Async:dir=target/flamegraphs;flameGraphOpts=--width,1900")

lazy val sealedMonad = project
  .in(file("."))
  .settings(baseSettings: _*)
  .settings(
    name := "sealed-monad",
    description := "Library to eliminate the boilerplate code",
    releaseProcess := Seq(
      checkSnapshotDependencies,
      inquireVersions,
      releaseStepCommandAndRemaining("+publishLocalSigned"),
      releaseStepCommandAndRemaining("+clean"),
      releaseStepCommandAndRemaining("+test"),
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("+publishSigned"),
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )
