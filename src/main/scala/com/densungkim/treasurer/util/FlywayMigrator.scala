package com.densungkim.treasurer.util

import com.densungkim.treasurer.config.PostgresConfig
import eu.timepit.refined.auto.autoUnwrap
import org.flywaydb.core.Flyway
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Try}

object FlywayMigrator {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def migrateDatabase(
    config: PostgresConfig,
    additionalMigrationLocations: List[String] = List.empty,
    addDefaultLocation: Boolean = true,
  ): Try[Unit] = Try {
    logger.info("Starting database migration...")
    val flyway          = Flyway
      .configure()
      .locations(
        (if (addDefaultLocation) List("classpath:db/migration") else Nil) ::: additionalMigrationLocations: _*,
      )
      .dataSource(config.jdbcUrl, config.user, config.password)
      .load()
    val migrationResult = flyway.migrate()
    logger.info(s"Applied ${migrationResult.migrationsExecuted} migrations.")
  }.recoverWith { case e =>
    logger.error("Database migration failed.", e)
    Failure(e)
  }
}
