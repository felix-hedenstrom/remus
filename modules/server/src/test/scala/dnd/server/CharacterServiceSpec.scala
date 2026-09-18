package dnd.server

import dnd.server.db.{CharacterRepo, TestDb, UserRepo}
import dnd.shared.ApiError
import zio.*
import zio.test.*

object CharacterServiceSpec extends ZIOSpecDefault:
  def spec = suite("CharacterService")(
    test("a user cannot access another user's character") {
      for
        users   <- ZIO.service[UserRepo]
        svc     <- ZIO.service[CharacterService]
        owner   <- users.create("owner1", "h")
        other   <- users.create("other1", "h")
        created <- svc.create(owner.id)
        result  <- svc.get(other.id, created.id.value).either
      yield assertTrue(result match
        case Left(_: ApiError.Forbidden) => true
        case _                           => false
      )
    },
    test("getting a nonexistent character fails with NotFound") {
      for
        users  <- ZIO.service[UserRepo]
        svc    <- ZIO.service[CharacterService]
        user   <- users.create("owner2", "h")
        result <- svc.get(user.id, 999999L).either
      yield assertTrue(result match
        case Left(_: ApiError.NotFound) => true
        case _                          => false
      )
    },
    test("the owner can create, update and delete their own character") {
      for
        users     <- ZIO.service[UserRepo]
        svc       <- ZIO.service[CharacterService]
        user      <- users.create("owner3", "h")
        created   <- svc.create(user.id)
        updated   <- svc.update(user.id, created.id.value, created.sheet.copy(header = created.sheet.header.copy(name = "Barry")))
        listed    <- svc.list(user.id)
        _         <- svc.delete(user.id, created.id.value)
        afterDelete <- svc.get(user.id, created.id.value).either
      yield assertTrue(
        updated.sheet.header.name == "Barry",
        listed.exists(_.id == created.id),
        afterDelete.isLeft
      )
    }
  ).provideShared(TestDb.layer, UserRepo.layer, CharacterRepo.layer, CharacterService.layer)
