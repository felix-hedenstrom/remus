package dnd.shared

/** One row per Attribute, in a fixed order matching the physical sheet. */
final case class Attributes(
    strength: AttributeScore,
    constitution: AttributeScore,
    agility: AttributeScore,
    intelligence: AttributeScore,
    will: AttributeScore,
    charisma: AttributeScore
) derives Codec, Schema

object Attributes:
  val default: Attributes = Attributes(
    AttributeScore.default,
    AttributeScore.default,
    AttributeScore.default,
    AttributeScore.default,
    AttributeScore.default,
    AttributeScore.default
  )
