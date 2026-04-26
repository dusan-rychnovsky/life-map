# life-map

A personal task management REST API built with Scala 3 and ZIO.

## Requirements

- JDK 17+
- [sbt](https://www.scala-sbt.org/) 1.10+

## Running the server

```bash
sbt run
```

The server starts on `http://localhost:8080` by default.

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

## Stack

- [Scala 3](https://www.scala-lang.org/)
- [ZIO 2](https://zio.dev/)
- [ZIO HTTP 3](https://zio.github.io/zio-http/)
- [ZIO JSON](https://zio.github.io/zio-json/)
