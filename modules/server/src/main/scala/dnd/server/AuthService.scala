package dnd.server

import dnd.server.db.{SessionRepo, UserRepo}
import dnd.shared.{ApiError, UserInfo}
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import zio.*

import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

trait AuthService:
  def completeDiscordLogin(discordId: String, username: String): IO[ApiError, (UserInfo, String, Instant)]
  def logout(token: String): UIO[Unit]
  def resolveSession(token: Option[String]): IO[ApiError, UserInfo]

final class AuthServiceLive(users: UserRepo, sessions: SessionRepo) extends AuthService:

  private val sessionTtl = java.time.Duration.ofDays(30)

  private def randomToken(): UIO[String] = ZIO.succeed {
    val bytes = new Array[Byte](32)
    new SecureRandom().nextBytes(bytes)
    Base64.getUrlEncoder.withoutPadding.encodeToString(bytes)
  }

  def completeDiscordLogin(discordId: String, username: String): IO[ApiError, (UserInfo, String, Instant)] =
    for
      row       <- users.findOrCreateByDiscordId(discordId, username).orDie
      token     <- randomToken()
      expiresAt  = Instant.now().plus(sessionTtl)
      _         <- sessions.create(token, row.id, expiresAt.toEpochMilli).orDie
    yield (UserInfo(row.id.refineUnsafe[Positive], row.username.refineUnsafe[Not[Blank]]), token, expiresAt)

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
    yield UserInfo(user.id.refineUnsafe[Positive], user.username.refineUnsafe[Not[Blank]])

object AuthService:
  val layer: URLayer[UserRepo & SessionRepo, AuthService] = ZLayer.derive[AuthServiceLive]
