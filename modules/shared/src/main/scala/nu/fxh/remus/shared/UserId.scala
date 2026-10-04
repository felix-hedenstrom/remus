package nu.fxh.remus.shared

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

/** A user's database row id. Always positive (SQLite autoincrement PK). */
type UserId = Long :| Positive
