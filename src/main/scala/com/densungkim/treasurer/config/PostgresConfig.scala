package com.densungkim.treasurer.config

import eu.timepit.refined.types.all.{NonEmptyString, PosInt, UserPortNumber}

final case class PostgresConfig(
  driver: NonEmptyString,
  host: NonEmptyString,
  port: UserPortNumber,
  user: NonEmptyString,
  schema: NonEmptyString,
  password: NonEmptyString,
  jdbcUrl: NonEmptyString,
  connections: DbConnectionsCfg,
)

final case class DbConnectionsCfg(
  poolSize: PosInt,
  maxLifetime: PosInt,
  minimumIdle: PosInt,
)
