package dnd.server.db

import dnd.shared.{Profession, Species}
import zio.*
import zio.test.*

object CharacterRepoSpec extends ZIOSpecDefault:
  def spec = suite("CharacterRepo")(
    test("create, find, update, list and delete a character") {
      for
        repo    <- ZIO.service[CharacterRepo]
        users   <- ZIO.service[UserRepo]
        user    <- users.findOrCreateByDiscordId("discord-alice", "alice")
        created <- repo.create(user.id, "Barry", Species.Human, Profession.Warrior, """{"a":1}""")
        found   <- repo.find(created.id)
        _       <- repo.update(created.id, "Barry Gronbark", Species.Human, Profession.Warrior, """{"a":2}""")
        updated <- repo.find(created.id)
        listed  <- repo.listByOwner(user.id)
        _       <- repo.delete(created.id)
        deleted <- repo.find(created.id)
      yield assertTrue(
        found.contains(created),
        updated.exists(_.name == "Barry Gronbark"),
        listed.map(_.id) == List(created.id),
        deleted.isEmpty
      )
    }
  ).provideShared(TestDb.layer, UserRepo.layer, CharacterRepo.layer)
