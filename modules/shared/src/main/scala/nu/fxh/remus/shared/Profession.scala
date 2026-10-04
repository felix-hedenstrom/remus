package nu.fxh.remus.shared

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

enum Profession derives Codec, Schema {

  case Artisan
  case Bard
  case Fighter
  case Hunter
  case Knight
  case Mage
  case Mariner
  case Merchant
  case Scholar
  case Thief
  case Custom(name: NonEmptyString)

}

object Profession:
  // `Custom` carries a parameter, so Scala can't derive `values` for this
  // enum. This is every case but `Custom`, for UI code that needs to iterate
  // over the known options.
  val known: IndexedSeq[Profession] =
    IndexedSeq(Artisan, Bard, Fighter, Hunter, Knight, Mage, Mariner, Merchant, Scholar, Thief)

  extension (p: Profession)
    def label: String = p match
      case Profession.Artisan      => "Artisan"
      case Profession.Bard         => "Bard"
      case Profession.Fighter      => "Fighter"
      case Profession.Hunter       => "Hunter"
      case Profession.Knight       => "Knight"
      case Profession.Mage         => "Mage"
      case Profession.Mariner      => "Mariner"
      case Profession.Merchant     => "Merchant"
      case Profession.Scholar      => "Scholar"
      case Profession.Thief        => "Thief"
      case Profession.Custom(name) => name

  /** Inverse of `label`. Trusts its input (DB-stored data written by `label`
    * itself), so an unrecognized string becomes `Custom` rather than failing.
    * Rows written before Profession existed may have a blank profession
    * column - that maps to a placeholder `Custom` instead of throwing.
    */
  def parse(raw: String): Profession = raw match
    case "Artisan"                    => Profession.Artisan
    case "Bard"                       => Profession.Bard
    case "Fighter"                    => Profession.Fighter
    case "Hunter"                     => Profession.Hunter
    case "Knight"                     => Profession.Knight
    case "Mage"                       => Profession.Mage
    case "Mariner"                    => Profession.Mariner
    case "Merchant"                   => Profession.Merchant
    case "Scholar"                    => Profession.Scholar
    case "Thief"                      => Profession.Thief
    case blank if blank.trim.isEmpty => Profession.Custom("Okänt".refineUnsafe[Not[Blank]])
    case other                        => Profession.Custom(other.refineUnsafe[Not[Blank]])