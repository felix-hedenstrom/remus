package dnd.shared

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

/** A character's database row id. Always positive (SQLite autoincrement PK). */
type CharacterId = Long :| Positive
