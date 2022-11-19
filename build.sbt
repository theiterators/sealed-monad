import com.jsuereth.sbtpgp.PgpKeys
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._

val isDotty = Def.setting(CrossVersion.partialVersion(scalaVersion.value).exists(_._1 != 2))

// Dependencies

val catsVersion                  = "2.8.0"
val castsTestkitScalatestVersion = "2.1.5"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core"              % catsVersion,
  "org.typelevel" %% "cats-laws"              % catsVersion                  % Test,
  "org.typelevel" %% "cats-testkit"           % catsVersion                  % Test,
  "org.typelevel" %% "cats-testkit-scalatest" % castsTestkitScalatestVersion % Test
)

libraryDependencies ++= (if (isDotty.value) Nil
                         else
                           Seq(compilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full)))

// Multiple Scala versions support

val scala_2_12             = "2.12.16"
val scala_2_13             = "2.13.10"
val dotty                  = "3.1.3"
val mainScalaVersion       = scala_2_13
val supportedScalaVersions = Seq(scala_2_12, scala_2_13, dotty)

lazy val baseSettings = Seq(
// Scala settings
  homepage     := Some(url("https://github.com/theiterators/sealed-monad")),
  scalaVersion := mainScalaVersion,
  scalacOptions := Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8") ++
    (if (isDotty.value)
       Seq("-language:implicitConversions", "-Ykind-projector", "-Xignore-scala2-macros")
     else Nil),
  scalafmtOnCompile := true,
// Sonatype settings
  publishTo            := sonatypePublishTo.value,
  sonatypeProfileName  := "pl.iterators",
  publishMavenStyle    := true,
  licenses             := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
  organization         := "pl.iterators",
  organizationName     := "Iterators",
  organizationHomepage := Some(url("https://iterato.rs")),
  developers := List(
    Developer(
      id = "mrzeznicki",
      name = "Marcin Rzeźnicki",
      email = "mrzeznicki@iterato.rs",
      url = url("https://github.com/marcin-rzeznicki")
    )
  ),
  scmInfo := Some(
    ScmInfo(
      browseUrl = url("https://github.com/theiterators/sealed-monad"),
      connection = "scm:git:https://github.com/theiterators/sealed-monad.git"
    )
  ),
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  crossScalaVersions            := supportedScalaVersions,
  releaseCrossBuild             := true
)

lazy val noPublishSettings =
  Seq(
    publishArtifact   := false,
    releaseCrossBuild := false,
    skip / publish    := true,
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
    name        := "examples",
    description := "Sealed monad - snippets of example code",
    moduleName  := "sealed-examples"
  )

lazy val docs = project
  .in(file("sealed-docs"))
  .dependsOn(sealedMonad % "test->test;compile->compile")
  .enablePlugins(MdocPlugin, DocusaurusPlugin)
  .settings(baseSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    name        := "docs",
    description := "Sealed monad documentation",
    moduleName  := "sealed-docs"
  )
  .settings(
    mdocVariables := Map(
      "VERSION" -> version.value
    )
  )

lazy val benchmarks = project
  .in(file("benchmarks"))
  .dependsOn(sealedMonad % "test->test;compile->compile")
  .enablePlugins(JmhPlugin)
  .settings(baseSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    name        := "benchmarks",
    description := "Sealed monad benchmarks",
    moduleName  := "sealed-benchmarks"
  )

addCommandAlias("flame", "benchmarks/jmh:run -p tokens=64 -prof jmh.extras.Async:dir=target/flamegraphs;flameGraphOpts=--width,1900")

lazy val sealedMonad = project
  .in(file("."))
  .settings(baseSettings: _*)
  .settings(
    name        := "sealed-monad",
    description := "Scala library for nice for-comprehension-style error handling",
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
