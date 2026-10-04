package dnd.shared

final case class InventoryItem(text: String, weight: WeightCategory) derives Codec, Schema
