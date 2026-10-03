package dnd.client

import com.raquo.laminar.api.L.*
import dnd.client.views.*
import org.scalajs.dom

object Main:

  def main(args: Array[String]): Unit =
    if dom.window.location.search.contains("login_error") then
      AppState.showError("Discord login failed or was cancelled")
    AppRuntime.run(Api.me())(
      onSuccess = {
        case Right(user) =>
          AppState.currentUser.set(Some(user))
          AppState.page.set(Page.CharacterList)
        case Left(_) => ()
      },
      onFailure = _ => ()
    )
    val containerNode = dom.document.getElementById("app")
    render(containerNode, appView())

  private def appView(): Element =
    div(
      cls := "app",
      child.maybe <-- AppState.errorMessage.signal.map(_.map { msg =>
        div(
          cls := "error-banner",
          span(msg),
          button(tpe := "button", "×", onClick --> (_ => AppState.clearError()))
        )
      }),
      child <-- AppState.page.signal.map {
        case Page.Login                  => LoginView()
        case Page.CharacterList          => CharacterListView()
        case Page.CharacterEditor(id)    => CharacterEditorView(id)
      }
    )
