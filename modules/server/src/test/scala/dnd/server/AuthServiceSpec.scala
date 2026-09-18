package dnd.server

import dnd.server.db.{SessionRepo, TestDb, UserRepo}
import dnd.shared.ApiError
import zio.*
import zio.test.*

object AuthServiceSpec extends ZIOSpecDefault:
  def spec = suite("AuthService")(
    test("register then login succeeds with the correct password") {
      for
        auth   <- ZIO.service[AuthService]
        _      <- auth.register("carol", "supersecret")
        result <- auth.login("carol", "supersecret")
      yield assertTrue(result._1.username == "carol")
    },
    test("login fails with the wrong password") {
      for
        auth   <- ZIO.service[AuthService]
        _      <- auth.register("dave", "correctpw1")
        result <- auth.login("dave", "wrongpw12").either
      yield assertTrue(result match
        case Left(_: ApiError.Unauthorized) => true
        case _                              => false
      )
    },
    test("registering a duplicate username fails with Conflict") {
      for
        auth   <- ZIO.service[AuthService]
        _      <- auth.register("erin", "password1")
        result <- auth.register("erin", "password2").either
      yield assertTrue(result match
        case Left(_: ApiError.Conflict) => true
        case _                          => false
      )
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
