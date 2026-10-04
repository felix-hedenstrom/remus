package nu.fxh.remus.shared

/** One row per Attribute, in a fixed order matching the physical sheet. */
final case class Attributes(
    strength: AttributeScore,
    constitution: AttributeScore,
    agility: AttributeScore,
    intelligence: AttributeScore,
    will: AttributeScore,
    charisma: AttributeScore
) derives Codec, Schema:
  def score(attribute: Attribute): AttributeScore = attribute match
    case Attribute.Strength     => strength
    case Attribute.Constitution => constitution
    case Attribute.Agility      => agility
    case Attribute.Intelligence => intelligence
    case Attribute.Will         => will
    case Attribute.Charisma     => charisma

object Attributes:
  val default: Attributes = Attributes(
    AttributeScore.default,
    AttributeScore.default,
    AttributeScore.default,
    AttributeScore.default,
    AttributeScore.default,
    AttributeScore.default
  )
