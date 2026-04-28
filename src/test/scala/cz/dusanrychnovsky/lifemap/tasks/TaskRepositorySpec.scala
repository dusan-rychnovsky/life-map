package cz.dusanrychnovsky.lifemap.tasks

import zio.ZIO
import zio.test._
import java.util.UUID

object TaskRepositorySpec extends ZIOSpecDefault:

  def spec = suite("TaskRepository")(
    test("create returns a task with New status and the given title and description") {
      for
        _    <- PostgresTestSupport.truncate
        repo <- ZIO.service[TaskRepository]
        task <- repo.create("Buy groceries", "Milk, eggs, bread")
      yield assertTrue(
        task.title == "Buy groceries",
        task.description == "Milk, eggs, bread",
        task.status == TaskStatus.New,
      )
    },

    test("getAll returns every created task") {
      for
        _     <- PostgresTestSupport.truncate
        repo  <- ZIO.service[TaskRepository]
        t1    <- repo.create("Task 1", "Desc 1")
        t2    <- repo.create("Task 2", "Desc 2")
        tasks <- repo.getAll
      yield assertTrue(tasks.exists(_.id == t1.id)) &&
            assertTrue(tasks.exists(_.id == t2.id))
    },

    test("updateStatus changes the task's status") {
      for
        _       <- PostgresTestSupport.truncate
        repo    <- ZIO.service[TaskRepository]
        task    <- repo.create("Task", "Desc")
        updated <- repo.updateStatus(task.id, TaskStatus.Active)
      yield assertTrue(updated.exists(_.status == TaskStatus.Active))
    },

    test("updateStatus returns None for an unknown id") {
      for
        _      <- PostgresTestSupport.truncate
        repo   <- ZIO.service[TaskRepository]
        result <- repo.updateStatus(UUID.randomUUID(), TaskStatus.Active)
      yield assertTrue(result.isEmpty)
    },
  ).provideShared(PostgresTestSupport.layer) @@ TestAspect.sequential
