package nu.fxh.remus.shared

import sttp.tapir.codec.iron.given

final case class SkillValue(skill: Skill, value: NonNegativeInt, markedForAdvancement: Boolean) derives Codec, Schema
