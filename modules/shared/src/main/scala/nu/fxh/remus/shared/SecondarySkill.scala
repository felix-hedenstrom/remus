package nu.fxh.remus.shared

import sttp.tapir.codec.iron.given

final case class SecondarySkill(
    name: String,
    value: NonNegativeInt,
    attribute: Attribute,
    markedForAdvancement: Boolean
) derives Codec, Schema
