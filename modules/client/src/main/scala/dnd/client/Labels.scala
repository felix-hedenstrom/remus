package dnd.client

import dnd.shared.{Attribute, Skill}

/** Swedish display labels, matching the physical Drakar och Demoner sheet.
  * The domain model stays in English (the official Dragonbane terms); this
  * is purely a presentation-layer translation back to the terms Felix's
  * group actually uses at the table.
  */
object Labels:

  def attribute(a: Attribute): String = a match
    case Attribute.STR => "STY"
    case Attribute.CON => "FYS"
    case Attribute.AGL => "SMI"
    case Attribute.INT => "INT"
    case Attribute.WIL => "PSY"
    case Attribute.CHA => "KAR"

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
