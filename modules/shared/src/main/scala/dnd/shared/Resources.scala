package dnd.shared

final case class Resources(
    willpower: ResourceTrack,
    bodyPoints: ResourceTrack,
    deathRolls: DeathRolls
) derives Codec, Schema

object Resources:
  val default: Resources = Resources(ResourceTrack.default, ResourceTrack.default, DeathRolls.default)
