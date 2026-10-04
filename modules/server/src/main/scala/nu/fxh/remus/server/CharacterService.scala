package nu.fxh.remus.server

import nu.fxh.remus.server.db.{CharacterRepo, CharacterRow}
import nu.fxh.remus.shared.*
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
      row            <- requireOwned(ownerId, id)
      current        <- parseSheet(row)
      processedSheet <- resolvePortrait(current, sheet)
      _ <- repo
             .update(id, processedSheet.header.name, processedSheet.header.species, processedSheet.header.profession, processedSheet.asJson.noSpaces)
             .orDie
    yield Character(id.refineUnsafe[Positive], processedSheet)

  // Only reprocesses the portrait when it actually changed, so repeated
  // saves of an untouched portrait don't re-encode the JPEG every time
  // (each re-encode is a fresh generation-loss pass).
  private def resolvePortrait(current: CharacterSheet, incoming: CharacterSheet): IO[ApiError, CharacterSheet] =
    if incoming.header.portrait == current.header.portrait then ZIO.succeed(incoming)
    else
      incoming.header.portrait match
        case None => ZIO.succeed(incoming)
        case Some(dataUrl) =>
          ZIO
            .attemptBlocking(PortraitProcessor.process(dataUrl))
            .orDie
            .flatMap {
              case Right(processed) => ZIO.succeed(incoming.copy(header = incoming.header.copy(portrait = Some(processed))))
              case Left(reason)     => ZIO.fail(ApiError.ValidationError(s"Kunde inte bearbeta bilden: $reason"))
            }

  def delete(ownerId: Long, id: Long): IO[ApiError, Unit] =
    requireOwned(ownerId, id).flatMap(row => repo.delete(row.id).orDie)

object CharacterService:
  val layer: URLayer[CharacterRepo, CharacterService] = ZLayer.fromFunction(CharacterServiceLive(_))
