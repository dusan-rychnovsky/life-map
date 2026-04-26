# CLAUDE.md

## Stack

- Scala 3.5.2, sbt 1.10.2
- ZIO 2.1.14, ZIO HTTP 3.2.0, ZIO JSON 0.7.3
- Tests: ZIO Test via `sbt test`

## Project layout

```
src/main/scala/cz/dusanrychnovsky/lifemap/
  Main.scala                  # entry point, wires layers + starts server (port 8080)
  model/Task.scala            # Task case class, TaskStatus enum, JSON codecs
  repository/TaskRepository.scala   # trait + in-memory implementation
  routes/TaskRoutes.scala     # HTTP routes, request/response types

src/test/scala/cz/dusanrychnovsky/lifemap/
  repository/TaskRepositorySpec.scala          # unit tests for the repository
  routes/TaskRoutesSpec.scala                  # unit tests for routes (in-process, no server)
  routes/TaskRoutesIntegrationSpec.scala       # integration tests (real server, real HTTP client)
```

## Import conventions

`zio.Task` conflicts with `cz.dusanrychnovsky.lifemap.model.Task`. Two wildcard imports from both packages always produce an ambiguity error in Scala 3 regardless of order. Fix: import the model type explicitly so it shadows the wildcard.

```scala
import zio._
import cz.dusanrychnovsky.lifemap.model.{Task, TaskStatus}  // explicit wins over zio.Task wildcard
```

`zio.json._` exports a `uuid` identifier that conflicts with `zio.http._`'s path-codec `uuid`. Fix: avoid the `zio.json._` wildcard and import only what is needed.

```scala
import zio.json.{DeriveJsonDecoder, DecoderOps, EncoderOps, JsonDecoder}
```

## Testing patterns

**Unit tests for routes** call `routes(request)` directly without a server. Because route handlers read the request body via `req.body.asString`, `Scope` must be provided even in unit tests:

```scala
test("...") { ... }.provide(TaskRepository.layer, Scope.default)
```

**Integration tests** spin up a real server on a random OS-assigned port using `Server.defaultWithPort(0)` and `Server.install(routes)`, then hit it with a real `Client`:

```scala
.provide(Server.defaultWithPort(0), TaskRepository.layer, Client.default, Scope.default)
```

**Repository isolation**: each test gets its own `TaskRepository` instance by calling `.provide(TaskRepository.layer)` per test (not per suite). If the layer is provided at the suite level it is shared and tests that depend on an empty store will fail.

## Error handling in routes

Route handlers use `ZIO[R, Response, Response]` internally — both success and error paths produce a `Response` — then collapse to `ZIO[R, Nothing, Response]` with `.merge`. This keeps `Routes[R, Nothing]` as the public type.
