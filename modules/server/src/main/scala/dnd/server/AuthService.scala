package dnd.server

import com.password4j.Password
import dnd.server.db.{SessionRepo, UserRepo}
import dnd.shared.{ApiError, UserInfo}
import zio.*

import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

trait AuthService:
  def register(username: String, password: String): IO[ApiError, UserInfo]
  def login(username: String, password: String): IO[ApiError, (UserInfo, String, Instant)]
  def logout(token: String): UIO[Unit]
  def resolveSession(token: Option[String]): IO[ApiError, UserInfo]

final class AuthServiceLive(users: UserRepo, sessions: SessionRepo) extends AuthService:

  private val sessionTtl = java.time.Duration.ofDays(30)

  private def hashPassword(password: String): Task[String] =
    ZIO.attemptBlocking(Password.hash(password).addRandomSalt().withArgon2().getResult)

  private def verifyPassword(password: String, hash: String): Task[Boolean] =
    ZIO.attemptBlocking(Password.check(password, hash).withArgon2())

  private def randomToken(): UIO[String] = ZIO.succeed {
    val bytes = new Array[Byte](32)
    new SecureRandom().nextBytes(bytes)
    Base64.getUrlEncoder.withoutPadding.encodeToString(bytes)
  }

  def register(username: String, password: String): IO[ApiError, UserInfo] =
    for
      _        <- ZIO.when(username.trim.isEmpty)(ZIO.fail(ApiError.ValidationError("Username must not be empty")))
      _        <- ZIO.when(password.length < 8)(
                    ZIO.fail(ApiError.ValidationError("Password must be at least 8 characters"))
                  )
      existing <- users.findByUsername(username).orDie
      _        <- ZIO.when(existing.isDefined)(
                    ZIO.fail(ApiError.Conflict(s"Username '$username' is already taken"))
                  )
      hashed   <- hashPassword(password).orDie
      row      <- users.create(username, hashed).orDie
    yield UserInfo(row.id, row.username)

  def login(username: String, password: String): IO[ApiError, (UserInfo, String, Instant)] =
    for
      rowOpt    <- users.findByUsername(username).orDie
      row       <- ZIO.fromOption(rowOpt).orElseFail(ApiError.Unauthorized("Invalid username or password"))
      ok        <- verifyPassword(password, row.passwordHash).orDie
      _         <- ZIO.unless(ok)(ZIO.fail(ApiError.Unauthorized("Invalid username or password")))
      token     <- randomToken()
      expiresAt  = Instant.now().plus(sessionTtl)
      _         <- sessions.create(token, row.id, expiresAt.toEpochMilli).orDie
    yield (UserInfo(row.id, row.username), token, expiresAt)

  def logout(token: String): UIO[Unit] = sessions.delete(token).orDie

  def resolveSession(token: Option[String]): IO[ApiError, UserInfo] =
    for
      t          <- ZIO.fromOption(token).orElseFail(ApiError.Unauthorized("Not logged in"))
      sessionOpt <- sessions.find(t).orDie
      session    <- ZIO.fromOption(sessionOpt).orElseFail(ApiError.Unauthorized("Session expired or invalid"))
      _          <- ZIO.when(session.expiresAt < Instant.now().toEpochMilli)(
                      sessions.delete(t).orDie *> ZIO.fail(ApiError.Unauthorized("Session expired"))
                    )
      userOpt    <- users.findById(session.userId).orDie
      user       <- ZIO.fromOption(userOpt).orElseFail(ApiError.Unauthorized("Session user missing"))
    yield UserInfo(user.id, user.username)

object AuthService:
  val layer: URLayer[UserRepo & SessionRepo, AuthService] = ZLayer.fromFunction(AuthServiceLive(_, _))
