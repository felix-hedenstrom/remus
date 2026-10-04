package nu.fxh.remus.shared

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

enum Species derives Codec, Schema {

  case Human
  case Custom(name: NonEmptyString)

}

object Species:
  extension (s: Species)
    def label: String = s match
      case Species.Human        => "Human"
      case Species.Custom(name) => name

  /** Inverse of `label`. Trusts its input (DB-stored data written by `label`
    * itself), so an unrecognized string becomes `Custom` rather than failing.
    * Rows written before Species existed may have a blank species column -
    * that maps to a placeholder `Custom` instead of throwing.
    */
  def parse(raw: String): Species = raw match
    case "Human"                      => Species.Human
    case blank if blank.trim.isEmpty => Species.Custom("Okänd".refineUnsafe[Not[Blank]])
    case other                        => Species.Custom(other.refineUnsafe[Not[Blank]])