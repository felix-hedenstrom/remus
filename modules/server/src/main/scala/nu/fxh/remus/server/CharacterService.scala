package nu.fxh.remus.server

import nu.fxh.remus.server.db.{CharacterChildren, CharacterFields, CharacterRecord, CharacterRepo}
import nu.fxh.remus.shared.*
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
  import CharacterService.{assemble, decompose}

  private def toCharacter(record: CharacterRecord): Character =
    Character(record.row.id.refineUnsafe[Positive], assemble(record))

  private def requireOwned(ownerId: Long, id: Long): IO[ApiError, CharacterRecord] =
    for
      recordOpt <- repo.find(id).orDie
      record    <- ZIO.fromOption(recordOpt).orElseFail(ApiError.NotFound(s"No character with id $id"))
      _         <- ZIO.unless(record.row.ownerUserId == ownerId)(ZIO.fail(ApiError.Forbidden("You do not own this character")))
    yield record

  def list(ownerId: Long): IO[ApiError, List[CharacterSummary]] =
    repo
      .listByOwner(ownerId)
      .orDie
      .map(_.map(row => CharacterSummary(row.id.refineUnsafe[Positive], row.fields.name, row.fields.species, row.fields.profession)))

  def create(ownerId: Long): IO[ApiError, Character] =
    val blank               = CharacterSheet.blank
    val (fields, children) = decompose(blank)
    repo.create(ownerId, fields, children).orDie.map(toCharacter)

  def get(ownerId: Long, id: Long): IO[ApiError, Character] =
    requireOwned(ownerId, id).map(toCharacter)

  def update(ownerId: Long, id: Long, sheet: CharacterSheet): IO[ApiError, Character] =
    for
      record              <- requireOwned(ownerId, id)
      current              = assemble(record)
      processedSheet      <- resolvePortrait(current, sheet)
      (fields, children) = decompose(processedSheet)
      _ <- repo.update(id, fields, children).orDie
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
    requireOwned(ownerId, id).flatMap(record => repo.delete(record.row.id).orDie)

