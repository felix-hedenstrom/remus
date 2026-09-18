package dnd.shared

import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.*
import sttp.tapir.Schema

/** Central home for all wire (de)serialization. Keeping it in one file makes
  * the derivation order/dependencies easy to see; given resolution in Scala
  * doesn't care about textual order within the object, only that everything
  * referenced is in scope.
  */
object Json:

  private def enumCodec[E <: reflect.Enum](values: Array[E]): Codec[E] =
    Codec.from(
      Decoder.decodeString.emap(s =>
        values.find(_.toString == s).toRight(s"Unknown value: $s")
      ),
      Encoder.encodeString.contramap(_.toString)
    )

  given Codec[Attribute] = enumCodec(Attribute.values)
  given Codec[Skill] = enumCodec(Skill.values)

  given Codec[CharacterId] =
    Codec.from(Decoder.decodeLong.map(CharacterId.apply), Encoder.encodeLong.contramap(_.value))

  // Explicit tapir Schemas for the wire representations above, so the
  // auto-derived OpenAPI docs match how these values are actually encoded
  // (plain strings/numbers) rather than tapir's default product/coproduct guess.
  given Schema[Attribute] = Schema.string
  given Schema[Skill] = Schema.string
  given Schema[CharacterId] = Schema.schemaForLong.as[CharacterId]

  given Codec[AttributeScore] = deriveCodec
  given Schema[AttributeScore] = Schema.derived

  given Codec[Attributes] = deriveCodec
  given Schema[Attributes] = Schema.derived

  given Codec[CombatStats] = deriveCodec
  given Schema[CombatStats] = Schema.derived

  given Codec[ResourceTrack] = deriveCodec
  given Schema[ResourceTrack] = Schema.derived

  given Codec[DeathRolls] = deriveCodec
  given Schema[DeathRolls] = Schema.derived

  given Codec[Resources] = deriveCodec
  given Schema[Resources] = Schema.derived

  given Codec[SkillValue] = deriveCodec
  given Schema[SkillValue] = Schema.derived

  given Codec[SecondarySkill] = deriveCodec
  given Schema[SecondarySkill] = Schema.derived

  given Codec[InventoryItem] = deriveCodec
  given Schema[InventoryItem] = Schema.derived

  given Codec[Inventory] = deriveCodec
  given Schema[Inventory] = Schema.derived

  given Codec[Currency] = deriveCodec
  given Schema[Currency] = Schema.derived

  given Codec[ArmorPenalties] = deriveCodec
  given Schema[ArmorPenalties] = Schema.derived

  given Codec[HelmetPenalties] = deriveCodec
  given Schema[HelmetPenalties] = Schema.derived

  given Codec[Armor] = deriveCodec
  given Schema[Armor] = Schema.derived

  given Codec[Weapon] = deriveCodec
  given Schema[Weapon] = Schema.derived

  given Codec[CharacterHeader] = deriveCodec
  given Schema[CharacterHeader] = Schema.derived

  given Codec[CharacterSheet] = deriveCodec
  given Schema[CharacterSheet] = Schema.derived

  given Codec[CharacterSummary] = deriveCodec
  given Schema[CharacterSummary] = Schema.derived

  given Codec[Character] = deriveCodec
  given Schema[Character] = Schema.derived

  given Codec[RegisterRequest] = deriveCodec
  given Schema[RegisterRequest] = Schema.derived

  given Codec[LoginRequest] = deriveCodec
  given Schema[LoginRequest] = Schema.derived

  given Codec[UserInfo] = deriveCodec
  given Schema[UserInfo] = Schema.derived

  given Codec[ApiError.NotFound] = deriveCodec
  given Schema[ApiError.NotFound] = Schema.derived

  given Codec[ApiError.Forbidden] = deriveCodec
  given Schema[ApiError.Forbidden] = Schema.derived

  given Codec[ApiError.Unauthorized] = deriveCodec
  given Schema[ApiError.Unauthorized] = Schema.derived

  given Codec[ApiError.ValidationError] = deriveCodec
  given Schema[ApiError.ValidationError] = Schema.derived

  given Codec[ApiError.Conflict] = deriveCodec
  given Schema[ApiError.Conflict] = Schema.derived
