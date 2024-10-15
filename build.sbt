val isScala3 = Def.setting(CrossVersion.partialVersion(scalaVersion.value).exists(_._1 != 2))

// Dependencies

val catsVersion                  = "2.12.0"
val castsTestkitScalatestVersion = "2.1.5"
val scalatestVersion             = "3.2.19"
val disciplineVersion            = "2.3.0"

// Multiple Scala versions support

val scala_2_13             = "2.13.15"
val scala_3                = "3.3.4"
val mainScalaVersion       = scala_2_13
val supportedScalaVersions = Seq(scala_2_13, scala_3)

ThisBuild / crossScalaVersions := supportedScalaVersions
ThisBuild / scalaVersion       := mainScalaVersion

ThisBuild / versionScheme                       := Some("early-semver")
ThisBuild / githubWorkflowJavaVersions          := Seq(JavaSpec.temurin("11"), JavaSpec.temurin("17"))
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")), RefPredicate.Equals(Ref.Branch("master")))
ThisBuild / tlBaseVersion                       := "2.0"
ThisBuild / tlCiHeaderCheck                     := false
ThisBuild / sonatypeCredentialHost              := xerial.sbt.Sonatype.sonatypeLegacy

lazy val baseSettings = Seq(
// Scala settings
  homepage := Some(url("https://github.com/theiterators/sealed-monad")),
  scalacOptions ++= (if (isScala3.value)
                       Seq(
                         "-deprecation",
                         "-unchecked",
                         "-feature",
                         "-language:implicitConversions",
                         "-Ykind-projector:underscores",
                         "-encoding",
                         "utf8"
                       )
                     else
                       Seq(
                         "-deprecation",
                         "-unchecked",
                         "-feature",
                         "-Xsource:3",
                         "-P:kind-projector:underscore-placeholders",
                         "-encoding",
                         "utf8"
                       )),
  libraryDependencies ++= Seq(
    "org.typelevel" %%% "cats-core"            % catsVersion,
    "org.typelevel" %%% "cats-laws"            % catsVersion       % Test,
    "org.typelevel" %%% "cats-testkit"         % catsVersion       % Test,
    "org.scalatest" %%% "scalatest"            % scalatestVersion  % Test,
    "org.typelevel" %%% "discipline-scalatest" % disciplineVersion % Test
  ) ++ (if (isScala3.value) Nil
        else
          Seq(compilerPlugin("org.typelevel" %% "kind-projector" % "0.13.3" cross CrossVersion.full))),
  scalafmtOnCompile := true,
// Sonatype settings
  licenses             := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
  organization         := "pl.iterators",
  organizationName     := "Iterators",
  organizationHomepage := Some(url("https://www.iteratorshq.com")),
  developers := List(
    Developer(
      id = "luksow",
      name = "Łukasz Sowa",
      email = "lukasz@iteratorshq.com",
      url = url("https://github.com/luksow")
    ),
    Developer(
      id = "pkiersznowski",
      name = "Paweł Kiersznowski",
      email = "pkiersznowski@iteratorshq.com",
      url = url("https://github.com/pk044")
    )
  ),
  scmInfo := Some(
    ScmInfo(
      browseUrl = url("https://github.com/theiterators/sealed-monad"),
      connection = "scm:git:https://github.com/theiterators/sealed-monad.git"
    )
  ),
  crossScalaVersions := supportedScalaVersions
)

lazy val noPublishSettings =
  Seq(
    publishArtifact := false,
    skip / publish  := true
  )

lazy val examples = project
  .in(file("examples"))
  .dependsOn(sealedMonad.jvm % "test->test;compile->compile")
  .settings(baseSettings *)
  .settings(noPublishSettings *)
  .settings(
    name        := "examples",
    description := "Sealed monad - snippets of example code",
    moduleName  := "sealed-examples"
  )

lazy val docs = project
  .in(file("sealed-docs"))
  .dependsOn(sealedMonad.jvm % "test->test;compile->compile")
  .enablePlugins(MdocPlugin, DocusaurusPlugin)
  .settings(baseSettings *)
  .settings(noPublishSettings *)
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
  .dependsOn(sealedMonad.jvm % "test->test;compile->compile")
  .enablePlugins(JmhPlugin)
  .settings(baseSettings *)
  .settings(noPublishSettings *)
  .settings(
    name        := "benchmarks",
    description := "Sealed monad benchmarks",
    moduleName  := "sealed-benchmarks"
  )

addCommandAlias("flame", "benchmarks/jmh:run -p tokens=64 -prof jmh.extras.Async:dir=target/flamegraphs;flameGraphOpts=--width,1900")

lazy val sealedMonad = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("sealedmonad"))
  .jsConfigure(_.disablePlugins(DoctestPlugin))
  .settings(baseSettings *)
  .settings(
    name        := "sealed-monad",
    description := "Scala library for nice for-comprehension-style error handling"
  )