object CharacterService:
  val layer: URLayer[CharacterRepo, CharacterService] = ZLayer.fromFunction(CharacterServiceLive(_))

  /** Reassembles the full sheet from the flattened row + child tables.
    * `.refineUnsafe` trusts this data (written by `decompose` below, via
    * this repo), the same way `CharacterRow`'s Species/Profession `Meta`
    * instances already trust DB-stored enum labels.
    */
  private[server] def assemble(record: CharacterRecord): CharacterSheet =
    val f = record.row.fields
    CharacterSheet(
      header = CharacterHeader(
        f.name.refineUnsafe[Not[Blank]],
        f.species,
        f.ageCategory,
        f.profession,
        f.weakness.refineUnsafe[Not[Blank]],
        f.appearance.refineUnsafe[Not[Blank]],
        f.portrait
      ),
      attributes = Attributes(
        strength = AttributeScore(f.strengthValue.refineUnsafe[Interval.Closed[0, 20]], f.strengthDistressed),
        constitution = AttributeScore(f.constitutionValue.refineUnsafe[Interval.Closed[0, 20]], f.constitutionDistressed),
        agility = AttributeScore(f.agilityValue.refineUnsafe[Interval.Closed[0, 20]], f.agilityDistressed),
        intelligence = AttributeScore(f.intelligenceValue.refineUnsafe[Interval.Closed[0, 20]], f.intelligenceDistressed),
        will = AttributeScore(f.willValue.refineUnsafe[Interval.Closed[0, 20]], f.willDistressed),
        charisma = AttributeScore(f.charismaValue.refineUnsafe[Interval.Closed[0, 20]], f.charismaDistressed)
      ),
      combatStats = CombatStats(f.damageBonusStr, f.damageBonusAgl, f.movement.refineUnsafe[GreaterEqual[0]]),
      resources = Resources(
        willpower = ResourceTrack(f.willpowerMax.refineUnsafe[GreaterEqual[0]], f.willpowerCurrent.refineUnsafe[GreaterEqual[0]]),
        bodyPoints = ResourceTrack(f.bodyPointsMax.refineUnsafe[GreaterEqual[0]], f.bodyPointsCurrent.refineUnsafe[GreaterEqual[0]]),
        deathRolls = DeathRolls(f.deathRollsSuccesses.refineUnsafe[GreaterEqual[0]], f.deathRollsFailures.refineUnsafe[GreaterEqual[0]])
      ),
      skills = record.children.skills.sortBy(s => Skill.values.indexOf(s.skill)),
      secondarySkills = record.children.secondarySkills,
      abilities = record.children.abilities,
      inventory = Inventory(f.carryCapacity.refineUnsafe[GreaterEqual[0]], record.children.inventoryItems, f.keepsake),
      currency = Currency(
        f.gold.refineUnsafe[GreaterEqual[0]],
        f.silver.refineUnsafe[GreaterEqual[0]],
        f.copper.refineUnsafe[GreaterEqual[0]]
      ),
      armor = Armor(
        armorType = f.armorType,
        protection = f.armorProtection.refineUnsafe[GreaterEqual[0]],
        penalties = ArmorPenalties(f.armorPenaltySneaking, f.armorPenaltyEvade, f.armorPenaltyAcrobatics),
        helmetType = f.helmetType,
        helmetProtection = f.helmetProtection.refineUnsafe[GreaterEqual[0]],
        helmetPenalties = HelmetPenalties(f.helmetPenaltySpotHidden, f.helmetPenaltyRangedAttacks)
      ),
      weapons = record.children.weapons
    )

  /** The only place list order becomes durable: the repo re-reads each
    * child table ordered by its stored `position`, assigned from this
    * list's order on write.
    */
  private[server] def decompose(sheet: CharacterSheet): (CharacterFields, CharacterChildren) =
    val fields = CharacterFields(
      name = sheet.header.name,
      species = sheet.header.species,
      ageCategory = sheet.header.ageCategory,
      profession = sheet.header.profession,
      weakness = sheet.header.weakness,
      appearance = sheet.header.appearance,
      portrait = sheet.header.portrait,
      strengthValue = sheet.attributes.strength.value,
      strengthDistressed = sheet.attributes.strength.distressed,
      constitutionValue = sheet.attributes.constitution.value,
      constitutionDistressed = sheet.attributes.constitution.distressed,
      agilityValue = sheet.attributes.agility.value,
      agilityDistressed = sheet.attributes.agility.distressed,
      intelligenceValue = sheet.attributes.intelligence.value,
      intelligenceDistressed = sheet.attributes.intelligence.distressed,
      willValue = sheet.attributes.will.value,
      willDistressed = sheet.attributes.will.distressed,
      charismaValue = sheet.attributes.charisma.value,
      charismaDistressed = sheet.attributes.charisma.distressed,
      damageBonusStr = sheet.combatStats.damageBonusStr,
      damageBonusAgl = sheet.combatStats.damageBonusAgl,
      movement = sheet.combatStats.movement,
      willpowerMax = sheet.resources.willpower.max,
      willpowerCurrent = sheet.resources.willpower.current,
      bodyPointsMax = sheet.resources.bodyPoints.max,
      bodyPointsCurrent = sheet.resources.bodyPoints.current,
      deathRollsSuccesses = sheet.resources.deathRolls.successes,
      deathRollsFailures = sheet.resources.deathRolls.failures,
      gold = sheet.currency.gold,
      silver = sheet.currency.silver,
      copper = sheet.currency.copper,
      armorType = sheet.armor.armorType,
      armorProtection = sheet.armor.protection,
      armorPenaltySneaking = sheet.armor.penalties.sneaking,
      armorPenaltyEvade = sheet.armor.penalties.evade,
      armorPenaltyAcrobatics = sheet.armor.penalties.acrobatics,
      helmetType = sheet.armor.helmetType,
      helmetProtection = sheet.armor.helmetProtection,
      helmetPenaltySpotHidden = sheet.armor.helmetPenalties.spotHidden,
      helmetPenaltyRangedAttacks = sheet.armor.helmetPenalties.rangedAttacks,
      carryCapacity = sheet.inventory.carryCapacity,
      keepsake = sheet.inventory.keepsake
    )
    val children = CharacterChildren(
      skills = sheet.skills,
      secondarySkills = sheet.secondarySkills,
      abilities = sheet.abilities,
      inventoryItems = sheet.inventory.items,
      weapons = sheet.weapons
    )
    (fields, children)
