package nu.fxh.remus.shared

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

enum Species derives Codec, Schema {

  // Core rulebook kins.
  case Human
  case Dwarf
  case Elf
  case Halfling
  case Mallard
  case Wolfkin
  // From an official Dragonbane supplement, not the core rulebook.
  case Frogling
  case Satyr
  case Custom(name: NonEmptyString)

}

object Species:
  // `Custom` carries a parameter, so Scala can't derive `values` for this
  // enum. This is every case but `Custom`, for UI code that needs to iterate
  // over the known options.
  val known: IndexedSeq[Species] =
    IndexedSeq(Human, Dwarf, Elf, Halfling, Mallard, Wolfkin, Frogling, Satyr)

  extension (s: Species)
    def label: String = s match
      case Species.Human        => "Human"
      case Species.Dwarf        => "Dwarf"
      case Species.Elf          => "Elf"
      case Species.Halfling     => "Halfling"
      case Species.Mallard      => "Mallard"
      case Species.Wolfkin      => "Wolfkin"
      case Species.Frogling     => "Frogling"
      case Species.Satyr        => "Satyr"
      case Species.Custom(name) => name

  /** Inverse of `label`. Trusts its input (DB-stored data written by `label`
    * itself), so an unrecognized string becomes `Custom` rather than failing.
    * Rows written before Species existed may have a blank species column -
    * that maps to a placeholder `Custom` instead of throwing.
    */
  def parse(raw: String): Species = raw match
    case "Human"                      => Species.Human
    case "Dwarf"                       => Species.Dwarf
    case "Elf"                         => Species.Elf
    case "Halfling"                    => Species.Halfling
    case "Mallard"                     => Species.Mallard
    case "Wolfkin"                     => Species.Wolfkin
    case "Frogling"                    => Species.Frogling
    case "Satyr"                       => Species.Satyr
    case blank if blank.trim.isEmpty => Species.Custom("Okänd".refineUnsafe[Not[Blank]])
    case other                        => Species.Custom(other.refineUnsafe[Not[Blank]])