package dnd.shared

import io.circe.Codec
import sttp.tapir.Schema
import sttp.tapir.codec.iron.given

case class CharacterSummary(
                             id: CharacterId,
                             name: String,
                             species: Species,
                             profession: Profession
                           ) derives Codec, Schema
