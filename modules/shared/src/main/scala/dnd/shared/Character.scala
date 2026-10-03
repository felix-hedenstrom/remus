package dnd.shared

import sttp.tapir.codec.iron.given

case class Character(
                      id: CharacterId,
                      sheet: CharacterSheet
                    ) derives Codec, Schema
