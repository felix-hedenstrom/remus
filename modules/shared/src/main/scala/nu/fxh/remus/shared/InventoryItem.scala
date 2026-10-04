package nu.fxh.remus.shared

final case class InventoryItem(text: String, weight: WeightCategory) derives Codec, Schema
