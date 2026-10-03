package dnd.shared

import io.github.iltotore.iron.autoRefine

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
) derives Codec, Schema

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
