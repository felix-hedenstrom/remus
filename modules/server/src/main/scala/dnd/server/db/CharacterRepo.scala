package dnd.server.db

import doobie.*
import doobie.implicits.*
import zio.*
import zio.interop.catz.*

final case class CharacterRow(
    id: Long,
    ownerUserId: Long,
    name: String,
    species: String,
    profession: String,
    sheetJson: String
)

trait CharacterRepo:
  def listByOwner(ownerUserId: Long): Task[List[CharacterRow]]
  def find(id: Long): Task[Option[CharacterRow]]
  def create(ownerUserId: Long, name: String, species: String, profession: String, sheetJson: String): Task[CharacterRow]
  def update(id: Long, name: String, species: String, profession: String, sheetJson: String): Task[Unit]
  def delete(id: Long): Task[Unit]

final class DoobieCharacterRepo(xa: Transactor[Task]) extends CharacterRepo:

  private val columns = "id, owner_user_id, name, species, profession, sheet_json"

  private def toRow(t: (Long, Long, String, String, String, String)): CharacterRow =
    CharacterRow(t._1, t._2, t._3, t._4, t._5, t._6)

  def listByOwner(ownerUserId: Long): Task[List[CharacterRow]] =
    (fr"SELECT" ++ Fragment.const(columns) ++ fr"FROM characters WHERE owner_user_id = $ownerUserId")
      .query[(Long, Long, String, String, String, String)]
      .to[List]
      .transact(xa)
      .map(_.map(toRow))

  def find(id: Long): Task[Option[CharacterRow]] =
    (fr"SELECT" ++ Fragment.const(columns) ++ fr"FROM characters WHERE id = $id")
      .query[(Long, Long, String, String, String, String)]
      .option
      .transact(xa)
      .map(_.map(toRow))

  def create(
      ownerUserId: Long,
      name: String,
      species: String,
      profession: String,
      sheetJson: String
  ): Task[CharacterRow] =
    sql"""INSERT INTO characters(owner_user_id, name, species, profession, sheet_json)
          VALUES ($ownerUserId, $name, $species, $profession, $sheetJson)""".update
      .withUniqueGeneratedKeys[Long]("id")
      .transact(xa)
      .map(CharacterRow(_, ownerUserId, name, species, profession, sheetJson))

  def update(id: Long, name: String, species: String, profession: String, sheetJson: String): Task[Unit] =
    sql"""UPDATE characters SET name = $name, species = $species, profession = $profession, sheet_json = $sheetJson
          WHERE id = $id""".update.run.transact(xa).unit

  def delete(id: Long): Task[Unit] =
    sql"DELETE FROM characters WHERE id = $id".update.run.transact(xa).unit

object CharacterRepo:
  val layer: URLayer[Transactor[Task], CharacterRepo] = ZLayer.fromFunction(DoobieCharacterRepo(_))
