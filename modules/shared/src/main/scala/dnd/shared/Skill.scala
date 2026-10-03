package dnd.shared

/** The fixed general + weapon skill list from the character sheet, each tied
  * to the attribute that governs it. Names follow the official Dragonbane
  * (the English localization of this ruleset) skill list. This is the single
  * source of truth for which skills exist — extend here if the ruleset needs
  * tweaking. Swedish labels belong in the client's i18n layer, not here.
  */
enum Skill(val attribute: Attribute) derives CanEqual, Codec, Schema:
  case Acrobatics extends Skill(Attribute.Agility)
  case Awareness extends Skill(Attribute.Intelligence)
  case Bartering extends Skill(Attribute.Charisma)
  case BeastLore extends Skill(Attribute.Intelligence)
  case Bluffing extends Skill(Attribute.Charisma)
  case Bushcraft extends Skill(Attribute.Intelligence)
  case Crafting extends Skill(Attribute.Strength)
  case Evade extends Skill(Attribute.Agility)
  case Healing extends Skill(Attribute.Intelligence)
  case HuntingAndFishing extends Skill(Attribute.Agility)
  case Languages extends Skill(Attribute.Intelligence)
  case MythsAndLegends extends Skill(Attribute.Intelligence)
  case Performance extends Skill(Attribute.Charisma)
  case Persuasion extends Skill(Attribute.Charisma)
  case Riding extends Skill(Attribute.Agility)
  case Seamanship extends Skill(Attribute.Intelligence)
  case SleightOfHand extends Skill(Attribute.Agility)
  case Sneaking extends Skill(Attribute.Agility)
  case SpotHidden extends Skill(Attribute.Intelligence)
  case Swimming extends Skill(Attribute.Agility)
  // Weapon skills
  case Axes extends Skill(Attribute.Strength)
  case Bows extends Skill(Attribute.Agility)
  case Brawling extends Skill(Attribute.Strength)
  case Crossbows extends Skill(Attribute.Agility)
  case Hammers extends Skill(Attribute.Strength)
  case Knives extends Skill(Attribute.Agility)
  case Slings extends Skill(Attribute.Agility)
  case Spears extends Skill(Attribute.Strength)
  case Staves extends Skill(Attribute.Agility)
  case Swords extends Skill(Attribute.Strength)

object Skill:
  // Display order matches the physical sheet, not alphabetical.
  val generalSkillsOrdered: List[Skill] = List(
    BeastLore, Bluffing, SleightOfHand, SpotHidden, Languages, Crafting, Acrobatics, HuntingAndFishing,
    Bartering, Healing, MythsAndLegends, Riding, Swimming, Seamanship, Sneaking, Evade, Performance,
    Awareness, Bushcraft, Persuasion
  )
  val weaponSkillsOrdered: List[Skill] = List(
    Crossbows, Hammers, Knives, Bows, Brawling, Slings, Spears, Staves, Swords, Axes
  )

  val weaponSkills: Set[Skill]  = weaponSkillsOrdered.toSet
  val generalSkills: Set[Skill] = generalSkillsOrdered.toSet
