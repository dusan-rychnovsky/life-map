package cz.dusanrychnovsky.lifemap.tasks

import zio.ZIO
import zio.http._
import zio.json.{DeriveJsonDecoder, DecoderOps, EncoderOps, JsonDecoder}
import java.util.UUID

private case class CreateTaskRequest(title: String, description: String)
private object CreateTaskRequest:
  given JsonDecoder[CreateTaskRequest] = DeriveJsonDecoder.gen

private case class UpdateStatusRequest(status: TaskStatus)
private object UpdateStatusRequest:
  given JsonDecoder[UpdateStatusRequest] = DeriveJsonDecoder.gen

object TaskRoutes:

  val routes: Routes[TaskRepository, Nothing] = Routes(

    Method.POST / "tasks" -> handler { (req: Request) =>
      (for
        body      <- req.body.asString.mapError(_ => Response.internalServerError("Failed to read request body"))
        createReq <- ZIO.fromEither(body.fromJson[CreateTaskRequest])
                       .mapError(msg => Response.badRequest(msg))
        task      <- ZIO.serviceWithZIO[TaskRepository](_.create(createReq.title, createReq.description))
      yield Response.json(task.toJson).copy(status = Status.Created))
        .merge
    },

    Method.GET / "tasks" -> handler {
      ZIO.serviceWithZIO[TaskRepository](_.getAll)
        .map(tasks => Response.json(tasks.toJson))
    },

    Method.PATCH / "tasks" / uuid("id") / "status" -> handler { (id: UUID, req: Request) =>
      (for
        body      <- req.body.asString.mapError(_ => Response.internalServerError("Failed to read request body"))
        updateReq <- ZIO.fromEither(body.fromJson[UpdateStatusRequest])
                       .mapError(msg => Response.badRequest(msg))
        maybeTask <- ZIO.serviceWithZIO[TaskRepository](_.updateStatus(id, updateReq.status))
        response  <- ZIO.fromOption(maybeTask)
                       .mapBoth(_ => Response.notFound(s"Task not found: $id"), task => Response.json(task.toJson))
      yield response)
        .merge
    },
  )
