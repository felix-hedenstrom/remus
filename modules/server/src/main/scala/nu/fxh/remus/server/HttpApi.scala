package nu.fxh.remus.server

import nu.fxh.remus.shared.Endpoints
import sttp.model.StatusCode
import sttp.model.headers.CookieValueWithMeta
import sttp.tapir.files.*
import sttp.tapir.ztapir.*
import zio.*

import java.time.{Duration, Instant}

object HttpApi:

  private def sessionCookie(token: String, expiresAt: Instant): CookieValueWithMeta =
    CookieValueWithMeta.unsafeApply(
      value = token,
      path = Some("/"),
      httpOnly = true,
      secure = false, // served over plain HTTP for self-hosting; set true behind HTTPS
      maxAge = Some(Duration.between(Instant.now(), expiresAt).getSeconds)
    )

  private val clearedCookie: CookieValueWithMeta =
    CookieValueWithMeta.unsafeApply(value = "", path = Some("/"), maxAge = Some(0))

  /** Serves the compiled Scala.js client bundle from the classpath.
    * `defaultFile` makes any unrecognized path fall back to index.html, so
    * this doubles as an SPA fallback even though the app doesn't use
    * deep-linkable client-side routes yet.
    */
  private val staticEndpoint: ZServerEndpoint[Any, Any] =
    staticResourcesGetServerEndpoint[Task](emptyInput)(
      getClass.getClassLoader,
      "static",
      FilesOptions.default[Task].defaultFile(List("index.html"))
    )

  def endpoints(
    auth: AuthService,
    discord: DiscordOAuthService,
    characters: CharacterService
  ): List[ZServerEndpoint[Any, Any]] =
    List(
      Endpoints.discordLogin.zServerLogic(_ => discord.beginLogin().map(url => (StatusCode.Found, url))),
      Endpoints.discordCallback.zServerLogic { case (code, state, error) =>
        discord
          .completeLogin(code, state, error)
          .map { case (_, token, expiresAt) => (StatusCode.Found, "/", sessionCookie(token, expiresAt)) }
          .catchAll(_ => ZIO.succeed((StatusCode.Found, "/?login_error=1", clearedCookie)))
      },
      Endpoints.logout.zServerLogic { tokenOpt =>
        ZIO.foreachDiscard(tokenOpt)(auth.logout).as(clearedCookie)
      },
      Endpoints.me.zServerSecurityLogic(auth.resolveSession).serverLogic(user => _ => ZIO.succeed(user)),
      Endpoints.listCharacters
        .zServerSecurityLogic(auth.resolveSession)
        .serverLogic(user => _ => characters.list(user.id)),
      Endpoints.createCharacter
        .zServerSecurityLogic(auth.resolveSession)
        .serverLogic(user => _ => characters.create(user.id)),
      Endpoints.getCharacter
        .zServerSecurityLogic(auth.resolveSession)
        .serverLogic(user => id => characters.get(user.id, id)),
      Endpoints.updateCharacter
        .zServerSecurityLogic(auth.resolveSession)
        .serverLogic(user => { case (id, sheet) => characters.update(user.id, id, sheet) }),
      Endpoints.deleteCharacter
        .zServerSecurityLogic(auth.resolveSession)
        .serverLogic(user => id => characters.delete(user.id, id)),
      staticEndpoint
    )
