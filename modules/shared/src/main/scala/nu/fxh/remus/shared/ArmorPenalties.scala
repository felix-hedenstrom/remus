package nu.fxh.remus.shared

final case class ArmorPenalties(sneaking: Boolean, evade: Boolean, acrobatics: Boolean) derives Codec, Schema

object ArmorPenalties:
  val none: ArmorPenalties = ArmorPenalties(false, false, false)
