package dnd.server

import dnd.server.db.*
import doobie.Transactor
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zio.*

object Main extends ZIOAppDefault:

  private val port   = sys.env.get("PORT").flatMap(_.toIntOption).getOrElse(8080)
  private val dbPath = sys.env.getOrElse("DB_PATH", "./data/pellegrino.db")

  private def ensureParentDir(path: String): Task[Unit] =
    ZIO.attemptBlockingIO {
      val parent = java.nio.file.Paths.get(path).toAbsolutePath.getParent
      if parent != null then java.nio.file.Files.createDirectories(parent)
      ()
    }

  private val program =
    for
      _     <- ensureParentDir(dbPath)
      xa    <- ZIO.service[Transactor[Task]]
      _     <- Migrations.run(xa)
      auth  <- ZIO.service[AuthService]
      chars <- ZIO.service[CharacterService]
      routes = ZioHttpInterpreter().toHttp(HttpApi.endpoints(auth, chars))
      _     <- ZIO.logInfo(s"Starting server on port $port")
      _     <- zio.http.Server.serve(routes)
    yield ()

  override def run =
    program.provide(
      Db.layer(dbPath),
      UserRepo.layer,
      SessionRepo.layer,
      CharacterRepo.layer,
      AuthService.layer,
      CharacterService.layer,
      zio.http.Server.defaultWithPort(port)
    )
