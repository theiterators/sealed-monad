addSbtPlugin("pl.project13.scala"  % "sbt-jmh"                  % "0.4.7")
addSbtPlugin("org.scalameta"       % "sbt-scalafmt"             % "2.5.2")
addSbtPlugin("org.typelevel"       % "sbt-typelevel-ci-release" % "0.7.4")
addSbtPlugin("com.github.tkawachi" % "sbt-doctest"              % "0.10.0")
addSbtPlugin("org.scalameta"       % "sbt-mdoc"                 % "2.6.1")
addSbtPlugin("com.timushev.sbt"    % "sbt-updates"              % "0.6.4")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"      % "1.3.2")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.3.2")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"                   % "1.16.0")
addSbtPlugin("org.scala-native"   % "sbt-scala-native"              % "0.5.4")

libraryDependencySchemes += "com.lihaoyi" %% "geny" % VersionScheme.Always
