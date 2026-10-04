package nu.fxh.remus.server

import zio.test.*

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.Base64
import javax.imageio.ImageIO

object PortraitProcessorSpec extends ZIOSpecDefault:

  private def pngDataUrl(w: Int, h: Int): String =
    val img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val g   = img.createGraphics()
    g.setColor(Color.BLUE)
    g.fillRect(0, 0, w, h)
    g.dispose()
    val out = new ByteArrayOutputStream()
    ImageIO.write(img, "png", out)
    s"data:image/png;base64,${Base64.getEncoder.encodeToString(out.toByteArray)}"

  def spec = suite("PortraitProcessor")(
    test("downscales a large image to within the max dimension and re-encodes as JPEG") {
      val result = PortraitProcessor.process(pngDataUrl(2000, 1000))
      result match
        case Left(err) => assertTrue(false, err.nonEmpty) // fail with message visible
        case Right(dataUrl) =>
          val bytes = Base64.getDecoder.decode(dataUrl.split(",", 2)(1))
          val img   = ImageIO.read(new ByteArrayInputStream(bytes))
          assertTrue(
            dataUrl.startsWith("data:image/jpeg;base64,"),
            img.getWidth == 512,
            img.getHeight == 256
          )
    },
    test("leaves an already-small image's dimensions unchanged") {
      val result = PortraitProcessor.process(pngDataUrl(100, 50))
      result match
        case Left(err) => assertTrue(false, err.nonEmpty)
        case Right(dataUrl) =>
          val bytes = Base64.getDecoder.decode(dataUrl.split(",", 2)(1))
          val img   = ImageIO.read(new ByteArrayInputStream(bytes))
          assertTrue(img.getWidth == 100, img.getHeight == 50)
    },
    test("fails on a non-data-URL string") {
      assertTrue(PortraitProcessor.process("not-a-data-url").isLeft)
    },
    test("fails on corrupt base64 image data") {
      assertTrue(PortraitProcessor.process("data:image/png;base64,not-valid-base64!!").isLeft)
    }
  )
