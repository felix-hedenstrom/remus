package nu.fxh.remus.server

import nu.fxh.remus.server.db.*
import doobie.Transactor
import sttp.client4.Backend
import sttp.client4.httpclient.zio.HttpClientZioBackend
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zio.*
import zio.config.magnolia.deriveConfig
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.{Server, Response, Routes}

/** Shared server bootstrap, parameterized over the `AuthService` wiring so
 * alternate entrypoints (e.g. a dev-only bypass, see the devtools module)
 * can reuse everything else unchanged.
 */
object AppRuntime:

  val configLayer: ZLayer[Any, Config.Error, AppConfig] =
    ZLayer.fromZIO(TypesafeConfigProvider.fromResourcePath().load(deriveConfig[AppConfig]))

  private def ensureParentDir(path: String): Task[Unit] =
    ZIO.attemptBlockingIO {
      val parent = java.nio.file.Paths.get(path).toAbsolutePath.getParent
      if parent != null then java.nio.file.Files.createDirectories(parent)
      ()
    }

  private def serveOn(routes: Routes[Any, Response], onPort: Int): Task[Nothing] =
    Server.serve(routes).provide(Server.defaultWithPort(onPort))

  private val program =
    for
      config <- ZIO.service[AppConfig]
      _ <- ensureParentDir(config.dbPath)
      xa <- ZIO.service[Transactor[Task]]
      _ <- Migrations.run(xa)
      auth <- ZIO.service[AuthService]
      discord <- ZIO.service[DiscordOAuthService]
      chars <- ZIO.service[CharacterService]
      publicRoutes = ZioHttpInterpreter().toHttp(HttpApi.endpoints(auth, discord, chars))
      adminRoutes = AdminApi.routes
      _ <- ZIO.logInfo(s"Starting public API on port ${config.port}, admin API on port ${config.adminPort}")
      _ <- serveOn(publicRoutes, config.port).zipPar(serveOn(adminRoutes, config.adminPort))
    yield ()

  def run(authLayer: URLayer[UserRepo & SessionRepo, AuthService]): Task[Unit] =
    program.provide(
      configLayer,
      ZLayer.fromZIO(ZIO.serviceWith[AppConfig](_.dbPath)).flatMap(env => Db.layer(env.get[nu.fxh.remus.shared.NonEmptyString])),
      UserRepo.layer,
      SessionRepo.layer,
      CharacterRepo.layer,
      authLayer,
      CharacterService.layer,
      HttpClientZioBackend.layer(),
      DiscordOAuthService.layer
    )

object Main extends ZIOAppDefault:
  override def run = AppRuntime.run(AuthService.layer)
