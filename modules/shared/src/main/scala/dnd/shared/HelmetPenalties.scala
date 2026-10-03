package dnd.shared

final case class HelmetPenalties(spotHidden: Boolean, rangedAttacks: Boolean) derives Codec, Schema

object HelmetPenalties:
  val none: HelmetPenalties = HelmetPenalties(false, false)
