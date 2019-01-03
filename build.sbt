enablePlugins(GitVersioning)

inThisBuild(
  Seq(
    organization := "pl.iterators",
    git.baseVersion := "0.0.1",
    scalaVersion := "2.12.6",
    scalacOptions := Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-Xlint:_",
      "-Ywarn-unused-import",
      "-Ywarn-unused:locals,privates",
      "-Ywarn-adapted-args",
      "-Ypartial-unification",
      "-encoding",
      "utf8",
      "-target:jvm-1.8",
      "-opt:l:inline",
      "-opt-inline-from:**"
    ),
    logBuffered in Test := false,
    scalafmtVersion := "1.4.0",
    scalafmtOnCompile := true
  ) ++ publishing)

name := "sealed"
moduleName := "sealed"
description := "Sealed monad"
bintrayRepository := "sealed-monad"

val cats        = "org.typelevel" %% "cats-core"        % "1.4.0"
val catsLaws    = "org.typelevel" %% "cats-kernel-laws" % "1.4.0"
val catsTestkit = "org.typelevel" %% "cats-testkit"     % "1.4.0"

def unitTests(modules: ModuleID*) = modules.map(_ % Test)

libraryDependencies ++= cats +: unitTests(catsTestkit, catsLaws)
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7")

lazy val examples = project
  .in(file("examples"))
  .dependsOn(LocalRootProject % "compile->compile;test->test")
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
  .settings(
    name := "benchmarks",
    description := "Sealed monad benchmarks",
    moduleName := "sealed-benchmarks",
    skip in publish := true
  )

aggregateProjects(
  examples,
  benchmarks
)
test / aggregate := false
addCommandAlias("flame", "benchmarks/jmh:run -p tokens=64 -prof jmh.extras.Async:dir=target/flamegraphs;flameGraphOpts=--width,1900")

lazy val publishing = Seq(
  publishMavenStyle := true,
  pomIncludeRepository := const(false),
  licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer(id = "mrzeznicki",
              name = "Marcin Rzeźnicki",
              email = "mrzeznicki@iterato.rs",
              url = url("https://github.com/marcin-rzeznicki"))),
  scmInfo := Some(
    ScmInfo(browseUrl = url("https://github.com/theiterators/sealed-monad"),
            connection = "scm:git:https://github.com/theiterators/sealed-monad.git")),
  bintrayOrganization := Some("theiterators"),
  bintrayReleaseOnPublish := false
)
