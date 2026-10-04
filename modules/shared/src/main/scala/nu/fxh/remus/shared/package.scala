package nu.fxh.remus.shared

import io.github.iltotore.iron.Not
import io.github.iltotore.iron.constraint.all.Blank
import sttp.tapir.Validator
import sttp.tapir.codec.iron.{PrimitiveValidatorForPredicate, ValidatorForPredicate}

export io.circe.Codec
export sttp.tapir.Schema
export io.github.iltotore.iron.circe.given


given PrimitiveValidatorForPredicate[String, Not[Blank]] =
  ValidatorForPredicate.fromPrimitiveValidator[String, Not[Blank]](Validator.minLength(1))

export sttp.tapir.codec.iron.ironTypeSchema
