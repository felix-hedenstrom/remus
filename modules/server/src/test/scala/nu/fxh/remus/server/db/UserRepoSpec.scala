package nu.fxh.remus.server.db

import zio.*
import zio.test.*

object UserRepoSpec extends ZIOSpecDefault:
  def spec = suite("UserRepo")(
    test("findOrCreateByDiscordId creates on first call and finds by id") {
      for
        repo    <- ZIO.service[UserRepo]
        created <- repo.findOrCreateByDiscordId("discord-bob", "bob")
        byId    <- repo.findById(created.id)
        missing <- repo.findById(created.id + 1_000_000L)
      yield assertTrue(byId.contains(created), missing.isEmpty)
    },
    test("findOrCreateByDiscordId with the same discordId returns the same row and updates the username") {
      for
        repo   <- ZIO.service[UserRepo]
        first  <- repo.findOrCreateByDiscordId("discord-erin", "erin")
        second <- repo.findOrCreateByDiscordId("discord-erin", "erin-new")
      yield assertTrue(second.id == first.id, second.username == "erin-new")
    }
  ).provideShared(TestDb.layer, UserRepo.layer)
