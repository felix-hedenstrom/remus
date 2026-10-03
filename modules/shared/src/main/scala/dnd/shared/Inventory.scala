package dnd.shared

import io.github.iltotore.iron.autoRefine
import sttp.tapir.codec.iron.given

final case class Inventory(
    carryCapacity: NonNegativeInt,
    items: List[InventoryItem],
    keepsake: String
) derives Codec, Schema

object Inventory:
  val default: Inventory = Inventory(carryCapacity = 5, items = Nil, keepsake = "")
