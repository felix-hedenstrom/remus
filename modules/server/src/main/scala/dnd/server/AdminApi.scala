package dnd.server

import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.*
import zio.*
import zio.http.*

object AdminApi:

  def routes: Routes[Any, Response] =
    val docs: List[ZServerEndpoint[Any, Any]] =
      SwaggerInterpreter().fromEndpoints[Task](AdminEndpoints.all, "Pellegrino Admin API", "1.0")
    ZioHttpInterpreter().toHttp(docs)
