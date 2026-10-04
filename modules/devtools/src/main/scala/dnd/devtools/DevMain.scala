package dnd.devtools

import dnd.server.AppRuntime
import zio.*

object DevMain extends ZIOAppDefault:
  override def run = AppRuntime.run(DevAuthService.bypassLayer)
