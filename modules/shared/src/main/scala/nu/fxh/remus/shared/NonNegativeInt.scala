package nu.fxh.remus.shared

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

/** A count/resource value that can never go negative. */
type NonNegativeInt = Int :| GreaterEqual[0]
