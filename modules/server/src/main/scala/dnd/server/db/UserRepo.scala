package dnd.server.db

import doobie.*
import doobie.implicits.*
import zio.*
import zio.interop.catz.*

final case class UserRow(id: Long, username: String, discordId: String)

trait UserRepo:
  def findOrCreateByDiscordId(discordId: String, username: String): Task[UserRow]
  def findById(id: Long): Task[Option[UserRow]]

final class DoobieUserRepo(xa: Transactor[Task]) extends UserRepo:

  def findOrCreateByDiscordId(discordId: String, username: String): Task[UserRow] =
    sql"""INSERT INTO users(username, discord_id) VALUES ($username, $discordId)
          ON CONFLICT(discord_id) DO UPDATE SET username = excluded.username
          RETURNING id, username, discord_id"""
      .query[(Long, String, String)]
      .unique
      .transact(xa)
      .map { case (id, u, d) => UserRow(id, u, d) }

  def findById(id: Long): Task[Option[UserRow]] =
    sql"SELECT id, username, discord_id FROM users WHERE id = $id"
      .query[(Long, String, String)]
      .option
      .transact(xa)
      .map(_.map { case (i, u, d) => UserRow(i, u, d) })

object UserRepo:
  val layer: URLayer[Transactor[Task], UserRepo] = ZLayer.fromFunction(DoobieUserRepo(_))
