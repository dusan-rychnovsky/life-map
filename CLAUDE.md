# CLAUDE.md

## Stack

- Scala 3.5.2, sbt 1.10.2
- ZIO 2.1.14, ZIO HTTP 3.2.0, ZIO JSON 0.7.3
- Quill 4.8.6 (`quill-jdbc-zio`), PostgreSQL JDBC 42.7.4, HikariCP 5.1.0
- Flyway 10.21.0 (`flyway-core` + `flyway-database-postgresql`)
- Tests: ZIO Test via `sbt test`, Testcontainers Scala 0.41.8 (Postgres)
- SLF4J binding: `slf4j-simple` (Hikari and Flyway log via SLF4J)

Run `sbt test` after every change to guard against regressions. Tests start a
real Postgres in Docker via Testcontainers, so Docker must be running. Do not
report a task as done until all tests pass.

After every change, also update `CLAUDE.md` and `README.md` so they stay in sync with the code. CLAUDE.md captures project-level conventions and non-obvious gotchas for future Claude sessions; README.md is the user-facing docs (requirements, run/build instructions, API). If a change touches the stack, layout, conventions, build/run flow, or public API, the corresponding doc must move with it in the same task — do not defer.

## Project layout

Code is organised by domain feature, not by syntactic role (no separate `model/`, `repository/`, `routes/` folders). All files that belong to the same feature live in the same package. Cross-cutting infrastructure (database wiring) lives in its own package.

```
src/main/scala/cz/dusanrychnovsky/lifemap/
  Main.scala        # entry point, wires layers + starts server (port 8080)
  db/
    Database.scala  # Hikari DataSource + Flyway migrate + Quill Postgres layers
    DotEnv.scala    # reads .env at the working directory; OS env wins on conflict
  tasks/
    Task.scala            # Task case class, TaskStatus enum, JSON codecs
    TaskRepository.scala  # trait + Quill/Postgres implementation
    TaskRoutes.scala      # HTTP routes, request/response types

src/main/resources/db/migration/
  V1__create_tasks.sql    # Flyway migration creating the tasks table

src/test/scala/cz/dusanrychnovsky/lifemap/
  tasks/
    PostgresTestSupport.scala         # shared Testcontainers Postgres + truncate helper
    TaskRepositorySpec.scala          # repository tests (real Postgres)
    TaskRoutesSpec.scala              # in-process route tests (real Postgres, no server)
    TaskRoutesIntegrationSpec.scala   # integration tests (real server, real Postgres)
```

## Configuration

Three env vars are required at runtime: `DB_URL`, `DB_USER`, `DB_PASSWORD`. They are read in this order:

1. The OS environment (`System.getenv`).
2. The `.env` file at the working directory (gitignored — `.env.example` is committed).

OS env wins, so production deployments that inject env vars don't need a `.env` file. `db/DotEnv.scala` does the loading; it is intentionally self-contained — no external dotenv dependency.

## Import conventions

`zio.Task` is a type alias (the higher-kinded ZIO effect type) and conflicts with our domain `Task` case class. In Scala 3, named imports take precedence over package members, so `import zio._` will shadow the package-level `Task` even when both files are in the same package. Fix: avoid `import zio._` and import only the specific ZIO names each file actually uses, so `zio.Task` never enters scope.

```scala
// TaskRepository.scala
import zio.{Random, ZIO, ZLayer}

// TaskRoutes.scala and tests
import zio.ZIO
import zio.{ZIO, Scope}   // where Scope is also needed
```

`zio.json._` exports a `uuid` identifier that conflicts with `zio.http._`'s path-codec `uuid`. Same fix: avoid the `zio.json._` wildcard.

```scala
import zio.json.{DeriveJsonDecoder, DecoderOps, EncoderOps, JsonDecoder}
```

`io.getquill._` IS imported wildcard inside `TaskRepository.scala`. ProtoQuill (Quill 4 on Scala 3) requires `Quoted`, `query`, `quote`, `lift`, `unquote`, the `MappedEncoding` factory, and several given parser-factory instances all in scope — selective imports trigger "Could not summon a parser factory" errors. The wildcard is safe here because the file does not also import `zio._`.

