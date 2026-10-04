package nu.fxh.remus.shared

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

/** A user's display name. Discord usernames are never blank. */
type UserName = String :| Not[Blank]

