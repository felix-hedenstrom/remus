package nu.fxh.remus.server.db

import nu.fxh.remus.server.CharacterService
import nu.fxh.remus.shared.CharacterSheet
import zio.*
import zio.test.*

object CharacterRepoSpec extends ZIOSpecDefault:
  def spec = suite("CharacterRepo")(
    test("create, find, update, list and delete a character") {
      val (fields, children) = CharacterService.decompose(CharacterSheet.blank)
      for
        repo    <- ZIO.service[CharacterRepo]
        users   <- ZIO.service[UserRepo]
        user    <- users.findOrCreateByDiscordId("discord-alice", "alice")
        created <- repo.create(user.id, fields.copy(name = "Barry"), children)
        found   <- repo.find(created.row.id)
        _       <- repo.update(created.row.id, fields.copy(name = "Barry Gronbark"), children)
        updated <- repo.find(created.row.id)
        listed  <- repo.listByOwner(user.id)
        _       <- repo.delete(created.row.id)
        deleted <- repo.find(created.row.id)
      yield assertTrue(
        found.map(_.row).contains(created.row),
        found.exists(_.children.skills.size == children.skills.size),
        updated.exists(_.row.fields.name == "Barry Gronbark"),
        listed.map(_.id) == List(created.row.id),
        deleted.isEmpty
      )
    }
  ).provideShared(TestDb.layer, UserRepo.layer, CharacterRepo.layer)
