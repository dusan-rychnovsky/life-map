ThisBuild / scalaVersion := "3.5.2"
ThisBuild / organization := "cz.dusanrychnovsky.lifemap"
ThisBuild / version      := "0.1.0-SNAPSHOT"

val zioVersion           = "2.1.14"
val zioHttpVersion       = "3.2.0"
val zioJsonVersion       = "0.7.3"
val quillVersion         = "4.8.6"
val postgresVersion      = "42.7.4"
val flywayVersion        = "10.21.0"
val hikariVersion        = "5.1.0"
val slf4jVersion         = "2.0.16"
val testcontainersVersion = "0.41.8"

lazy val root = (project in file("."))
  .settings(
    name := "life-map",
    libraryDependencies ++= Seq(
      "dev.zio"       %% "zio"                            % zioVersion,
      "dev.zio"       %% "zio-http"                       % zioHttpVersion,
      "dev.zio"       %% "zio-json"                       % zioJsonVersion,
      "io.getquill"   %% "quill-jdbc-zio"                 % quillVersion,
      "org.postgresql" % "postgresql"                     % postgresVersion,
      "com.zaxxer"     % "HikariCP"                       % hikariVersion,
      "org.flywaydb"   % "flyway-core"                    % flywayVersion,
      "org.flywaydb"   % "flyway-database-postgresql"     % flywayVersion,
      "org.slf4j"      % "slf4j-simple"                   % slf4jVersion,
      "dev.zio"       %% "zio-test"                       % zioVersion           % Test,
      "dev.zio"       %% "zio-test-sbt"                   % zioVersion           % Test,
      "com.dimafeng"  %% "testcontainers-scala-postgresql" % testcontainersVersion % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    Test / fork := true,
    Test / parallelExecution := false,
    Test / javaOptions ++= Seq(
      s"-Ddocker.host=${sys.env.getOrElse("DOCKER_HOST", "unix:///var/run/docker.sock")}",
      s"-Dapi.version=${sys.env.getOrElse("DOCKER_API_VERSION", "1.43")}",
    ),
  )
