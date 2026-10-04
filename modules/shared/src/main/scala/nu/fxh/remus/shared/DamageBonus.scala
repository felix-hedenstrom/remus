package nu.fxh.remus.shared

// Damage bonus is a straight lookup from STR (melee) / AGL (ranged) score
// into this fixed three-step table - players don't choose or homebrew a
// value, so unlike Species/Profession there's no Custom escape hatch here.
enum DamageBonus derives Codec, Schema {
  case None
  case D4
  case D6
}

object DamageBonus:
  extension (d: DamageBonus)
    def label: String = d match
      case DamageBonus.None => "-"
      case DamageBonus.D4   => "+1D4"
      case DamageBonus.D6   => "+1D6"
