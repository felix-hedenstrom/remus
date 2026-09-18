package dnd.server.db

import zio.*
import zio.test.*

object UserRepoSpec extends ZIOSpecDefault:
  def spec = suite("UserRepo")(
    test("create and find by username/id") {
      for
        repo    <- ZIO.service[UserRepo]
        created <- repo.create("bob", "hashedpw")
        byName  <- repo.findByUsername("bob")
        byId    <- repo.findById(created.id)
        missing <- repo.findByUsername("nobody")
      yield assertTrue(byName.contains(created), byId.contains(created), missing.isEmpty)
    }
  ).provideShared(TestDb.layer, UserRepo.layer)
