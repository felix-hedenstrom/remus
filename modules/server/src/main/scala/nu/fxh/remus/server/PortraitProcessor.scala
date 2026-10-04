package nu.fxh.remus.server

import java.awt.{Graphics2D, RenderingHints}
import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.Base64
import javax.imageio.{IIOImage, ImageIO, ImageWriteParam}
import scala.util.Try

// Downscales and recompresses an uploaded portrait server-side. This must NOT
// be done in the browser via <canvas> (drawImage + toDataURL): Firefox's
// privacy.resistFingerprinting (and similar protections in Brave/Tor) silently
// corrupts canvas pixel readback into rainbow/striped garbage, since the
// readback doesn't happen synchronously inside the user's click gesture.
// Running the equivalent resize on the JVM sidesteps that entirely.
object PortraitProcessor:
  private val MaxDimension   = 512
  private val JpegQuality    = 0.85f
  private val DataUrlPattern = "^data:[^;]+;base64,(.*)$".r

  // TODO: HEIC (iPhone's default camera format) isn't supported by plain
  // ImageIO without extra plugins; such uploads will fail here.
  def process(dataUrl: String): Either[String, String] =
    dataUrl match
      case DataUrlPattern(base64) =>
        Try {
          val bytes    = Base64.getDecoder.decode(base64)
          val original = ImageIO.read(new ByteArrayInputStream(bytes))
          if original == null then throw new IllegalArgumentException("Unsupported or corrupt image data")

          val scale   = math.min(1.0, MaxDimension.toDouble / math.max(original.getWidth, original.getHeight))
          val targetW = math.max(1, math.round(original.getWidth * scale).toInt)
          val targetH = math.max(1, math.round(original.getHeight * scale).toInt)

          val resized       = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB)
          val g: Graphics2D = resized.createGraphics()
          g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
          g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
          g.drawImage(original, 0, 0, targetW, targetH, null)
          g.dispose()

          val out    = new ByteArrayOutputStream()
          val writer = ImageIO.getImageWritersByFormatName("jpg").next()
          val param  = writer.getDefaultWriteParam()
          param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT)
          param.setCompressionQuality(JpegQuality)
          val ios = ImageIO.createImageOutputStream(out)
          writer.setOutput(ios)
          writer.write(null, new IIOImage(resized, null, null), param)
          writer.dispose()
          ios.close()

          s"data:image/jpeg;base64,${Base64.getEncoder.encodeToString(out.toByteArray)}"
        }.toEither.left.map(e => Option(e.getMessage).getOrElse(e.getClass.getSimpleName))
      case _ => Left("Not a data URL")
