package dnd.shared

import io.github.iltotore.iron.autoRefine
import sttp.tapir.codec.iron.given

final case class ResourceTrack(max: NonNegativeInt, current: NonNegativeInt) derives Codec, Schema

object ResourceTrack:
  val default: ResourceTrack = ResourceTrack(10, 10)
