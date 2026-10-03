package dnd.shared

import io.github.iltotore.iron.autoRefine
import sttp.tapir.codec.iron.given

final case class Armor(
    protection: NonNegativeInt,
    penalties: ArmorPenalties,
    helmetProtection: NonNegativeInt,
    helmetPenalties: HelmetPenalties
) derives Codec, Schema

object Armor:
  val default: Armor = Armor(0, ArmorPenalties.none, 0, HelmetPenalties.none)
