inThisBuild(
  Seq(
    organization := "pl.iterators",
    version := "0.0.1",
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
  ))

name := "sealed"
moduleName := "sealed"
description := "Sealed monad"

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
    moduleName := "sealed-examples"
  )

lazy val benchmarks = project
  .in(file("benchmarks"))
  .dependsOn(LocalRootProject)
  .enablePlugins(JmhPlugin)
  .settings(
    name := "benchmarks",
    description := "Sealed monad benchmarks",
    moduleName := "sealed-benchmarks"
  )

aggregateProjects(
  examples,
  benchmarks
)
test / aggregate := false
addCommandAlias("flame", "benchmarks/jmh:run -p tokens=64 -prof jmh.extras.Async:dir=target/flamegraphs;flameGraphOpts=--width,1900")
