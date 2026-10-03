package dnd.client

import dnd.shared.*
import sttp.client4.impl.zio.FetchZioBackend
import sttp.model.headers.CookieValueWithMeta
import sttp.tapir.client.sttp4.SttpClientInterpreter
import zio.*

/** Thin wrapper around the shared Tapir endpoints, calling them over the
  * browser's fetch API. Requests are same-origin, so the browser attaches
  * the session cookie automatically — we don't need to thread it through
  * by hand, which is why every security input below is `None`.
  */
object Api:
  private val backend     = FetchZioBackend()
  private val interpreter = SttpClientInterpreter()

  def logout(): Task[Either[ApiError, CookieValueWithMeta]] =
    interpreter.toClientThrowDecodeFailures(Endpoints.logout, None, backend)(None)

  def me(): Task[Either[ApiError, UserInfo]] =
    interpreter.toSecureClientThrowDecodeFailures(Endpoints.me, None, backend)(None)(())

  def listCharacters(): Task[Either[ApiError, List[CharacterSummary]]] =
    interpreter.toSecureClientThrowDecodeFailures(Endpoints.listCharacters, None, backend)(None)(())

  def createCharacter(): Task[Either[ApiError, Character]] =
    interpreter.toSecureClientThrowDecodeFailures(Endpoints.createCharacter, None, backend)(None)(())

  def getCharacter(id: Long): Task[Either[ApiError, Character]] =
    interpreter.toSecureClientThrowDecodeFailures(Endpoints.getCharacter, None, backend)(None)(id)

  def updateCharacter(id: Long, sheet: CharacterSheet): Task[Either[ApiError, Character]] =
    interpreter.toSecureClientThrowDecodeFailures(Endpoints.updateCharacter, None, backend)(None)((id, sheet))

  def deleteCharacter(id: Long): Task[Either[ApiError, Unit]] =
    interpreter.toSecureClientThrowDecodeFailures(Endpoints.deleteCharacter, None, backend)(None)(id)
