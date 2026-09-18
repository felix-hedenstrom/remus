package dnd.client

import com.raquo.laminar.api.L.*
import dnd.shared.UserInfo

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
