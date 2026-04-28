package cz.dusanrychnovsky.lifemap.db

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import org.flywaydb.core.Flyway
import zio.{ZIO, ZLayer}
import javax.sql.DataSource

object Database:

  type Ctx = Quill.Postgres[SnakeCase]

  final case class Config(jdbcUrl: String, username: String, password: String)

  object Config:
    val layer: ZLayer[Any, Throwable, Config] =
      ZLayer:
        for
          url      <- env("DB_URL")
          username <- env("DB_USER")
          password <- env("DB_PASSWORD")
        yield Config(url, username, password)

    private def env(name: String): ZIO[Any, Throwable, String] =
      ZIO
        .fromOption(DotEnv.get(name))
        .orElseFail(new RuntimeException(
          s"Required env var $name is not set (set it in your shell or in a .env file at the repo root)"
        ))

  val dataSourceLayer: ZLayer[Config, Throwable, DataSource] =
    ZLayer.scoped:
      for
        cfg <- ZIO.service[Config]
        ds  <- ZIO.acquireRelease(
                 ZIO.attemptBlocking {
                   val hc = new HikariConfig()
                   hc.setJdbcUrl(cfg.jdbcUrl)
                   hc.setUsername(cfg.username)
                   hc.setPassword(cfg.password)
                   hc.setDriverClassName("org.postgresql.Driver")
                   new HikariDataSource(hc): DataSource
                 }
               )(ds => ZIO.attemptBlocking(ds.asInstanceOf[HikariDataSource].close()).orDie)
      yield ds

  val migrate: ZLayer[DataSource, Throwable, DataSource] =
    ZLayer.fromZIO:
      for
        ds <- ZIO.service[DataSource]
        _  <- ZIO.attemptBlocking {
                Flyway.configure().dataSource(ds).load().migrate()
              }
      yield ds

  val quillLayer: ZLayer[DataSource, Nothing, Ctx] =
    Quill.Postgres.fromNamingStrategy(SnakeCase)

  val live: ZLayer[Any, Throwable, Ctx] =
    Config.layer >>> dataSourceLayer >>> migrate >>> quillLayer
