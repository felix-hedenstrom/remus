package dnd.server.db

import doobie.*
import doobie.implicits.*
import zio.*
import zio.interop.catz.*

final case class SessionRow(userId: Long, expiresAt: Long)

trait SessionRepo:
  def create(token: String, userId: Long, expiresAt: Long): Task[Unit]
  def find(token: String): Task[Option[SessionRow]]
  def delete(token: String): Task[Unit]

final class DoobieSessionRepo(xa: Transactor[Task]) extends SessionRepo:

  def create(token: String, userId: Long, expiresAt: Long): Task[Unit] =
    sql"INSERT INTO sessions(token, user_id, expires_at) VALUES ($token, $userId, $expiresAt)".update.run
      .transact(xa)
      .unit

  def find(token: String): Task[Option[SessionRow]] =
    sql"SELECT user_id, expires_at FROM sessions WHERE token = $token"
      .query[(Long, Long)]
      .option
      .transact(xa)
      .map(_.map { case (u, e) => SessionRow(u, e) })

  def delete(token: String): Task[Unit] =
    sql"DELETE FROM sessions WHERE token = $token".update.run.transact(xa).unit

object SessionRepo:
  val layer: URLayer[Transactor[Task], SessionRepo] = ZLayer.fromFunction(DoobieSessionRepo(_))
