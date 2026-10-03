package dnd.shared

final case class CharacterHeader(
                                  name: String,
                                  playerName: String,
                                  species: String,
                                  ageCategory: String,
                                  profession: String,
                                  weakness: String,
                                  appearance: String
                                ) derives Codec, Schema

object CharacterHeader:
  val default: CharacterHeader = CharacterHeader("", "", "", "", "", "", "")
