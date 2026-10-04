package nu.fxh.remus.client

import nu.fxh.remus.shared.{AgeCategory, Attribute, DamageBonus, Grip, Profession, Skill, Species, WeaponProperty, WeightCategory}

/** Swedish display labels, matching the physical Drakar och Demoner sheet.
  * The domain model stays in English (the official Dragonbane terms); this
  * is purely a presentation-layer translation back to the terms Felix's
  * group actually uses at the table.
  */
object Labels:

  // Species/Profession only have their first real case plus a Custom(name)
  // escape hatch so far - extend these matches as more official options are added.
  def species(s: Species): String = s match
    case Species.Human        => "Människa"
    case Species.Custom(name) => name

  def profession(p: Profession): String = p match
    case Profession.Warrior     => "Krigare"
    case Profession.Custom(name) => name

  def ageCategory(a: AgeCategory): String = a match
    case AgeCategory.Young => "Ung"
    case AgeCategory.Adult => "Vuxen"
    case AgeCategory.Old   => "Gammal"

  def grip(g: Grip): String = g match
    case Grip.OneHanded => "1H"
    case Grip.TwoHanded => "2H"

  def weightCategory(w: WeightCategory): String = w match
    case WeightCategory.Light  => "Lätt"
    case WeightCategory.Normal => "Normal"
    case WeightCategory.Heavy  => "Tung"

  // Official Fria Ligan Swedish terms for the weapon feature/property enum,
  // taken from the licensed Foundry VTT system's sv.json localization.
  def weaponProperty(p: WeaponProperty): String = p match
    case WeaponProperty.Bludgeoning   => "Krossande"
    case WeaponProperty.Long          => "Lång"
    case WeaponProperty.Mounted       => "Riddjur"
    case WeaponProperty.NoDamageBonus => "Ingen skadebonus"
    case WeaponProperty.NoParry       => "Ej parering"
    case WeaponProperty.Piercing      => "Stickande"
    case WeaponProperty.Quiver        => "Koger"
    case WeaponProperty.Slashing      => "Huggande"
    case WeaponProperty.Subtle        => "Smidig"
    case WeaponProperty.Thrown        => "Kast"
    case WeaponProperty.Toppling      => "Fällande"
    case WeaponProperty.Shield        => "Sköld"
    case WeaponProperty.Unarmed       => "Obeväpnad"
    case WeaponProperty.Enchanted1    => "Förtrollad (1)"
    case WeaponProperty.Enchanted2    => "Förtrollad (2)"
    case WeaponProperty.Enchanted3    => "Förtrollad (3)"
    case WeaponProperty.Penetrating1  => "Penetrerande (1)"
    case WeaponProperty.Penetrating2  => "Penetrerande (2)"
    case WeaponProperty.Penetrating3  => "Penetrerande (3)"

  // Die-bonus notation is language-agnostic, so this just delegates to
  // DamageBonus.label - kept as its own function for symmetry with the
  // other enum Labels and as the one place to localize if that changes.
  def damageBonus(d: DamageBonus): String = d.label

  def attribute(a: Attribute): String = a match
    case Attribute.Strength => "STY"
    case Attribute.Constitution => "FYS"
    case Attribute.Agility => "SMI"
    case Attribute.Intelligence => "INT"
    case Attribute.Will => "PSY"
    case Attribute.Charisma => "KAR"

  def skill(s: Skill): String = s match
    case Skill.Acrobatics        => "Hoppa & klättra"
    case Skill.Awareness         => "Upptäcka fara"
    case Skill.Bartering         => "Köpslå"
    case Skill.BeastLore         => "Bestiologi"
    case Skill.Bluffing          => "Bluffa"
    case Skill.Bushcraft         => "Vildmarksvana"
    case Skill.Crafting          => "Hantverk"
    case Skill.Evade             => "Undvika"
    case Skill.Healing           => "Läkekonst"
    case Skill.HuntingAndFishing => "Jakt & fiske"
    case Skill.Languages         => "Främmande språk"
    case Skill.MythsAndLegends   => "Myter & legender"
    case Skill.Performance       => "Uppträda"
    case Skill.Persuasion        => "Övertala"
    case Skill.Riding            => "Rida"
    case Skill.Seamanship        => "Sjökunnighet"
    case Skill.SleightOfHand     => "Fingerfärdighet"
    case Skill.Sneaking          => "Smyga"
    case Skill.SpotHidden        => "Finna dolda ting"
    case Skill.Swimming          => "Simma"
    case Skill.Axes              => "Yxa"
    case Skill.Bows              => "Pilbåge"
    case Skill.Brawling          => "Slagsmål"
    case Skill.Crossbows         => "Armborst"
    case Skill.Hammers           => "Hammare"
    case Skill.Knives            => "Kniv"
    case Skill.Slings            => "Slunga"
    case Skill.Spears            => "Spjut"
    case Skill.Staves            => "Stav"
    case Skill.Swords            => "Svärd"
