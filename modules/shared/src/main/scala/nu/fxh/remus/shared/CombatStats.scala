package nu.fxh.remus.shared

import io.github.iltotore.iron.autoRefine
import sttp.tapir.codec.iron.given

final case class CombatStats(
    damageBonusStr: DamageBonus,
    damageBonusAgl: DamageBonus,
    movement: NonNegativeInt
) derives Codec, Schema

object CombatStats:
  val default: CombatStats = CombatStats(DamageBonus.None, DamageBonus.None, 10)
