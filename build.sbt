val scala3Version = "3.9.0"

val zioVersion         = "2.1.26"
val tapirVersion       = "1.13.31"
val circeVersion       = "0.14.16"
val sttpVersion        = "4.0.26"
val doobieVersion      = "1.0.0-RC12"
val interopCatsVersion = "23.1.0.13"
val laminarVersion     = "17.2.1"
val zioConfigVersion   = "4.1.0"
val ironVersion        = "3.0.3"

ThisBuild / scalaVersion := scala3Version
ThisBuild / organization := "nu.fxh.pelle"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

// iron-circe pulls in its own circe-generic transitively; pin it to the
// version we already depend on so the project's circe auto-derivation
// (which several case classes rely on implicitly) doesn't shift versions.
ThisBuild / dependencyOverrides ++= Seq(
  "io.circe" %% "circe-core"    % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser"  % circeVersion
)

lazy val shared = (projectMatrix in file("modules/shared"))
  .settings(
    name := "shared",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core"       % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
      "io.circe"                    %% "circe-core"       % circeVersion,
      "io.circe"                    %% "circe-generic"    % circeVersion,
      "io.circe"                    %% "circe-parser"     % circeVersion,
      "io.github.iltotore"         %% "iron"              % ironVersion,
      "io.github.iltotore"         %% "iron-circe"        % ironVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-iron"       % tapirVersion
    )
  )
  .jvmPlatform(scalaVersions = Seq(scala3Version))
  .jsPlatform(scalaVersions = Seq(scala3Version))

lazy val sharedJVM = shared.jvm(scala3Version)
lazy val sharedJS  = shared.js(scala3Version)

lazy val copyClientAssets = taskKey[File]("Link the client with Scala.js and copy the bundle into server resources")
lazy val copyRuntimeDeps  = taskKey[File]("Copy the server's runtime dependency jars into target/docker-libs")

lazy val server = (project in file("modules/server"))
  .dependsOn(sharedJVM)
  .settings(
    name := "server",
    copyClientAssets := Def.uncached {
      (client / Compile / fullLinkJS).value
      val outDir  = (client / Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value
      val target  = (Compile / resourceManaged).value / "static" / "main.js"
      IO.copyFile(outDir / "main.js", target)
      target
    },
    // Wired into the resource pipeline (not just a manually-run task) so any
    // `compile`/`run`/`package` on this project always ships the current
    // client build - see https://www.scala-sbt.org/1.x/docs/Howto-Generating-Files.html
    Compile / resourceGenerators += Def.task { Seq(copyClientAssets.value) }.taskValue,
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
      "dev.zio" %% "zio-config"          % zioConfigVersion,
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
      "org.xerial" % "sqlite-jdbc" % "3.53.4.0",
      "com.softwaremill.sttp.client4" %% "zio" % sttpVersion,
      "ch.qos.logback" % "logback-classic" % "1.6.3",
      "dev.zio" %% "zio-test"     % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

lazy val devtools = (project in file("modules/devtools"))
  .dependsOn(server)
  .settings(
    name := "devtools",
    publish / skip := true
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
  .aggregate(sharedJVM, sharedJS, server, devtools, client)
  .settings(
    name := "pellegrino-simulator",
    publish / skip := true,
    Compile / run := (server / Compile / run).evaluated
  )

addCommandAlias("devRun", "devtools/runMain dnd.devtools.DevMain")
