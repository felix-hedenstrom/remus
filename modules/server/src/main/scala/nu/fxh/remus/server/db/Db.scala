package nu.fxh.remus.server.db

import doobie.*
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway
import zio.*
import zio.interop.catz.*

import scala.concurrent.ExecutionContext

object Db:

  /** `busy_timeout` lets SQLite retry instead of immediately failing when a
    * writer briefly holds the file lock; WAL lets reads proceed concurrently
    * with a writer; `foreign_keys=on` makes the schema's `ON DELETE CASCADE`
    * clauses actually fire (off by default in SQLite).
    */
  def jdbcUrl(dbPath: String): String =
    s"jdbc:sqlite:$dbPath?busy_timeout=5000&journal_mode=WAL&foreign_keys=on"

  private def migrate(url: String): Task[Unit] =
    ZIO.attemptBlocking {
      Flyway
        .configure()
        .dataSource(url, "", "")
        .locations("classpath:db/migration")
        .load()
        .migrate()
    }.unit

  /** A single-file SQLite database, migrated to the latest schema via
    * Flyway before the connection pool is built.
    */
  def layer(dbPath: String): ZLayer[Any, Throwable, Transactor[Task]] =
    ZLayer.scoped {
      val url = jdbcUrl(dbPath)
      for
        _  <- migrate(url)
        resource = HikariTransactor.newHikariTransactor[Task](
                     "org.sqlite.JDBC",
                     url,
                     "",
                     "",
                     ExecutionContext.global
                   )
        xa <- ZIO.acquireRelease(resource.allocated)(_._2.orDie).map(_._1)
      yield xa
    }
