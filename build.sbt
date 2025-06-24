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
