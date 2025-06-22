package com.densungkim.treasurer.config

import eu.timepit.refined.types.all.{NonEmptyString, PosInt}

import scala.concurrent.duration.FiniteDuration

final case class HttpConfig(
  host: NonEmptyString,
  port: PosInt,
  requestTimeout: FiniteDuration,
  idleTimeout: FiniteDuration,
  backlog: PosInt
)
