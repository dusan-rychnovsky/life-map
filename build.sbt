ThisBuild / scalaVersion := "3.5.2"
ThisBuild / organization := "cz.dusanrychnovsky.lifemap"
ThisBuild / version      := "0.1.0-SNAPSHOT"

val zioVersion     = "2.1.14"
val zioHttpVersion = "3.2.0"
val zioJsonVersion = "0.7.3"

lazy val root = (project in file("."))
  .settings(
    name := "life-map",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"          % zioVersion,
      "dev.zio" %% "zio-http"     % zioHttpVersion,
      "dev.zio" %% "zio-json"     % zioJsonVersion,
      "dev.zio" %% "zio-test"     % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )
