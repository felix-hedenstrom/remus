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
        owner   <- users.findOrCreateByDiscordId("discord-owner1", "owner1")
        other   <- users.findOrCreateByDiscordId("discord-other1", "other1")
        created <- svc.create(owner.id)
        result  <- svc.get(other.id, created.id).either
      yield assertTrue(result match
        case Left(_: ApiError.Forbidden) => true
        case _                           => false
      )
    },
    test("getting a nonexistent character fails with NotFound") {
      for
        users  <- ZIO.service[UserRepo]
        svc    <- ZIO.service[CharacterService]
        user   <- users.findOrCreateByDiscordId("discord-owner2", "owner2")
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
        user      <- users.findOrCreateByDiscordId("discord-owner3", "owner3")
        created   <- svc.create(user.id)
        updated   <- svc.update(user.id, created.id, created.sheet.copy(header = created.sheet.header.copy(name = "Barry")))
        listed    <- svc.list(user.id)
        _         <- svc.delete(user.id, created.id)
        afterDelete <- svc.get(user.id, created.id).either
      yield assertTrue(
        updated.sheet.header.name == "Barry",
        listed.exists(_.id == created.id),
        afterDelete.isLeft
      )
    }
  ).provideShared(TestDb.layer, UserRepo.layer, CharacterRepo.layer, CharacterService.layer)
