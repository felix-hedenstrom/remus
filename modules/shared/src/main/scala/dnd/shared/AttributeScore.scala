package dnd.shared

import io.github.iltotore.iron.autoRefine
import sttp.tapir.codec.iron.given

final case class AttributeScore(value: AttributeValue, distressed: Boolean) derives Codec, Schema

object AttributeScore:
  val default: AttributeScore = AttributeScore(10, distressed = false)
