package nu.fxh.remus.client
package views

import com.raquo.laminar.api.L.*
import org.scalajs.dom

object LoginView:

  def apply(): Element =
    div(
      cls := "login-view",
      div(
        cls := "login-card panel",
        h1("Drakar och Demoner"),
        button(
          tpe := "button",
          cls := "discord-login primary",
          "Logga in med Discord",
          onClick --> (_ => dom.window.location.href = "/api/auth/discord/login")
        )
      )
    )
