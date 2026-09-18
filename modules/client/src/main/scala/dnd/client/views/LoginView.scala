package dnd.client
package views

import com.raquo.laminar.api.L.*
import dnd.shared.{ApiError, LoginRequest, RegisterRequest}

object LoginView:

  def apply(): Element =
    val isRegisterMode = Var(false)
    val username        = Var("")
    val password        = Var("")
    val submitting       = Var(false)

    def networkError(t: Throwable): ApiError = ApiError.ValidationError(Option(t.getMessage).getOrElse(t.toString))

    def onLoggedIn(user: dnd.shared.UserInfo): Unit =
      submitting.set(false)
      AppState.currentUser.set(Some(user))
      AppState.page.set(Page.CharacterList)

    def onError(err: ApiError): Unit =
      submitting.set(false)
      AppState.showError(err.message)

    def doLogin(): Unit =
      AppRuntime.run(Api.login(LoginRequest(username.now(), password.now())))(
        onSuccess = {
          case Right((user, _)) => onLoggedIn(user)
          case Left(err)        => onError(err)
        },
        onFailure = t => onError(networkError(t))
      )

    def submit(): Unit =
      AppState.clearError()
      submitting.set(true)
      if isRegisterMode.now() then
        AppRuntime.run(Api.register(RegisterRequest(username.now(), password.now())))(
          onSuccess = {
            case Right(_)  => doLogin()
            case Left(err) => onError(err)
          },
          onFailure = t => onError(networkError(t))
        )
      else doLogin()

    div(
      cls := "login-view",
      h1("Drakar och Demoner"),
      div(
        cls := "mode-toggle",
        button(
          tpe := "button",
          cls("active") <-- isRegisterMode.signal.map(!_),
          "Logga in",
          onClick --> (_ => isRegisterMode.set(false))
        ),
        button(
          tpe := "button",
          cls("active") <-- isRegisterMode.signal,
          "Skapa konto",
          onClick --> (_ => isRegisterMode.set(true))
        )
      ),
      div(
        cls := "field",
        label("Användarnamn"),
        input(
          typ := "text",
          value <-- username.signal,
          onInput.mapToValue --> username.writer
        )
      ),
      div(
        cls := "field",
        label("Lösenord"),
        input(
          typ := "password",
          value <-- password.signal,
          onInput.mapToValue --> password.writer
        )
      ),
      button(
        tpe := "button",
        disabled <-- submitting.signal,
        child.text <-- isRegisterMode.signal.map(if _ then "Skapa konto" else "Logga in"),
        onClick --> (_ => submit())
      )
    )
