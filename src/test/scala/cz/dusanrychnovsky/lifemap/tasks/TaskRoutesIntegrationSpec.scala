package cz.dusanrychnovsky.lifemap.tasks

import zio.{ZIO, Scope}
import zio.http._
import zio.json.DecoderOps
import zio.test._
import java.util.UUID

object TaskRoutesIntegrationSpec extends ZIOSpecDefault:

  def spec = suite("TaskRoutes - Integration")(

    test("POST /tasks returns 201 Created with the new task") {
      for
        port     <- Server.install(TaskRoutes.routes)
        client   <- ZIO.service[Client]
        response <- client(Request(
                      method = Method.POST,
                      url = url"http://localhost:$port/tasks",
                      body = Body.fromString("""{"title":"Buy groceries","description":"Milk and eggs"}"""),
                    ))
        body     <- response.body.asString
        task     <- ZIO.fromEither(body.fromJson[Task]).mapError(new RuntimeException(_))
      yield assertTrue(response.status == Status.Created) &&
            assertTrue(task.title == "Buy groceries") &&
            assertTrue(task.status == TaskStatus.New)
    },

    test("GET /tasks returns 200 OK with JSON array") {
      for
        port     <- Server.install(TaskRoutes.routes)
        client   <- ZIO.service[Client]
        _        <- client(Request(
                      method = Method.POST,
                      url = url"http://localhost:$port/tasks",
                      body = Body.fromString("""{"title":"Walk the dog","description":"30 minutes"}"""),
                    ))
        response <- client(Request.get(url"http://localhost:$port/tasks"))
        body     <- response.body.asString
        tasks    <- ZIO.fromEither(body.fromJson[List[Task]]).mapError(new RuntimeException(_))
      yield assertTrue(response.status == Status.Ok) &&
            assertTrue(tasks.exists(_.title == "Walk the dog"))
    },

    test("PATCH /tasks/:id/status returns 200 OK with updated task") {
      for
        port        <- Server.install(TaskRoutes.routes)
        client      <- ZIO.service[Client]
        createResp  <- client(Request(
                         method = Method.POST,
                         url = url"http://localhost:$port/tasks",
                         body = Body.fromString("""{"title":"Read a book","description":"Fiction"}"""),
                       ))
        createBody  <- createResp.body.asString
        task        <- ZIO.fromEither(createBody.fromJson[Task]).mapError(new RuntimeException(_))
        patchResp   <- client(Request(
                         method = Method.PATCH,
                         url = url"http://localhost:$port/tasks/${task.id}/status",
                         body = Body.fromString("""{"status":"active"}"""),
                       ))
        patchBody   <- patchResp.body.asString
        updated     <- ZIO.fromEither(patchBody.fromJson[Task]).mapError(new RuntimeException(_))
      yield assertTrue(patchResp.status == Status.Ok) &&
            assertTrue(updated.id == task.id) &&
            assertTrue(updated.status == TaskStatus.Active)
    },

    test("PATCH /tasks/:id/status returns 404 for unknown id") {
      val id = UUID.randomUUID()
      for
        port     <- Server.install(TaskRoutes.routes)
        client   <- ZIO.service[Client]
        response <- client(Request(
                      method = Method.PATCH,
                      url = url"http://localhost:$port/tasks/$id/status",
                      body = Body.fromString("""{"status":"active"}"""),
                    ))
      yield assertTrue(response.status == Status.NotFound)
    },

  ).provide(
    Server.defaultWithPort(0),
    TaskRepository.layer,
    Client.default,
    Scope.default,
  )
