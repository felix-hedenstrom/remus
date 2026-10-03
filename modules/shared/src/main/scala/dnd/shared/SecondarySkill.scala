package dnd.shared

import sttp.tapir.codec.iron.given

final case class SecondarySkill(name: String, value: NonNegativeInt) derives Codec, Schema
