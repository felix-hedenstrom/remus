package nu.fxh.remus.server.db

import doobie.Transactor
import zio.*

import java.nio.file.Files

/** A fresh temp-file SQLite database, migrated once, for use as a shared
  * layer across a test suite. Real doobie queries against a real (if
  * throwaway) database — no repository mocking.
  */
object TestDb:

  private val tempPath: ZLayer[Any, Throwable, String] =
    ZLayer.scoped {
      for
        path <- ZIO.attemptBlockingIO(Files.createTempFile("remus-test", ".db"))
        _    <- ZIO.addFinalizer(ZIO.attemptBlockingIO(Files.deleteIfExists(path)).ignore)
      yield path.toString
    }

  val layer: ZLayer[Any, Throwable, Transactor[Task]] =
    tempPath
      .flatMap(env => Db.layer(env.get[String]))
      .tap(env => Migrations.run(env.get[Transactor[Task]]))
