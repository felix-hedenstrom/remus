package nu.fxh.remus.shared

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

type NonEmptyString = String :| Not[Blank]
