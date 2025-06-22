package com.densungkim.treasurer.config

import eu.timepit.refined.types.string.NonEmptyString

import scala.concurrent.duration.FiniteDuration

final case class JwtConfig(
  secret: NonEmptyString,
  expiration: FiniteDuration,
)
