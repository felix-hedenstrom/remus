package dnd.server

import dnd.server.db.{CharacterRepo, CharacterRow}
import dnd.shared.*
import io.circe.parser.decode
import io.circe.syntax.*
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import zio.*

trait CharacterService:
  def list(ownerId: Long): IO[ApiError, List[CharacterSummary]]
  def create(ownerId: Long): IO[ApiError, Character]
  def get(ownerId: Long, id: Long): IO[ApiError, Character]
  def update(ownerId: Long, id: Long, sheet: CharacterSheet): IO[ApiError, Character]
  def delete(ownerId: Long, id: Long): IO[ApiError, Unit]

final class CharacterServiceLive(repo: CharacterRepo) extends CharacterService:

  private def parseSheet(row: CharacterRow): IO[ApiError, CharacterSheet] =
    ZIO
      .fromEither(decode[CharacterSheet](row.sheetJson))
      .orElseFail(ApiError.ValidationError(s"Corrupt character sheet data for id ${row.id}"))

  private def toCharacter(row: CharacterRow): IO[ApiError, Character] =
    parseSheet(row).map(sheet => Character(row.id.refineUnsafe[Positive], sheet))

  private def requireOwned(ownerId: Long, id: Long): IO[ApiError, CharacterRow] =
    for
      rowOpt <- repo.find(id).orDie
      row    <- ZIO.fromOption(rowOpt).orElseFail(ApiError.NotFound(s"No character with id $id"))
      _      <- ZIO.unless(row.ownerUserId == ownerId)(ZIO.fail(ApiError.Forbidden("You do not own this character")))
    yield row

  def list(ownerId: Long): IO[ApiError, List[CharacterSummary]] =
    repo
      .listByOwner(ownerId)
      .orDie
      .map(_.map(row => CharacterSummary(row.id.refineUnsafe[Positive], row.name, row.species, row.profession)))

  def create(ownerId: Long): IO[ApiError, Character] =
    val blank = CharacterSheet.blank
    repo
      .create(ownerId, blank.header.name, blank.header.species, blank.header.profession, blank.asJson.noSpaces)
      .orDie
      .map(row => Character(row.id.refineUnsafe[Positive], blank))

  def get(ownerId: Long, id: Long): IO[ApiError, Character] =
    requireOwned(ownerId, id).flatMap(toCharacter)

  def update(ownerId: Long, id: Long, sheet: CharacterSheet): IO[ApiError, Character] =
    for
      _ <- requireOwned(ownerId, id)
      _ <- repo.update(id, sheet.header.name, sheet.header.species, sheet.header.profession, sheet.asJson.noSpaces).orDie
    yield Character(id.refineUnsafe[Positive], sheet)

  def delete(ownerId: Long, id: Long): IO[ApiError, Unit] =
    requireOwned(ownerId, id).flatMap(row => repo.delete(row.id).orDie)

object CharacterService:
  val layer: URLayer[CharacterRepo, CharacterService] = ZLayer.fromFunction(CharacterServiceLive(_))
