package cz.dusanrychnovsky.lifemap

import cz.dusanrychnovsky.lifemap.repository.TaskRepository
import cz.dusanrychnovsky.lifemap.routes.TaskRoutes
import zio._
import zio.http._

object Main extends ZIOAppDefault:

  override def run =
    Server.serve(TaskRoutes.routes)
      .provide(
        Server.default,
        TaskRepository.layer,
      )
