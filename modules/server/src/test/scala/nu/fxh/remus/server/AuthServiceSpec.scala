package nu.fxh.remus.server

import nu.fxh.remus.server.db.{SessionRepo, TestDb, UserRepo}
import nu.fxh.remus.shared.ApiError
import zio.*
import zio.test.*

object AuthServiceSpec extends ZIOSpecDefault:
  def spec = suite("AuthService")(
    test("completeDiscordLogin creates a new user on first login") {
      for
        auth   <- ZIO.service[AuthService]
        result <- auth.completeDiscordLogin("discord-1", "carol")
      yield assertTrue(result._1.username == "carol")
    },
    test("completeDiscordLogin with the same discordId reuses the account and updates the username") {
      for
        auth   <- ZIO.service[AuthService]
        first  <- auth.completeDiscordLogin("discord-2", "dave")
        second <- auth.completeDiscordLogin("discord-2", "dave-renamed")
      yield assertTrue(second._1.id == first._1.id, second._1.username == "dave-renamed")
    },
    test("resolveSession returns Unauthorized for a missing cookie") {
      for
        auth   <- ZIO.service[AuthService]
        result <- auth.resolveSession(None).either
      yield assertTrue(result match
        case Left(_: ApiError.Unauthorized) => true
        case _                              => false
      )
    }
  ).provideShared(TestDb.layer, UserRepo.layer, SessionRepo.layer, AuthService.layer)
