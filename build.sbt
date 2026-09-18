val scala3Version = "3.9.0"

val zioVersion         = "2.1.26"
val tapirVersion       = "1.13.31"
val circeVersion       = "0.14.16"
val sttpVersion        = "4.0.26"
val doobieVersion      = "1.0.0-RC12"
val interopCatsVersion = "23.1.0.13"
val laminarVersion     = "17.2.1"

ThisBuild / scalaVersion := scala3Version
ThisBuild / organization := "nu.fxh.pelle"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

lazy val shared = (projectMatrix in file("modules/shared"))
  .settings(
    name := "shared",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core"       % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
      "io.circe"                    %% "circe-core"       % circeVersion,
      "io.circe"                    %% "circe-generic"    % circeVersion,
      "io.circe"                    %% "circe-parser"     % circeVersion
    )
  )
  .jvmPlatform(scalaVersions = Seq(scala3Version))
  .jsPlatform(scalaVersions = Seq(scala3Version))

lazy val sharedJVM = shared.jvm(scala3Version)
lazy val sharedJS  = shared.js(scala3Version)

lazy val copyClientAssets = taskKey[Unit]("Link the client with Scala.js and copy the bundle into server resources")
lazy val copyRuntimeDeps  = taskKey[File]("Copy the server's runtime dependency jars into target/docker-libs")

lazy val server = (project in file("modules/server"))
  .dependsOn(sharedJVM)
  .settings(
    name := "server",
    copyClientAssets := Def.uncached {
      (client / Compile / fullLinkJS).value
      val outDir     = (client / Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value
      val staticDir  = (Compile / resourceDirectory).value / "static"
      IO.copyFile(outDir / "main.js", staticDir / "main.js")
    },
    copyRuntimeDeps := Def.uncached {
      val outDir = target.value / "docker-libs"
      IO.delete(outDir)
      IO.createDirectory(outDir)
      val conv = fileConverter.value
      (Compile / dependencyClasspathAsJars).value.foreach { af =>
        val file = conv.toPath(af.data).toFile
        IO.copyFile(file, outDir / file.getName)
      }
      outDir
    },
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server"    % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-files"               % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle"  % tapirVersion,
      "org.tpolecat" %% "doobie-core"      % doobieVersion,
      "org.tpolecat" %% "doobie-hikari"    % doobieVersion,
      "dev.zio" %% "zio-interop-cats" % interopCatsVersion,
      "org.xerial" % "sqlite-jdbc" % "3.53.4.0",
      "com.password4j" % "password4j" % "1.8.4",
      "ch.qos.logback" % "logback-classic" % "1.6.3",
      "dev.zio" %% "zio-test"     % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

lazy val client = (project in file("modules/client"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(sharedJS)
  .settings(
    name := "client",
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= (_.withModuleKind(org.scalajs.linker.interface.ModuleKind.NoModule)),
    libraryDependencies ++= Seq(
      "com.raquo" %% "laminar" % laminarVersion,
      "org.scala-js" %% "scalajs-dom" % "2.8.1",
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-client4" % tapirVersion,
      "com.softwaremill.sttp.client4" %% "zio" % sttpVersion
    )
  )

lazy val root = (project in file("."))
  .aggregate(sharedJVM, sharedJS, server, client)
  .settings(
    name := "pellegrino-simulator",
    publish / skip := true
  )
