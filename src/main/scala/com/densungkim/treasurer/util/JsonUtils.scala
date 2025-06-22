package com.densungkim.treasurer.util

import io.circe.generic.extras.Configuration

trait JsonUtils {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
}
