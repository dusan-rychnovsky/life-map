package cz.dusanrychnovsky.lifemap.tasks

import com.dimafeng.testcontainers.PostgreSQLContainer
import cz.dusanrychnovsky.lifemap.db.Database
import org.testcontainers.utility.DockerImageName
import zio.{ZIO, ZLayer}
import javax.sql.DataSource

/** Shared test infrastructure: spins up a single PostgreSQL container per
  * spec, runs Flyway migrations once, and provides a [[TaskRepository]]
  * backed by it.
  *
  * Tests must call [[truncate]] at the start of each test body to start
  * with an empty `tasks` table.
  */
object PostgresTestSupport:

  private val containerLayer: ZLayer[Any, Throwable, PostgreSQLContainer] =
    ZLayer.scoped:
      ZIO.acquireRelease(
        ZIO.attemptBlocking {
          val c = PostgreSQLContainer(
            dockerImageNameOverride = DockerImageName.parse("postgres:16-alpine")
          )
          c.start()
          c
        }
      )(c => ZIO.attemptBlocking(c.stop()).orDie)

  private val configFromContainer: ZLayer[PostgreSQLContainer, Nothing, Database.Config] =
    ZLayer:
      ZIO.serviceWith[PostgreSQLContainer]: c =>
        Database.Config(c.jdbcUrl, c.username, c.password)

  /** Full test environment: a started Postgres container, a migrated
    * `DataSource`, a Quill context, and a [[TaskRepository]]. */
  val layer: ZLayer[Any, Throwable, DataSource & Database.Ctx & TaskRepository] =
    containerLayer >>> configFromContainer >>>
      Database.dataSourceLayer >>> Database.migrate >+>
      Database.quillLayer >+> TaskRepository.layer

  /** Removes all rows from the `tasks` table. Call this at the start of
    * each test body so tests don't see each other's writes. */
  val truncate: ZIO[DataSource, Throwable, Unit] =
    ZIO.serviceWithZIO[DataSource]: ds =>
      ZIO.attemptBlocking {
        val conn = ds.getConnection
        try
          val stmt = conn.createStatement()
          try stmt.executeUpdate("TRUNCATE TABLE tasks")
          finally stmt.close()
        finally conn.close()
      }.unit
