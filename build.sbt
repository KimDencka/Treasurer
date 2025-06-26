import sbt.Keys.scalacOptions

ThisBuild / organization      := "com.densungkim"
ThisBuild / version           := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion      := "2.13.16"
ThisBuild / scalafmtOnCompile := true
Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = (project in file("."))
  .settings(
    name        := "treasurer",
    libraryDependencies ++= Dependencies.librariesDependencies,
    scalacOptions ++= CompilerOptions.cOptions,
    Test / scalacOptions --= Seq("-Xfatal-warnings"),
    Test / test := (Test / test).dependsOn(Test / scalafmt).value,
    Test / javaOptions += "-Xshare:off",
    Test / fork := true,
  )

lazy val integration = (project in file("integration"))
  .dependsOn(root % "compile->compile;test->test")
  .settings(
    name := "integration",
    scalacOptions ++= CompilerOptions.cOptions,
    Test / parallelExecution := false,
    Test / scalacOptions --= Seq("-Xfatal-warnings"),
    Test / test := (Test / test).dependsOn(Test / scalafmt).value,
  )
