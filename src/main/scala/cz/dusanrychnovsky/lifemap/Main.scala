package cz.dusanrychnovsky.lifemap

import cz.dusanrychnovsky.lifemap.db.Database
import cz.dusanrychnovsky.lifemap.tasks.{TaskRepository, TaskRoutes}
import zio.ZIO
import zio.http._

object Main extends zio.ZIOAppDefault:

  override def run =
    Server.serve(TaskRoutes.routes)
      .provide(
        Server.default,
        Database.live,
        TaskRepository.layer,
      )
