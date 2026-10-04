package nu.fxh.remus.server.db

import nu.fxh.remus.shared.*
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import io.github.iltotore.iron.*
import zio.*
import zio.interop.catz.*

final case class CharacterFields(
    name: String,
    species: Species,
    ageCategory: AgeCategory,
    profession: Profession,
    weakness: String,
    appearance: String,
    portrait: Option[String],
    strengthValue: Int,
    strengthDistressed: Boolean,
    constitutionValue: Int,
    constitutionDistressed: Boolean,
    agilityValue: Int,
    agilityDistressed: Boolean,
    intelligenceValue: Int,
    intelligenceDistressed: Boolean,
    willValue: Int,
    willDistressed: Boolean,
    charismaValue: Int,
    charismaDistressed: Boolean,
    damageBonusStr: DamageBonus,
    damageBonusAgl: DamageBonus,
    movement: Int,
    willpowerMax: Int,
    willpowerCurrent: Int,
    bodyPointsMax: Int,
    bodyPointsCurrent: Int,
    deathRollsSuccesses: Int,
    deathRollsFailures: Int,
    gold: Int,
    silver: Int,
    copper: Int,
    armorType: String,
    armorProtection: Int,
    armorPenaltySneaking: Boolean,
    armorPenaltyEvade: Boolean,
    armorPenaltyAcrobatics: Boolean,
    helmetType: String,
    helmetProtection: Int,
    helmetPenaltySpotHidden: Boolean,
    helmetPenaltyRangedAttacks: Boolean,
    carryCapacity: Int,
    keepsake: String
)

final case class CharacterRow(id: Long, ownerUserId: Long, fields: CharacterFields)

/** The lists that used to live inside `sheet_json`, now each their own
  * child table. Reuses the shared domain types directly (they're already
  * exactly the right shape) rather than introducing parallel row types.
  */
final case class CharacterChildren(
    skills: List[SkillValue],
    secondarySkills: List[SecondarySkill],
    abilities: List[Ability],
    inventoryItems: List[InventoryItem],
    weapons: List[Weapon]
)

final case class CharacterRecord(row: CharacterRow, children: CharacterChildren)

trait CharacterRepo:
  def listByOwner(ownerUserId: Long): Task[List[CharacterRow]]
  def find(id: Long): Task[Option[CharacterRecord]]
  def create(ownerUserId: Long, fields: CharacterFields, children: CharacterChildren): Task[CharacterRecord]
  def update(id: Long, fields: CharacterFields, children: CharacterChildren): Task[Unit]
  def delete(id: Long): Task[Unit]

