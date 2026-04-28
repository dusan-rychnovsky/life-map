package cz.dusanrychnovsky.lifemap.tasks

import cz.dusanrychnovsky.lifemap.db.Database
import io.getquill._
import zio.{Random, ZIO, ZLayer}
import java.util.UUID

trait TaskRepository:
  def create(title: String, description: String): ZIO[Any, Throwable, Task]
  def getAll: ZIO[Any, Throwable, List[Task]]
  def updateStatus(id: UUID, status: TaskStatus): ZIO[Any, Throwable, Option[Task]]

object TaskRepository:
  val layer: ZLayer[Database.Ctx, Nothing, TaskRepository] =
    ZLayer.fromFunction(PostgresTaskRepository(_))

private final class PostgresTaskRepository(quill: Database.Ctx) extends TaskRepository:
  import quill._

  private given MappedEncoding[TaskStatus, String] = MappedEncoding:
    case TaskStatus.New       => "new"
    case TaskStatus.Active    => "active"
    case TaskStatus.Completed => "completed"
    case TaskStatus.Removed   => "removed"

  private given MappedEncoding[String, TaskStatus] = MappedEncoding:
    case "new"       => TaskStatus.New
    case "active"    => TaskStatus.Active
    case "completed" => TaskStatus.Completed
    case "removed"   => TaskStatus.Removed
    case other       => throw new RuntimeException(s"Unknown task status in DB: '$other'")

  private inline def tasks = quote { querySchema[Task]("tasks") }

  def create(title: String, description: String): ZIO[Any, Throwable, Task] =
    for
      id   <- Random.nextUUID
      task  = Task(id, title, description, TaskStatus.New)
      _    <- run(quote { tasks.insertValue(lift(task)) })
    yield task

  def getAll: ZIO[Any, Throwable, List[Task]] =
    run(quote { tasks })

  def updateStatus(id: UUID, status: TaskStatus): ZIO[Any, Throwable, Option[Task]] =
    for
      n      <- run(quote { tasks.filter(_.id == lift(id)).update(_.status -> lift(status)) })
      result <- if n == 0 then ZIO.succeed(None)
                else run(quote { tasks.filter(_.id == lift(id)) }).map(_.headOption)
    yield result
