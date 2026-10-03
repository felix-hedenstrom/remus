package dnd.devtools

import dnd.server.AppRuntime
import zio.*

/** Dev-only entrypoint: `sbt devRun`. Boots the full server with
  * `DevAuthService` wired in place of the real Discord-backed auth, so no
  * session cookie or Discord app registration is needed locally.
  */
object DevMain extends ZIOAppDefault:
  override def run = AppRuntime.run(DevAuthService.bypassLayer)