final class DoobieCharacterRepo(xa: Transactor[Task]) extends CharacterRepo:

  // Enum columns are stored as their case name / label, as a database
  // implementation detail hidden behind this repo's typed API - same
  // pattern as Species/Profession below, just without a Custom case.
  private given Meta[Species]        = Meta[String].timap(Species.parse)(_.label)
  private given Meta[Profession]     = Meta[String].timap(Profession.parse)(_.label)
  private given Meta[AgeCategory]    = Meta[String].timap(AgeCategory.valueOf)(_.toString)
  private given Meta[DamageBonus]    = Meta[String].timap(DamageBonus.valueOf)(_.toString)
  private given Meta[Grip]           = Meta[String].timap(Grip.valueOf)(_.toString)
  private given Meta[WeightCategory] = Meta[String].timap(WeightCategory.valueOf)(_.toString)
  private given Meta[WeaponProperty] = Meta[String].timap(WeaponProperty.valueOf)(_.toString)
  private given Meta[Attribute]      = Meta[String].timap(Attribute.valueOf)(_.toString)
  private given Meta[Skill]          = Meta[String].timap(Skill.valueOf)(_.toString)

  private type HeaderCols     = (String, Species, AgeCategory, Profession, String, String, Option[String])
  private type AttributesCols = (Int, Boolean, Int, Boolean, Int, Boolean, Int, Boolean, Int, Boolean, Int, Boolean)
  private type CombatCols     = (DamageBonus, DamageBonus, Int)
  private type ResourcesCols  = (Int, Int, Int, Int, Int, Int)
  private type CurrencyCols   = (Int, Int, Int)
  private type ArmorCols      = (String, Int, Boolean, Boolean, Boolean, String, Int, Boolean, Boolean)
  private type InventoryCols  = (Int, String)
  private type RowCols = (Long, Long, HeaderCols, AttributesCols, CombatCols, ResourcesCols, CurrencyCols, ArmorCols, InventoryCols)

  private val rowColumns = Fragment.const(
    "id, owner_user_id, " +
      "name, species, age_category, profession, weakness, appearance, portrait, " +
      "attr_strength_value, attr_strength_distressed, attr_constitution_value, attr_constitution_distressed, " +
      "attr_agility_value, attr_agility_distressed, attr_intelligence_value, attr_intelligence_distressed, " +
      "attr_will_value, attr_will_distressed, attr_charisma_value, attr_charisma_distressed, " +
      "damage_bonus_str, damage_bonus_agl, movement, " +
      "willpower_max, willpower_current, body_points_max, body_points_current, " +
      "death_rolls_successes, death_rolls_failures, " +
      "gold, silver, copper, " +
      "armor_type, armor_protection, armor_penalty_sneaking, armor_penalty_evade, armor_penalty_acrobatics, " +
      "helmet_type, helmet_protection, helmet_penalty_spot_hidden, helmet_penalty_ranged_attacks, " +
      "carry_capacity, keepsake"
  )

  private def toRow(t: RowCols): CharacterRow =
    val (id, ownerUserId, header, attrs, combat, resources, currency, armor, inventory) = t
    val (name, species, ageCategory, profession, weakness, appearance, portrait)        = header
    val (strV, strD, conV, conD, aglV, aglD, intV, intD, willV, willD, chaV, chaD)       = attrs
    val (dmgStr, dmgAgl, movement)                                                      = combat
    val (wpMax, wpCur, bpMax, bpCur, drS, drF)                                          = resources
    val (gold, silver, copper)                                                          = currency
    val (armorType, armorProt, armorSneak, armorEvade, armorAcro, helmType, helmProt, helmSpot, helmRanged) = armor
    val (carryCap, keepsake)                                                            = inventory
    CharacterRow(
      id,
      ownerUserId,
      CharacterFields(
        name,
        species,
        ageCategory,
        profession,
        weakness,
        appearance,
        portrait,
        strV,
        strD,
        conV,
        conD,
        aglV,
        aglD,
        intV,
        intD,
        willV,
        willD,
        chaV,
        chaD,
        dmgStr,
        dmgAgl,
        movement,
        wpMax,
        wpCur,
        bpMax,
        bpCur,
        drS,
        drF,
        gold,
        silver,
        copper,
        armorType,
        armorProt,
        armorSneak,
        armorEvade,
        armorAcro,
        helmType,
        helmProt,
        helmSpot,
        helmRanged,
        carryCap,
        keepsake
      )
    )

  private def selectRow(id: Long): ConnectionIO[Option[CharacterRow]] =
    (fr"SELECT" ++ rowColumns ++ fr"FROM characters WHERE id = $id").query[RowCols].option.map(_.map(toRow))

  private def insertRow(ownerUserId: Long, f: CharacterFields): ConnectionIO[Long] =
    sql"""INSERT INTO characters(
            owner_user_id, name, species, age_category, profession, weakness, appearance, portrait,
            attr_strength_value, attr_strength_distressed, attr_constitution_value, attr_constitution_distressed,
            attr_agility_value, attr_agility_distressed, attr_intelligence_value, attr_intelligence_distressed,
            attr_will_value, attr_will_distressed, attr_charisma_value, attr_charisma_distressed,
            damage_bonus_str, damage_bonus_agl, movement,
            willpower_max, willpower_current, body_points_max, body_points_current,
            death_rolls_successes, death_rolls_failures,
            gold, silver, copper,
            armor_type, armor_protection, armor_penalty_sneaking, armor_penalty_evade, armor_penalty_acrobatics,
            helmet_type, helmet_protection, helmet_penalty_spot_hidden, helmet_penalty_ranged_attacks,
            carry_capacity, keepsake
          ) VALUES (
            $ownerUserId, ${f.name}, ${f.species}, ${f.ageCategory}, ${f.profession}, ${f.weakness}, ${f.appearance}, ${f.portrait},
            ${f.strengthValue}, ${f.strengthDistressed}, ${f.constitutionValue}, ${f.constitutionDistressed},
            ${f.agilityValue}, ${f.agilityDistressed}, ${f.intelligenceValue}, ${f.intelligenceDistressed},
            ${f.willValue}, ${f.willDistressed}, ${f.charismaValue}, ${f.charismaDistressed},
            ${f.damageBonusStr}, ${f.damageBonusAgl}, ${f.movement},
            ${f.willpowerMax}, ${f.willpowerCurrent}, ${f.bodyPointsMax}, ${f.bodyPointsCurrent},
            ${f.deathRollsSuccesses}, ${f.deathRollsFailures},
            ${f.gold}, ${f.silver}, ${f.copper},
            ${f.armorType}, ${f.armorProtection}, ${f.armorPenaltySneaking}, ${f.armorPenaltyEvade}, ${f.armorPenaltyAcrobatics},
            ${f.helmetType}, ${f.helmetProtection}, ${f.helmetPenaltySpotHidden}, ${f.helmetPenaltyRangedAttacks},
            ${f.carryCapacity}, ${f.keepsake}
          )""".update.withUniqueGeneratedKeys[Long]("id")

  private def updateRow(id: Long, f: CharacterFields): ConnectionIO[Unit] =
    sql"""UPDATE characters SET
            name = ${f.name}, species = ${f.species}, age_category = ${f.ageCategory}, profession = ${f.profession},
            weakness = ${f.weakness}, appearance = ${f.appearance}, portrait = ${f.portrait},
            attr_strength_value = ${f.strengthValue}, attr_strength_distressed = ${f.strengthDistressed},
            attr_constitution_value = ${f.constitutionValue}, attr_constitution_distressed = ${f.constitutionDistressed},
            attr_agility_value = ${f.agilityValue}, attr_agility_distressed = ${f.agilityDistressed},
            attr_intelligence_value = ${f.intelligenceValue}, attr_intelligence_distressed = ${f.intelligenceDistressed},
            attr_will_value = ${f.willValue}, attr_will_distressed = ${f.willDistressed},
            attr_charisma_value = ${f.charismaValue}, attr_charisma_distressed = ${f.charismaDistressed},
            damage_bonus_str = ${f.damageBonusStr}, damage_bonus_agl = ${f.damageBonusAgl}, movement = ${f.movement},
            willpower_max = ${f.willpowerMax}, willpower_current = ${f.willpowerCurrent},
            body_points_max = ${f.bodyPointsMax}, body_points_current = ${f.bodyPointsCurrent},
            death_rolls_successes = ${f.deathRollsSuccesses}, death_rolls_failures = ${f.deathRollsFailures},
            gold = ${f.gold}, silver = ${f.silver}, copper = ${f.copper},
            armor_type = ${f.armorType}, armor_protection = ${f.armorProtection},
            armor_penalty_sneaking = ${f.armorPenaltySneaking}, armor_penalty_evade = ${f.armorPenaltyEvade},
            armor_penalty_acrobatics = ${f.armorPenaltyAcrobatics},
            helmet_type = ${f.helmetType}, helmet_protection = ${f.helmetProtection},
            helmet_penalty_spot_hidden = ${f.helmetPenaltySpotHidden}, helmet_penalty_ranged_attacks = ${f.helmetPenaltyRangedAttacks},
            carry_capacity = ${f.carryCapacity}, keepsake = ${f.keepsake}
          WHERE id = $id""".update.run.map(_ => ())

  private def selectSkills(characterId: Long): ConnectionIO[List[SkillValue]] =
    sql"SELECT skill, value, marked_for_advancement FROM character_skills WHERE character_id = $characterId"
      .query[(Skill, Int, Boolean)]
      .to[List]
      .map(_.map { case (skill, value, marked) => SkillValue(skill, value.refineUnsafe, marked) })

  private def selectSecondarySkills(characterId: Long): ConnectionIO[List[SecondarySkill]] =
    sql"""SELECT name, value, attribute, marked_for_advancement FROM character_secondary_skills
          WHERE character_id = $characterId ORDER BY position"""
      .query[(String, Int, Attribute, Boolean)]
      .to[List]
      .map(_.map { case (name, value, attribute, marked) => SecondarySkill(name, value.refineUnsafe, attribute, marked) })

  private def selectAbilities(characterId: Long): ConnectionIO[List[Ability]] =
    sql"SELECT text FROM character_abilities WHERE character_id = $characterId ORDER BY position"
      .query[String]
      .to[List]
      .map(_.map(Ability(_)))

  private def selectInventoryItems(characterId: Long): ConnectionIO[List[InventoryItem]] =
    sql"SELECT text, weight FROM character_inventory_items WHERE character_id = $characterId ORDER BY position"
      .query[(String, WeightCategory)]
      .to[List]
      .map(_.map { case (text, weight) => InventoryItem(text, weight) })

  private def selectWeapons(characterId: Long): ConnectionIO[List[Weapon]] =
    for
      cols <- sql"""SELECT id, name, grip, weapon_range, damage, break_value
                    FROM character_weapons WHERE character_id = $characterId ORDER BY position"""
                .query[(Long, String, Grip, String, String, String)]
                .to[List]
      props <- sql"""SELECT cw.id, cwp.property FROM character_weapon_properties cwp
                     JOIN character_weapons cw ON cw.id = cwp.weapon_id
                     WHERE cw.character_id = $characterId"""
                 .query[(Long, WeaponProperty)]
                 .to[List]
      propsByWeaponId = props.groupMap(_._1)(_._2).view.mapValues(_.toSet).toMap
    yield cols.map { case (id, name, grip, range, damage, breakValue) =>
      Weapon(name, grip, range, damage, breakValue, propsByWeaponId.getOrElse(id, Set.empty))
    }

  private def selectChildren(characterId: Long): ConnectionIO[CharacterChildren] =
    for
      skills          <- selectSkills(characterId)
      secondarySkills <- selectSecondarySkills(characterId)
      abilities       <- selectAbilities(characterId)
      inventoryItems  <- selectInventoryItems(characterId)
      weapons         <- selectWeapons(characterId)
    yield CharacterChildren(skills, secondarySkills, abilities, inventoryItems, weapons)

  private def deleteChildren(characterId: Long): ConnectionIO[Unit] =
    for
      _ <- sql"DELETE FROM character_skills WHERE character_id = $characterId".update.run
      _ <- sql"DELETE FROM character_secondary_skills WHERE character_id = $characterId".update.run
      _ <- sql"DELETE FROM character_abilities WHERE character_id = $characterId".update.run
      _ <- sql"DELETE FROM character_inventory_items WHERE character_id = $characterId".update.run
      // character_weapon_properties cascades via ON DELETE CASCADE (foreign_keys=on).
      _ <- sql"DELETE FROM character_weapons WHERE character_id = $characterId".update.run
    yield ()

  private def insertChildren(characterId: Long, children: CharacterChildren): ConnectionIO[Unit] =
    val skillRows = children.skills.map(s => (characterId, s.skill, s.value: Int, s.markedForAdvancement))
    val secondarySkillRows = children.secondarySkills.zipWithIndex.map { case (s, pos) =>
      (characterId, pos, s.name, s.value: Int, s.attribute, s.markedForAdvancement)
    }
    val abilityRows = children.abilities.zipWithIndex.map { case (a, pos) => (characterId, pos, a.text) }
    val inventoryItemRows = children.inventoryItems.zipWithIndex.map { case (i, pos) => (characterId, pos, i.text, i.weight) }
    val weaponRows = children.weapons.zipWithIndex.map { case (w, pos) =>
      (characterId, pos, w.name, w.grip, w.range, w.damage, w.breakValue)
    }
    for
      _ <-
        if skillRows.isEmpty then 0.pure[ConnectionIO]
        else
          Update[(Long, Skill, Int, Boolean)](
            "INSERT INTO character_skills(character_id, skill, value, marked_for_advancement) VALUES (?, ?, ?, ?)"
          ).updateMany(skillRows)
      _ <-
        if secondarySkillRows.isEmpty then 0.pure[ConnectionIO]
        else
          Update[(Long, Int, String, Int, Attribute, Boolean)](
            "INSERT INTO character_secondary_skills(character_id, position, name, value, attribute, marked_for_advancement) VALUES (?, ?, ?, ?, ?, ?)"
          ).updateMany(secondarySkillRows)
      _ <-
        if abilityRows.isEmpty then 0.pure[ConnectionIO]
        else
          Update[(Long, Int, String)](
            "INSERT INTO character_abilities(character_id, position, text) VALUES (?, ?, ?)"
          ).updateMany(abilityRows)
      _ <-
        if inventoryItemRows.isEmpty then 0.pure[ConnectionIO]
        else
          Update[(Long, Int, String, WeightCategory)](
            "INSERT INTO character_inventory_items(character_id, position, text, weight) VALUES (?, ?, ?, ?)"
          ).updateMany(inventoryItemRows)
      _ <-
        if weaponRows.isEmpty then 0.pure[ConnectionIO]
        else
          Update[(Long, Int, String, Grip, String, String, String)](
            "INSERT INTO character_weapons(character_id, position, name, grip, weapon_range, damage, break_value) VALUES (?, ?, ?, ?, ?, ?, ?)"
          ).updateMany(weaponRows)
      weaponIds <-
        if weaponRows.isEmpty then List.empty[(Int, Long)].pure[ConnectionIO]
        else
          sql"SELECT position, id FROM character_weapons WHERE character_id = $characterId".query[(Int, Long)].to[List]
      idByPosition = weaponIds.toMap
      propertyRows = children.weapons.zipWithIndex.flatMap { case (w, pos) =>
        w.properties.toList.map(p => (idByPosition(pos), p))
      }
      _ <-
        if propertyRows.isEmpty then 0.pure[ConnectionIO]
        else
          Update[(Long, WeaponProperty)](
            "INSERT INTO character_weapon_properties(weapon_id, property) VALUES (?, ?)"
          ).updateMany(propertyRows)
    yield ()

  def listByOwner(ownerUserId: Long): Task[List[CharacterRow]] =
    (fr"SELECT" ++ rowColumns ++ fr"FROM characters WHERE owner_user_id = $ownerUserId")
      .query[RowCols]
      .to[List]
      .transact(xa)
      .map(_.map(toRow))

  def find(id: Long): Task[Option[CharacterRecord]] =
    selectRow(id)
      .flatMap {
        case None      => none[CharacterRecord].pure[ConnectionIO]
        case Some(row) => selectChildren(id).map(children => Some(CharacterRecord(row, children)))
      }
      .transact(xa)

  def create(ownerUserId: Long, fields: CharacterFields, children: CharacterChildren): Task[CharacterRecord] =
    (for
      id <- insertRow(ownerUserId, fields)
      _  <- insertChildren(id, children)
    yield CharacterRecord(CharacterRow(id, ownerUserId, fields), children)).transact(xa)

  def update(id: Long, fields: CharacterFields, children: CharacterChildren): Task[Unit] =
    (for
      _ <- updateRow(id, fields)
      _ <- deleteChildren(id)
      _ <- insertChildren(id, children)
    yield ()).transact(xa)

  def delete(id: Long): Task[Unit] =
    // character_skills/secondary_skills/abilities/inventory_items/weapons
    // (and transitively weapon_properties) all cascade via ON DELETE CASCADE.
    sql"DELETE FROM characters WHERE id = $id".update.run.transact(xa).unit

object CharacterRepo:
  val layer: URLayer[Transactor[Task], CharacterRepo] = ZLayer.fromFunction(DoobieCharacterRepo(_))
