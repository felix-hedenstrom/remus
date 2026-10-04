package nu.fxh.remus.shared

import io.github.iltotore.iron.autoRefine

final case class CharacterHeader(
                                  name: NonEmptyString,
                                  species: Species,
                                  ageCategory: AgeCategory,
                                  profession: Profession,
                                  weakness: NonEmptyString,
                                  appearance: NonEmptyString,
                                  portrait: Option[String]
                                ) derives Codec, Schema

object CharacterHeader:
  val default: CharacterHeader = CharacterHeader("<name>", Species.Human, AgeCategory.Adult, Profession.Fighter, "<weakness>", "<appearance>", None)
