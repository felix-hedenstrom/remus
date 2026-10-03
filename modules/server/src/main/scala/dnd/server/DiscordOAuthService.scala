package dnd.server

import dnd.shared.{ApiError, UserInfo}
import io.circe.generic.auto.*
import io.circe.parser.decode
import sttp.client4.*
import zio.*

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

trait DiscordOAuthService:
  def beginLogin(): UIO[String]
  def completeLogin(code: Option[String], state: Option[String], error: Option[String])
      : IO[ApiError, (UserInfo, String, Instant)]

/** Handles the "Login with Discord" OAuth2 flow: builds the authorize URL,
  * and on callback exchanges the code for an access token, fetches the
  * Discord user, and hands off to AuthService to find-or-create the local
  * account and start a session.
  *
  * The CSRF `state` param is a self-verifying signed token (timestamp +
  * HMAC-SHA256 over it, keyed by the Discord client secret) rather than a DB
  * row, so there's nothing to persist or clean up.
  */
final class DiscordOAuthServiceLive(
  auth: AuthService,
  backend: Backend[Task],
  clientId: String,
  clientSecret: String,
  redirectUri: String
) extends DiscordOAuthService:

  private val stateTtl = java.time.Duration.ofMinutes(10)

  private case class TokenResponse(access_token: String)
  private case class DiscordUser(id: String, username: String)

  private def hmac(message: String): String =
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
    Base64.getUrlEncoder.withoutPadding.encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)))

  private def signState(): String =
    val ts = Instant.now().toEpochMilli.toString
    s"$ts.${hmac(ts)}"

  private def verifyState(state: String): Boolean =
    state.split("\\.", 2) match
      case Array(ts, sig) =>
        val expected     = hmac(ts).getBytes(StandardCharsets.UTF_8)
        val actual        = sig.getBytes(StandardCharsets.UTF_8)
        val signatureOk   = expected.length == actual.length && MessageDigest.isEqual(expected, actual)
        val notExpired    =
          scala.util.Try(ts.toLong).toOption.exists(millis => Instant.ofEpochMilli(millis).plus(stateTtl).isAfter(Instant.now()))
        signatureOk && notExpired
      case _ => false

  def beginLogin(): UIO[String] = ZIO.succeed {
    uri"https://discord.com/api/oauth2/authorize"
      .addParam("client_id", clientId)
      .addParam("redirect_uri", redirectUri)
      .addParam("response_type", "code")
      .addParam("scope", "identify")
      .addParam("state", signState())
      .toString
  }

  def completeLogin(
    code: Option[String],
    state: Option[String],
    error: Option[String]
  ): IO[ApiError, (UserInfo, String, Instant)] =
    for
      _      <- ZIO.when(error.isDefined)(ZIO.fail(ApiError.Unauthorized("Discord login was cancelled")))
      c      <- ZIO.fromOption(code).orElseFail(ApiError.Unauthorized("Missing code"))
      s      <- ZIO.fromOption(state).orElseFail(ApiError.Unauthorized("Missing state"))
      _      <- ZIO.unless(verifyState(s))(ZIO.fail(ApiError.Unauthorized("Invalid or expired state")))
      token  <- exchangeCode(c).orDie
      user   <- fetchUser(token).orDie
      result <- auth.completeDiscordLogin(user.id, user.username)
    yield result

  private def exchangeCode(code: String): Task[String] =
    basicRequest
      .post(uri"https://discord.com/api/oauth2/token")
      .body(
        Map(
          "client_id"     -> clientId,
          "client_secret" -> clientSecret,
          "grant_type"    -> "authorization_code",
          "code"          -> code,
          "redirect_uri"  -> redirectUri
        )
      )
      .response(asStringAlways)
      .send(backend)
      .flatMap(resp => ZIO.fromEither(decode[TokenResponse](resp.body)))
      .map(_.access_token)

  private def fetchUser(accessToken: String): Task[DiscordUser] =
    basicRequest
      .get(uri"https://discord.com/api/users/@me")
      .header("Authorization", s"Bearer $accessToken")
      .response(asStringAlways)
      .send(backend)
      .flatMap(resp => ZIO.fromEither(decode[DiscordUser](resp.body)))

object DiscordOAuthService:
  val layer: URLayer[AuthService & Backend[Task] & AppConfig, DiscordOAuthService] =
    ZLayer.fromFunction { (auth: AuthService, backend: Backend[Task], config: AppConfig) =>
      DiscordOAuthServiceLive(auth, backend, config.discordClientId, config.discordClientSecret, config.discordRedirectUri)
    }
