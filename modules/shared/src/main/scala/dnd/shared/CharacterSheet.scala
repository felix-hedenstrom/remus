package dnd.shared

final case class CombatStats(
    damageBonusStr: String,
    damageBonusAgl: String,
    movement: Int
)
object CombatStats:
  val default: CombatStats = CombatStats("-", "-", 10)

final case class ResourceTrack(max: Int, current: Int)
object ResourceTrack:
  val default: ResourceTrack = ResourceTrack(10, 10)

final case class DeathRolls(successes: Int, failures: Int)
object DeathRolls:
  val default: DeathRolls = DeathRolls(0, 0)

final case class Resources(
    willpower: ResourceTrack,
    bodyPoints: ResourceTrack,
    deathRolls: DeathRolls
)
object Resources:
  val default: Resources = Resources(ResourceTrack.default, ResourceTrack.default, DeathRolls.default)

final case class SkillValue(skill: Skill, value: Int)

final case class SecondarySkill(name: String, value: Int)

final case class InventoryItem(text: String)

final case class Inventory(
    carryCapacity: Int,
    items: List[InventoryItem],
    keepsake: String
)
object Inventory:
  val default: Inventory = Inventory(carryCapacity = 5, items = Nil, keepsake = "")

final case class Currency(gold: Int, silver: Int, copper: Int)
object Currency:
  val default: Currency = Currency(0, 0, 0)

final case class ArmorPenalties(sneaking: Boolean, evade: Boolean, acrobatics: Boolean)
object ArmorPenalties:
  val none: ArmorPenalties = ArmorPenalties(false, false, false)

final case class HelmetPenalties(spotHidden: Boolean, rangedAttacks: Boolean)
object HelmetPenalties:
  val none: HelmetPenalties = HelmetPenalties(false, false)

final case class Armor(
    protection: Int,
    penalties: ArmorPenalties,
    helmetProtection: Int,
    helmetPenalties: HelmetPenalties
)
object Armor:
  val default: Armor = Armor(0, ArmorPenalties.none, 0, HelmetPenalties.none)

final case class Weapon(
    name: String,
    grip: String,
    range: String,
    damage: String,
    breakValue: String,
    properties: String
)

final case class CharacterHeader(
    name: String,
    playerName: String,
    species: String,
    ageCategory: String,
    profession: String,
    weakness: String,
    appearance: String
)
object CharacterHeader:
  val default: CharacterHeader = CharacterHeader("", "", "", "", "", "", "")

/** The full interactive sheet — everything but id/ownership/timestamps,
  * which live on CharacterSummary/the DB row instead.
  */
final case class CharacterSheet(
    header: CharacterHeader,
    attributes: Attributes,
    combatStats: CombatStats,
    resources: Resources,
    skills: List[SkillValue],
    secondarySkills: List[SecondarySkill],
    abilitiesText: String,
    inventory: Inventory,
    currency: Currency,
    armor: Armor,
    weapons: List[Weapon]
)

object CharacterSheet:
  val blank: CharacterSheet = CharacterSheet(
    header = CharacterHeader.default,
    attributes = Attributes.default,
    combatStats = CombatStats.default,
    resources = Resources.default,
    skills = Skill.values.map(SkillValue(_, 0)).toList,
    secondarySkills = Nil,
    abilitiesText = "",
    inventory = Inventory.default,
    currency = Currency.default,
    armor = Armor.default,
    weapons = Nil
  )

final case class CharacterId(value: Long) extends AnyVal

final case class CharacterSummary(
    id: CharacterId,
    name: String,
    species: String,
    profession: String
)

final case class Character(
    id: CharacterId,
    sheet: CharacterSheet
)
