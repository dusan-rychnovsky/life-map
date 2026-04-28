# life-map

A personal task management REST API built with Scala 3 and ZIO, backed by PostgreSQL.

## Requirements

- JDK 17+
- [sbt](https://www.scala-sbt.org/) 1.10+
- A running PostgreSQL instance (for `sbt run`)
- Docker (for `sbt test` — tests spin up Postgres in [Testcontainers](https://java.testcontainers.org/))

## Configuration

Database credentials are read from environment variables:

| Variable      | Example                                     |
|---------------|---------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/lifemap`  |
| `DB_USER`     | `lifemap`                                   |
| `DB_PASSWORD` | `changeme`                                  |

For local development, copy `.env.example` to `.env` and edit it. The application
auto-loads `.env` from the working directory at startup; real OS environment
variables (e.g. those set by Docker, CI, or your shell) take precedence over
file entries. The `.env` file is gitignored — do not commit credentials.

```bash
cp .env.example .env
# edit .env to point at your local Postgres
```

Schema migrations live under `src/main/resources/db/migration/` and are applied
by [Flyway](https://flywaydb.org/) on application startup.

## Running the server

```bash
# create the database first, e.g. via psql or:
docker run --name lifemap-pg -e POSTGRES_USER=lifemap -e POSTGRES_PASSWORD=changeme \
  -e POSTGRES_DB=lifemap -p 5432:5432 -d postgres:16-alpine

sbt run
```

The server starts on `http://localhost:8080` by default.

## Running with Docker

The app's Docker image expects DB credentials at runtime:

```bash
docker build -t life-map .
docker run --rm -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/lifemap \
  -e DB_USER=lifemap \
  -e DB_PASSWORD=changeme \
  life-map
```

## API

### Create a task

```
POST /tasks
Content-Type: application/json

{"title": "Buy groceries", "description": "Milk and eggs"}
```

Response: `201 Created` with the created task as JSON. New tasks always start with status `new`.

### Get all tasks

```
GET /tasks
```

Response: `200 OK` with a JSON array of all tasks.

### Update task status

```
PATCH /tasks/{id}/status
Content-Type: application/json

{"status": "active"}
```

Valid statuses: `new`, `active`, `completed`, `removed`.

Response: `200 OK` with the updated task, or `404 Not Found` if the id is unknown.

### Task schema

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Buy groceries",
  "description": "Milk and eggs",
  "status": "new"
}
```

## Running the tests

```bash
sbt test
```

Tests start an ephemeral Postgres container via Testcontainers, run Flyway
migrations against it, and use the same Quill-based repository as production.
Make sure Docker is running before invoking `sbt test`.

## Stack

- [Scala 3](https://www.scala-lang.org/)
- [ZIO 2](https://zio.dev/)
- [ZIO HTTP 3](https://zio.github.io/zio-http/)
- [ZIO JSON](https://zio.github.io/zio-json/)
- [Quill](https://zio.dev/zio-quill/) (JDBC ZIO module) — type-safe SQL
- [PostgreSQL](https://www.postgresql.org/) — persistent storage
- [Flyway](https://flywaydb.org/) — schema migrations
- [HikariCP](https://github.com/brettwooldridge/HikariCP) — connection pool
- [Testcontainers](https://java.testcontainers.org/) (Scala) — integration tests
