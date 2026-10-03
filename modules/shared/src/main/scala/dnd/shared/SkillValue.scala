package dnd.shared

import sttp.tapir.codec.iron.given

final case class SkillValue(skill: Skill, value: NonNegativeInt) derives Codec, Schema
