package dnd.shared

// The full set of weapon features/properties used across the official
// Dragonbane weapon tables (matching the terms from Fria Ligan's licensed
// Foundry VTT system - see DoD.weaponFeatureTypes), not a hand-picked subset.
enum WeaponProperty derives Codec, Schema {

  case Bludgeoning, Long, Mounted, NoDamageBonus, NoParry, Piercing, Quiver, Slashing,
    Subtle, Thrown, Toppling, Shield, Unarmed, Enchanted1, Enchanted2, Enchanted3,
    Penetrating1, Penetrating2, Penetrating3

}
