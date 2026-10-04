package nu.fxh.remus.shared

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

/** An attribute's score, clamped to the 0-20 range used across the sheet. */
type AttributeValue = Int :| Interval.Closed[0, 20]
