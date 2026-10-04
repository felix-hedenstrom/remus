package nu.fxh.remus.server.db

import doobie.*
import doobie.implicits.*
import zio.*
import zio.interop.catz.*

import scala.io.Source

/** Tiny idempotent migration runner: applies numbered .sql resource files
  * once each, tracked in a schema_migrations table. Flyway's SQLite support
  * is a community plugin with rough edges; at this scale a hand-rolled
  * runner is simpler and easier to reason about.
  */
object Migrations:

  private val files = List("V1__init.sql")

  def run(xa: Transactor[Task]): Task[Unit] =
    for
      _       <- ensureTable(xa)
      applied <- appliedVersions(xa)
      _       <- ZIO.foreachDiscard(files.filterNot(applied.contains))(apply(xa, _))
    yield ()

  private def ensureTable(xa: Transactor[Task]): Task[Unit] =
    sql"CREATE TABLE IF NOT EXISTS schema_migrations (version TEXT PRIMARY KEY)".update.run
      .transact(xa)
      .unit

  private def appliedVersions(xa: Transactor[Task]): Task[Set[String]] =
    sql"SELECT version FROM schema_migrations".query[String].to[List].transact(xa).map(_.toSet)

  private def apply(xa: Transactor[Task], file: String): Task[Unit] =
    for
      content <- ZIO.attemptBlockingIO {
                   val src = Source.fromResource(s"db/$file")
                   try src.mkString
                   finally src.close()
                 }
      statements = content.split(";").map(_.trim).filter(_.nonEmpty)
      _ <- ZIO.foreachDiscard(statements)(stmt => Fragment.const0(stmt).update.run.transact(xa))
      _ <- sql"INSERT INTO schema_migrations(version) VALUES ($file)".update.run.transact(xa)
    yield ()
