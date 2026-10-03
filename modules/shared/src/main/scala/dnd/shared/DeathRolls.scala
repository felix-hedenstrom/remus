package dnd.shared

import io.github.iltotore.iron.autoRefine
import sttp.tapir.codec.iron.given

final case class DeathRolls(successes: NonNegativeInt, failures: NonNegativeInt) derives Codec, Schema

object DeathRolls:
  val default: DeathRolls = DeathRolls(0, 0)
