package dnd.shared

final case class Weapon(
                         name: String,
                         grip: Grip,
                         range: String,
                         damage: String,
                         breakValue: String,
                         properties: Set[WeaponProperty]
                       ) derives Codec, Schema
