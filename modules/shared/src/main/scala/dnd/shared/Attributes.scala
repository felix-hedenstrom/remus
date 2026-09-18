package dnd.shared

enum Attribute derives CanEqual:
  case STR, CON, AGL, INT, WIL, CHA

final case class AttributeScore(value: Int, distressed: Boolean)

object AttributeScore:
  val default: AttributeScore = AttributeScore(10, distressed = false)

/** One row per Attribute, in a fixed order matching the physical sheet. */
final case class Attributes(
    str: AttributeScore,
    con: AttributeScore,
    agl: AttributeScore,
    int: AttributeScore,
    wil: AttributeScore,
    cha: AttributeScore
)

object Attributes:
  val default: Attributes = Attributes(
    AttributeScore.default,
    AttributeScore.default,
    AttributeScore.default,
    AttributeScore.default,
    AttributeScore.default,
    AttributeScore.default
  )
