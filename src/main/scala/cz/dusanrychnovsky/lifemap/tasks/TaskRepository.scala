package cz.dusanrychnovsky.lifemap.tasks

import zio.{UIO, ULayer, ZLayer, Ref, Random}
import java.util.UUID

trait TaskRepository:
  def create(title: String, description: String): UIO[Task]
  def getAll: UIO[List[Task]]
  def updateStatus(id: UUID, status: TaskStatus): UIO[Option[Task]]

object TaskRepository:
  val layer: ULayer[TaskRepository] =
    ZLayer.fromZIO(
      Ref.make(Map.empty[UUID, Task]).map(InMemoryTaskRepository(_))
    )

private final class InMemoryTaskRepository(ref: Ref[Map[UUID, Task]]) extends TaskRepository:

  def create(title: String, description: String): UIO[Task] =
    for
      id   <- Random.nextUUID
      task  = Task(id, title, description, TaskStatus.New)
      _    <- ref.update(_ + (id -> task))
    yield task

  def getAll: UIO[List[Task]] =
    ref.get.map(_.values.toList)

  def updateStatus(id: UUID, status: TaskStatus): UIO[Option[Task]] =
    ref.modify: tasks =>
      tasks.get(id) match
        case None =>
          (None, tasks)
        case Some(task) =>
          val updated = task.copy(status = status)
          (Some(updated), tasks + (id -> updated))
