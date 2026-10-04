package nu.fxh.remus.server

import nu.fxh.remus.server.db.{CharacterRepo, TestDb, UserRepo}
import nu.fxh.remus.shared.ApiError
import io.github.iltotore.iron.autoRefine
import zio.*
import zio.test.*

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

object CharacterServiceSpec extends ZIOSpecDefault:

  private def samplePngDataUrl(w: Int = 800, h: Int = 600): String =
    val img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val g   = img.createGraphics()
    g.setColor(java.awt.Color.RED)
    g.fillRect(0, 0, w, h)
    g.dispose()
    val out = new ByteArrayOutputStream()
    ImageIO.write(img, "png", out)
    s"data:image/png;base64,${Base64.getEncoder.encodeToString(out.toByteArray)}"

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
    },
    test("uploading a portrait stores a resized JPEG data URL") {
      for
        users   <- ZIO.service[UserRepo]
        svc     <- ZIO.service[CharacterService]
        user    <- users.findOrCreateByDiscordId("discord-owner4", "owner4")
        created <- svc.create(user.id)
        updated <- svc.update(user.id, created.id, created.sheet.copy(header = created.sheet.header.copy(portrait = Some(samplePngDataUrl()))))
        portrait = updated.sheet.header.portrait.get
        img      = ImageIO.read(new java.io.ByteArrayInputStream(Base64.getDecoder.decode(portrait.split(",", 2)(1))))
      yield assertTrue(
        portrait.startsWith("data:image/jpeg;base64,"),
        math.max(img.getWidth, img.getHeight) <= 512
      )
    },
    test("saving an unchanged portrait does not reprocess it") {
      for
        users    <- ZIO.service[UserRepo]
        svc      <- ZIO.service[CharacterService]
        user     <- users.findOrCreateByDiscordId("discord-owner5", "owner5")
        created  <- svc.create(user.id)
        first    <- svc.update(user.id, created.id, created.sheet.copy(header = created.sheet.header.copy(portrait = Some(samplePngDataUrl()))))
        second   <- svc.update(user.id, created.id, first.sheet.copy(header = first.sheet.header.copy(name = "Barry")))
      yield assertTrue(second.sheet.header.portrait == first.sheet.header.portrait)
    },
    test("an invalid portrait data URL fails the update with ValidationError") {
      for
        users   <- ZIO.service[UserRepo]
        svc     <- ZIO.service[CharacterService]
        user    <- users.findOrCreateByDiscordId("discord-owner6", "owner6")
        created <- svc.create(user.id)
        result  <- svc.update(user.id, created.id, created.sheet.copy(header = created.sheet.header.copy(portrait = Some("not-a-data-url")))).either
      yield assertTrue(result match
        case Left(_: ApiError.ValidationError) => true
        case _                                 => false
      )
    }
  ).provideShared(TestDb.layer, UserRepo.layer, CharacterRepo.layer, CharacterService.layer)
