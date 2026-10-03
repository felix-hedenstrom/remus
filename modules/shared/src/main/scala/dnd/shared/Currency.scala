package dnd.shared

import io.github.iltotore.iron.autoRefine
import sttp.tapir.codec.iron.given

final case class Currency(gold: NonNegativeInt, silver: NonNegativeInt, copper: NonNegativeInt) derives Codec, Schema

object Currency:
  val default: Currency = Currency(0, 0, 0)
