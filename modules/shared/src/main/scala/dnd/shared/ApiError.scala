package dnd.shared

sealed trait ApiError derives CanEqual:
  def message: String

object ApiError:
  final case class NotFound(message: String) extends ApiError
  final case class Forbidden(message: String) extends ApiError
  final case class Unauthorized(message: String) extends ApiError
  final case class ValidationError(message: String) extends ApiError
  final case class Conflict(message: String) extends ApiError
