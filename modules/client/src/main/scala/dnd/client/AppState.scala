package dnd.client

import com.raquo.laminar.api.L.*
import dnd.shared.UserInfo
import org.scalajs.dom

enum Page derives CanEqual:
  case Login
  case CharacterList
  case CharacterEditor(id: Long)

object AppState:
  val currentUser: Var[Option[UserInfo]] = Var(None)
  val page: Var[Page]                    = Var(Page.Login)
  val errorMessage: Var[Option[String]]  = Var(None)

  def showError(message: String): Unit = errorMessage.set(Some(message))
  def clearError(): Unit                = errorMessage.set(None)

  private val characterIdPath = "^/characters/(\\d+)$".r

  /** Reads the current browser URL into a Page, falling back to `default`
    * when the path doesn't match a known bookmarkable route.
    */
  def pageFromLocation(default: Page): Page =
    dom.window.location.pathname match
      case characterIdPath(id) => Page.CharacterEditor(id.toLong)
      case _                    => default

  def openCharacter(id: Long): Unit =
    dom.window.history.pushState(null, "", s"/characters/$id")
    page.set(Page.CharacterEditor(id))

  def goToList(): Unit =
    dom.window.history.pushState(null, "", "/")
    page.set(Page.CharacterList)

  dom.window.addEventListener("popstate", (_: dom.PopStateEvent) => page.set(pageFromLocation(Page.CharacterList)))
