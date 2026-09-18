package dnd.shared

final case class RegisterRequest(username: String, password: String)
final case class LoginRequest(username: String, password: String)
final case class UserInfo(id: Long, username: String)
