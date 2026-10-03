package dnd.shared

import io.github.iltotore.iron.autoRefine
import sttp.tapir.codec.iron.given

final case class CombatStats(
    damageBonusStr: String,
    damageBonusAgl: String,
    movement: NonNegativeInt
) derives Codec, Schema

object CombatStats:
  val default: CombatStats = CombatStats("-", "-", 10)
