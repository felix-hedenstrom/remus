package dnd.shared

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

enum Profession derives Codec, Schema {

  case Warrior
  case Custom(name: NonEmptyString)

}

object Profession:
  extension (p: Profession)
    def label: String = p match
      case Profession.Warrior      => "Warrior"
      case Profession.Custom(name) => name

  /** Inverse of `label`. Trusts its input (DB-stored data written by `label`
    * itself), so an unrecognized string becomes `Custom` rather than failing.
    * Rows written before Profession existed may have a blank profession
    * column - that maps to a placeholder `Custom` instead of throwing.
    */
  def parse(raw: String): Profession = raw match
    case "Warrior"                    => Profession.Warrior
    case blank if blank.trim.isEmpty => Profession.Custom("Okänt".refineUnsafe[Not[Blank]])
    case other                        => Profession.Custom(other.refineUnsafe[Not[Blank]])