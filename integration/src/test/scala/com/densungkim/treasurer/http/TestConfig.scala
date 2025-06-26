package com.densungkim.treasurer.http

import com.densungkim.treasurer.config.PostgresConfig
import pureconfig.ConfigSource
import eu.timepit.refined.pureconfig._ // DON'T REMOVE THIS LINE
import pureconfig.generic.auto.exportReader
import slick.jdbc.PostgresProfile.api._

object TestConfig {

  val pgConfig: PostgresConfig = ConfigSource.default.at("postgres").loadOrThrow[PostgresConfig]

  val database = Database.forURL(
    url = pgConfig.jdbcUrl.value,
    user = pgConfig.user.value,
    password = pgConfig.password.value,
    driver = pgConfig.driver.value,
    executor = AsyncExecutor(
      name = "slick-executor",
      minThreads = pgConfig.connections.poolSize.value,
      maxThreads = pgConfig.connections.poolSize.value,
      maxConnections = pgConfig.connections.poolSize.value,
      queueSize = 1000,
    ),
  )

}
