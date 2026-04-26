# life-map

A personal task management (todo list) REST API built with Scala 3 and ZIO.

## Requirements

- JDK 17+
- [sbt](https://www.scala-sbt.org/) 1.10+

## Running the server

```bash
sbt run
```

The server starts on `http://localhost:8080` by default.

### Endpoints

| Method | Path | Description          |
|--------|------|----------------------|
| GET    | `/`  | Returns Hello World  |

## Running the tests

```bash
sbt test
```

## Stack

- [Scala 3](https://www.scala-lang.org/)
- [ZIO 2](https://zio.dev/)
- [ZIO HTTP 3](https://zio.github.io/zio-http/)
