package com.densungkim.treasurer.config

import eu.timepit.refined.types.all.{NonEmptyString, PosInt}

final case class HttpConfig(
  host: NonEmptyString,
  port: PosInt,
)
