package dnd.shared

import io.circe.Codec
import sttp.tapir.Schema

sealed trait ApiError derives CanEqual:
  def message: String

object ApiError:

  case class NotFound(message: String) extends ApiError derives Schema, Codec

  case class Forbidden(message: String) extends ApiError derives Schema, Codec

  case class Unauthorized(message: String) extends ApiError derives Schema, Codec

  case class ValidationError(message: String) extends ApiError derives Schema, Codec

  case class Conflict(message: String) extends ApiError derives Schema, Codec
