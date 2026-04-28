package cz.dusanrychnovsky.lifemap.tasks

import zio.{Scope, ZIO}
import zio.http._
import zio.json.DecoderOps
import zio.test._
import java.util.UUID

object TaskRoutesSpec extends ZIOSpecDefault:

  private val routes = TaskRoutes.routes

  def spec = suite("TaskRoutes")(

    suite("POST /tasks")(
      test("returns 201 Created with the new task") {
        val req = Request(
          method = Method.POST,
          url = url"http://localhost/tasks",
          body = Body.fromString("""{"title":"Buy groceries","description":"Milk and eggs"}"""),
        )
        for
          _        <- PostgresTestSupport.truncate
          response <- routes(req)
          body     <- response.body.asString
          task     <- ZIO.fromEither(body.fromJson[Task])
        yield assertTrue(response.status == Status.Created) &&
              assertTrue(task.title == "Buy groceries") &&
              assertTrue(task.description == "Milk and eggs") &&
              assertTrue(task.status == TaskStatus.New)
      },

      test("returns 400 Bad Request for invalid JSON") {
        val req = Request(
          method = Method.POST,
          url = url"http://localhost/tasks",
          body = Body.fromString("not-json"),
        )
        for
          _        <- PostgresTestSupport.truncate
          response <- routes(req)
        yield assertTrue(response.status == Status.BadRequest)
      },
    ),

    suite("GET /tasks")(
      test("returns 200 OK with a JSON array") {
        val req = Request.get(url"http://localhost/tasks")
        for
          _        <- PostgresTestSupport.truncate
          response <- routes(req)
          body     <- response.body.asString
          tasks    <- ZIO.fromEither(body.fromJson[List[Task]])
        yield assertTrue(response.status == Status.Ok) &&
              assertTrue(tasks.isEmpty)
      },

      test("includes tasks that were previously created") {
        val postReq = Request(
          method = Method.POST,
          url = url"http://localhost/tasks",
          body = Body.fromString("""{"title":"Walk the dog","description":"30 minutes"}"""),
        )
        val getReq = Request.get(url"http://localhost/tasks")
        for
          _        <- PostgresTestSupport.truncate
          _        <- routes(postReq)
          response <- routes(getReq)
          body     <- response.body.asString
          tasks    <- ZIO.fromEither(body.fromJson[List[Task]])
        yield assertTrue(tasks.exists(_.title == "Walk the dog"))
      },
    ),

    suite("PATCH /tasks/:id/status")(
      test("returns 200 OK with the updated task") {
        val postReq = Request(
          method = Method.POST,
          url = url"http://localhost/tasks",
          body = Body.fromString("""{"title":"Read a book","description":"Fiction"}"""),
        )
        for
          _          <- PostgresTestSupport.truncate
          createResp <- routes(postReq)
          createBody <- createResp.body.asString
          task       <- ZIO.fromEither(createBody.fromJson[Task])
          patchReq    = Request(
                          method = Method.PATCH,
                          url = url"http://localhost/tasks/${task.id}/status",
                          body = Body.fromString("""{"status":"active"}"""),
                        )
          patchResp  <- routes(patchReq)
          patchBody  <- patchResp.body.asString
          updated    <- ZIO.fromEither(patchBody.fromJson[Task])
        yield assertTrue(patchResp.status == Status.Ok) &&
              assertTrue(updated.id == task.id) &&
              assertTrue(updated.status == TaskStatus.Active)
      },

      test("returns 404 Not Found for an unknown task id") {
        val id  = UUID.randomUUID()
        val req = Request(
          method = Method.PATCH,
          url = url"http://localhost/tasks/$id/status",
          body = Body.fromString("""{"status":"active"}"""),
        )
        for
          _        <- PostgresTestSupport.truncate
          response <- routes(req)
        yield assertTrue(response.status == Status.NotFound)
      },

      test("returns 400 Bad Request for an invalid status value") {
        val postReq = Request(
          method = Method.POST,
          url = url"http://localhost/tasks",
          body = Body.fromString("""{"title":"Task","description":"Desc"}"""),
        )
        for
          _          <- PostgresTestSupport.truncate
          createResp <- routes(postReq)
          createBody <- createResp.body.asString
          task       <- ZIO.fromEither(createBody.fromJson[Task])
          patchReq    = Request(
                          method = Method.PATCH,
                          url = url"http://localhost/tasks/${task.id}/status",
                          body = Body.fromString("""{"status":"invalid"}"""),
                        )
          patchResp  <- routes(patchReq)
        yield assertTrue(patchResp.status == Status.BadRequest)
      },
    ),
  ).provideShared(PostgresTestSupport.layer, Scope.default) @@ TestAspect.sequential
