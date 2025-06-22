package com.densungkim.treasurer.config

import eu.timepit.refined.types.numeric.PosInt

final case class ExecutionContextsConfig (
  http: Threads,
  database: Threads,
  cpu: Threads
)

final case class Threads(threads: PosInt)
