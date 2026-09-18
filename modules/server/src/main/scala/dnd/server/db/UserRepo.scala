package dnd.server.db

import doobie.*
import doobie.implicits.*
import zio.*
import zio.interop.catz.*

final case class UserRow(id: Long, username: String, passwordHash: String)

trait UserRepo:
  def create(username: String, passwordHash: String): Task[UserRow]
  def findByUsername(username: String): Task[Option[UserRow]]
  def findById(id: Long): Task[Option[UserRow]]

final class DoobieUserRepo(xa: Transactor[Task]) extends UserRepo:

  def create(username: String, passwordHash: String): Task[UserRow] =
    sql"INSERT INTO users(username, password_hash) VALUES ($username, $passwordHash)".update
      .withUniqueGeneratedKeys[Long]("id")
      .transact(xa)
      .map(UserRow(_, username, passwordHash))

  def findByUsername(username: String): Task[Option[UserRow]] =
    sql"SELECT id, username, password_hash FROM users WHERE username = $username"
      .query[(Long, String, String)]
      .option
      .transact(xa)
      .map(_.map { case (id, u, p) => UserRow(id, u, p) })

  def findById(id: Long): Task[Option[UserRow]] =
    sql"SELECT id, username, password_hash FROM users WHERE id = $id"
      .query[(Long, String, String)]
      .option
      .transact(xa)
      .map(_.map { case (i, u, p) => UserRow(i, u, p) })

object UserRepo:
  val layer: URLayer[Transactor[Task], UserRepo] = ZLayer.fromFunction(DoobieUserRepo(_))
