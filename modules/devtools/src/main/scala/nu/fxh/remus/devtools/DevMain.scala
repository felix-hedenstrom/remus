package nu.fxh.remus.devtools

import nu.fxh.remus.server.AppRuntime
import zio.*

object DevMain extends ZIOAppDefault:
  override def run = AppRuntime.run(DevAuthService.bypassLayer)
