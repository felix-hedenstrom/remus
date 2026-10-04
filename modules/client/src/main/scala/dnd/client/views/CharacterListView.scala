package dnd.client
package views

import com.raquo.laminar.api.L.*
import dnd.shared.CharacterSummary

object CharacterListView:

  def apply(): Element =
    val characters = Var(List.empty[CharacterSummary])

    def reload(): Unit =
      AppRuntime.run(Api.listCharacters())(
        onSuccess = {
          case Right(list) => characters.set(list)
          case Left(err)   => AppState.showError(err.message)
        },
        onFailure = t => AppState.showError(t.getMessage)
      )

    def createCharacter(): Unit =
      AppRuntime.run(Api.createCharacter())(
        onSuccess = {
          case Right(character) => AppState.openCharacter(character.id)
          case Left(err)        => AppState.showError(err.message)
        },
        onFailure = t => AppState.showError(t.getMessage)
      )

    def deleteCharacter(id: Long): Unit =
      AppRuntime.run(Api.deleteCharacter(id))(
        onSuccess = {
          case Right(_)  => reload()
          case Left(err) => AppState.showError(err.message)
        },
        onFailure = t => AppState.showError(t.getMessage)
      )

    def logout(): Unit =
      AppRuntime.run(Api.logout())(
        onSuccess = _ => {
          AppState.currentUser.set(None)
          AppState.page.set(Page.Login)
        },
        onFailure = _ => {
          AppState.currentUser.set(None)
          AppState.page.set(Page.Login)
        }
      )

    div(
      cls := "character-list-view",
      onMountCallback(_ => reload()),
      div(
        cls := "toolbar",
        h1("Dina karaktärer"),
        button(tpe := "button", "Logga ut", onClick --> (_ => logout()))
      ),
      button(tpe := "button", "Skapa ny karaktär", onClick --> (_ => createCharacter())),
      div(
        cls := "character-rows",
        children <-- characters.signal.map(_.map { c =>
          div(
            cls := "character-row",
            span(
              cls := "name",
              onClick --> (_ => AppState.openCharacter(c.id)),
              if c.name.trim.isEmpty then "(namnlös)" else c.name
            ),
            span(cls := "meta", s"${Labels.species(c.species)} ${Labels.profession(c.profession)}".trim),
            button(tpe := "button", "Ta bort", onClick --> (_ => deleteCharacter(c.id)))
          )
        })
      )
    )
