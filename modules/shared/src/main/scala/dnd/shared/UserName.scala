package dnd.shared

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import sttp.tapir.Validator
import sttp.tapir.codec.iron.{PrimitiveValidatorForPredicate, ValidatorForPredicate}

/** A user's display name. Discord usernames are never blank. */
type UserName = String :| Not[Blank]

// tapir-iron (1.13.31, sttp/iron/codec/iron/TapirCodecIron.scala) hardcodes a
// validator for String :| Not[Empty] but not String :| Not[Blank]; its low-priority
// fallback for unmatched predicates requires the predicate to literally be
// DescribedAs[p, d] at the top level (IsDescription.derived), which Not[Blank] isn't
// (Blank itself is DescribedAs-wrapped, but Not[Blank] wraps that one level further
// out), so that inline match hard-fails instead of falling through. Supplying this
// given directly - the same mechanism the library uses for its own Not[Empty] case -
// sidesteps that path entirely. Declared at package level (not inside an object) so
// it's in scope package-wide without an import, the same way the rest of dnd.shared
// relies on same-package visibility.
given PrimitiveValidatorForPredicate[String, Not[Blank]] =
  ValidatorForPredicate.fromPrimitiveValidator[String, Not[Blank]](Validator.minLength(1))
