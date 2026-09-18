package dnd.server.db

import doobie.*
import doobie.hikari.HikariTransactor
import zio.*
import zio.interop.catz.*

import scala.concurrent.ExecutionContext

object Db:

  /** A single-file SQLite database. `busy_timeout` lets SQLite retry instead
    * of immediately failing when a writer briefly holds the file lock;
    * WAL lets reads proceed concurrently with a writer.
    */
  def layer(dbPath: String): ZLayer[Any, Throwable, Transactor[Task]] =
    ZLayer.scoped {
      val url = s"jdbc:sqlite:$dbPath?busy_timeout=5000&journal_mode=WAL"
      val resource = HikariTransactor.newHikariTransactor[Task](
        "org.sqlite.JDBC",
        url,
        "",
        "",
        ExecutionContext.global
      )
      ZIO.acquireRelease(resource.allocated)(_._2.orDie).map(_._1)
    }
