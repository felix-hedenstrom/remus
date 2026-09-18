package dnd.shared

/** The fixed general + weapon skill list from the character sheet, each tied
  * to the attribute that governs it. Names follow the official Dragonbane
  * (the English localization of this ruleset) skill list. This is the single
  * source of truth for which skills exist — extend here if the ruleset needs
  * tweaking. Swedish labels belong in the client's i18n layer, not here.
  */
enum Skill(val attribute: Attribute) derives CanEqual:
  case Acrobatics extends Skill(Attribute.AGL)
  case Awareness extends Skill(Attribute.INT)
  case Bartering extends Skill(Attribute.CHA)
  case BeastLore extends Skill(Attribute.INT)
  case Bluffing extends Skill(Attribute.CHA)
  case Bushcraft extends Skill(Attribute.INT)
  case Crafting extends Skill(Attribute.STR)
  case Evade extends Skill(Attribute.AGL)
  case Healing extends Skill(Attribute.INT)
  case HuntingAndFishing extends Skill(Attribute.AGL)
  case Languages extends Skill(Attribute.INT)
  case MythsAndLegends extends Skill(Attribute.INT)
  case Performance extends Skill(Attribute.CHA)
  case Persuasion extends Skill(Attribute.CHA)
  case Riding extends Skill(Attribute.AGL)
  case Seamanship extends Skill(Attribute.INT)
  case SleightOfHand extends Skill(Attribute.AGL)
  case Sneaking extends Skill(Attribute.AGL)
  case SpotHidden extends Skill(Attribute.INT)
  case Swimming extends Skill(Attribute.AGL)
  // Weapon skills
  case Axes extends Skill(Attribute.STR)
  case Bows extends Skill(Attribute.AGL)
  case Brawling extends Skill(Attribute.STR)
  case Crossbows extends Skill(Attribute.AGL)
  case Hammers extends Skill(Attribute.STR)
  case Knives extends Skill(Attribute.AGL)
  case Slings extends Skill(Attribute.AGL)
  case Spears extends Skill(Attribute.STR)
  case Staves extends Skill(Attribute.AGL)
  case Swords extends Skill(Attribute.STR)

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
