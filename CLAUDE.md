# CLAUDE.md

## Stack

- Scala 3.5.2, sbt 1.10.2
- ZIO 2.1.14, ZIO HTTP 3.2.0, ZIO JSON 0.7.3
- Tests: ZIO Test via `sbt test`

Run `sbt test` after every change to guard against regressions. Do not report a task as done until all tests pass.

After every change, also update `CLAUDE.md` and `README.md` so they stay in sync with the code. CLAUDE.md captures project-level conventions and non-obvious gotchas for future Claude sessions; README.md is the user-facing docs (requirements, run/build instructions, API). If a change touches the stack, layout, conventions, build/run flow, or public API, the corresponding doc must move with it in the same task — do not defer.

## Project layout

Code is organised by domain feature, not by syntactic role (no separate `model/`, `repository/`, `routes/` folders). All files that belong to the same feature live in the same package.

```
src/main/scala/cz/dusanrychnovsky/lifemap/
  Main.scala        # entry point, wires layers + starts server (port 8080)
  tasks/
    Task.scala            # Task case class, TaskStatus enum, JSON codecs
    TaskRepository.scala  # trait + in-memory implementation
    TaskRoutes.scala      # HTTP routes, request/response types

src/test/scala/cz/dusanrychnovsky/lifemap/
  tasks/
    TaskRepositorySpec.scala          # unit tests for the repository
    TaskRoutesSpec.scala              # unit tests for routes (in-process, no server)
    TaskRoutesIntegrationSpec.scala   # integration tests (real server, real HTTP client)
```

## Import conventions

`zio.Task` is a type alias (the higher-kinded ZIO effect type) and conflicts with our domain `Task` case class. In Scala 3, named imports take precedence over package members, so `import zio._` will shadow the package-level `Task` even when both files are in the same package. Fix: avoid `import zio._` and import only the specific ZIO names each file actually uses, so `zio.Task` never enters scope.

```scala
// TaskRepository.scala
import zio.{UIO, ULayer, ZLayer, Ref, Random}

// TaskRoutes.scala and tests
import zio.ZIO
import zio.{ZIO, Scope}   // where Scope is also needed
```

`zio.json._` exports a `uuid` identifier that conflicts with `zio.http._`'s path-codec `uuid`. Same fix: avoid the `zio.json._` wildcard.

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

## Docker

Multi-stage `Dockerfile` at the repo root: `sbtscala/scala-sbt` builder runs `sbt package` and stages the application jar plus all runtime-dependency jars (resolved via `sbt 'export Runtime / dependencyClasspath'`) into `/dist/lib`; an `eclipse-temurin:21-jre` runtime stage runs them with `java -cp '/app/lib/*' cz.dusanrychnovsky.lifemap.Main`. No sbt plugins required.

```
docker build -t life-map .
docker run --rm -p 8080:8080 life-map
```

The builder image tag pins a different sbt version than `project/build.properties` (currently 1.12.9 vs 1.10.2). That is intentional — sbt's launcher reads `project/build.properties` and bootstraps the project's pinned version, so the tag's bundled sbt is irrelevant. Don't "fix" the tag to match 1.10.2; published `sbtscala/scala-sbt` tags for older sbt versions get pruned.
