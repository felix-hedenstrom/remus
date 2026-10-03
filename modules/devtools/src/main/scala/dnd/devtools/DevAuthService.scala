package dnd.devtools

import dnd.server.AuthService
import dnd.server.db.{SessionRepo, UserRepo}
import dnd.shared.{ApiError, UserInfo}
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import zio.*

/** Decorates whatever `AuthService` is in the environment, always resolving
  * to a fixed, real `users` row instead of checking the session cookie, so
  * the app is usable locally without a Discord login.
  */
final class DevAuthService(delegate: AuthService, users: UserRepo) extends AuthService:
  export delegate.{completeDiscordLogin, logout}

  def resolveSession(token: Option[String]): IO[ApiError, UserInfo] =
    users
      .findOrCreateByDiscordId("dev-bypass", "dev")
      .orDie
      .map(row => UserInfo(row.id.refineUnsafe[Positive], row.username.refineUnsafe[Not[Blank]]))

object DevAuthService:
  private val layer: URLayer[AuthService & UserRepo, AuthService] = ZLayer.derive[DevAuthService]

  /** Entrypoint-ready layer: builds the real `AuthServiceLive` internally,
    * then wraps it, so callers only need to supply `UserRepo & SessionRepo`
    * (matching `AuthService.layer`'s shape) rather than threading a second
    * `AuthService` instance through by hand.
    */
  val bypassLayer: URLayer[UserRepo & SessionRepo, AuthService] =
    (AuthService.layer ++ ZLayer.service[UserRepo]) >>> layer
