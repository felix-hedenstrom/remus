package dnd.shared

import sttp.model.StatusCode
import sttp.model.headers.CookieValueWithMeta
import sttp.tapir.*
import sttp.tapir.json.circe.*
import Json.given

object Endpoints:

  private val errorOutput: EndpointOutput[ApiError] =
    oneOf[ApiError](
      oneOfVariant(StatusCode.NotFound, jsonBody[ApiError.NotFound]),
      oneOfVariant(StatusCode.Forbidden, jsonBody[ApiError.Forbidden]),
      oneOfVariant(StatusCode.Unauthorized, jsonBody[ApiError.Unauthorized]),
      oneOfVariant(StatusCode.BadRequest, jsonBody[ApiError.ValidationError]),
      oneOfVariant(StatusCode.Conflict, jsonBody[ApiError.Conflict])
    )

  private val base = endpoint.errorOut(errorOutput)

  val register: PublicEndpoint[RegisterRequest, ApiError, UserInfo, Any] =
    base.post.in("api" / "auth" / "register").in(jsonBody[RegisterRequest]).out(jsonBody[UserInfo])

  val login: PublicEndpoint[LoginRequest, ApiError, (UserInfo, CookieValueWithMeta), Any] =
    base.post
      .in("api" / "auth" / "login")
      .in(jsonBody[LoginRequest])
      .out(jsonBody[UserInfo])
      .out(setCookie("session"))

  val logout: PublicEndpoint[Option[String], ApiError, CookieValueWithMeta, Any] =
    base.post.in("api" / "auth" / "logout").in(cookie[Option[String]]("session")).out(setCookie("session"))

  /** All character endpoints require the session cookie; the security logic
    * resolves it to a UserInfo (or fails with Unauthorized) before the
    * regular server logic runs.
    */
  private val secured = endpoint.securityIn(cookie[Option[String]]("session")).errorOut(errorOutput)

  val listCharacters: Endpoint[Option[String], Unit, ApiError, List[CharacterSummary], Any] =
    secured.get.in("api" / "characters").out(jsonBody[List[CharacterSummary]])

  val createCharacter: Endpoint[Option[String], Unit, ApiError, Character, Any] =
    secured.post.in("api" / "characters").out(jsonBody[Character])

  val getCharacter: Endpoint[Option[String], Long, ApiError, Character, Any] =
    secured.get.in("api" / "characters" / path[Long]("id")).out(jsonBody[Character])

  val updateCharacter: Endpoint[Option[String], (Long, CharacterSheet), ApiError, Character, Any] =
    secured.put
      .in("api" / "characters" / path[Long]("id"))
      .in(jsonBody[CharacterSheet])
      .out(jsonBody[Character])

  val deleteCharacter: Endpoint[Option[String], Long, ApiError, Unit, Any] =
    secured.delete.in("api" / "characters" / path[Long]("id"))

  val all: List[AnyEndpoint] =
    List(register, login, logout, listCharacters, createCharacter, getCharacter, updateCharacter, deleteCharacter)