## Repository conventions

`TaskRepository` methods return `ZIO[Any, Throwable, A]` (the DataSource is captured by the `Quill.Postgres[SnakeCase]` instance, and SQL exceptions widen to `Throwable`). Routes therefore must `.mapError(repoFailure)` repository calls — `repoFailure` produces a 500 response.

`TaskStatus <-> String` translation lives in `TaskRepository` as `MappedEncoding` givens. The DB column is `TEXT` with a `CHECK` constraint, not a Postgres enum, so adding a new status only requires touching the migration and the encoding givens.

## Testing patterns

**Shared container, per-test truncation**: each spec uses `provideShared(PostgresTestSupport.layer)` so one Postgres container starts per spec and Flyway migrates it once. Each test body opens with `_ <- PostgresTestSupport.truncate` to clear the `tasks` table — DO NOT skip this, the layer is shared and tests would otherwise leak state.

**Sequential aspect required**: ZIO Test runs tests within a suite in parallel by default; with a shared mutable database that breaks isolation. Every spec ends with `@@ TestAspect.sequential`.

**Unit tests for routes** call `routes(request)` directly without a server. Because route handlers read the request body via `req.body.asString`, `Scope` must still be provided alongside `PostgresTestSupport.layer`.

**Integration tests** spin up a real server on a random OS-assigned port using `Server.defaultWithPort(0)` and `Server.install(routes)`, then hit it with a real `Client`.

## Docker for tests

Testcontainers needs to talk to the Docker daemon. On Docker Desktop / WSL, `build.sbt` sets two system properties for forked test JVMs to keep things deterministic:

- `-Ddocker.host=unix:///var/run/docker.sock` (overridable via `DOCKER_HOST` env var)
- `-Dapi.version=1.43` (overridable via `DOCKER_API_VERSION` env var)

The pinned `api.version=1.43` exists because Docker Desktop 29's daemon (Docker API 1.54) returns HTTP 400 to docker-java's default version probe when the embedded client requests a newer API path than the daemon advertises. 1.43 is the floor that all Docker engines >= 24 support and is what testcontainers-java 1.20.x's bundled docker-java would otherwise auto-negotiate to.

`Test / fork := true` and `Test / parallelExecution := false` are also set. Forking is required so Testcontainers can clean up subprocesses; sequential execution prevents the three suites from each starting a Postgres container at the same time.

## Error handling in routes

Route handlers use `ZIO[R, Response, Response]` internally — both success and error paths produce a `Response` — then collapse to `ZIO[R, Nothing, Response]` with `.merge`. This keeps `Routes[R, Nothing]` as the public type. Repository `Throwable` errors are mapped to a 500 `Response` via `repoFailure` before they reach `.merge`.

## Docker

Multi-stage `Dockerfile` at the repo root: `sbtscala/scala-sbt` builder runs `sbt package` and stages the application jar plus all runtime-dependency jars (resolved via `sbt 'export Runtime / dependencyClasspath'`) into `/dist/lib`; an `eclipse-temurin:21-jre` runtime stage runs them with `java -cp '/app/lib/*' cz.dusanrychnovsky.lifemap.Main`. No sbt plugins required.

```
docker build -t life-map .
docker run --rm -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/lifemap \
  -e DB_USER=lifemap -e DB_PASSWORD=changeme \
  life-map
```

The image does NOT bundle a `.env`; production must pass `DB_URL`, `DB_USER`, `DB_PASSWORD` as real env vars (which is the expected pattern for containerised deployments).

The builder image tag pins a different sbt version than `project/build.properties` (currently 1.12.9 vs 1.10.2). That is intentional — sbt's launcher reads `project/build.properties` and bootstraps the project's pinned version, so the tag's bundled sbt is irrelevant. Don't "fix" the tag to match 1.10.2; published `sbtscala/scala-sbt` tags for older sbt versions get pruned.
