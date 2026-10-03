package dnd.shared

final case class Weapon(
                         name: String,
                         grip: String,
                         range: String,
                         damage: String,
                         breakValue: String,
                         properties: String
                       ) derives Codec, Schema
