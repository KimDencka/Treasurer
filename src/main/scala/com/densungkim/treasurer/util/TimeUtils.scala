package com.densungkim.treasurer.util

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object TimeUtils {
  implicit class TimeOpts(target: LocalDateTime) {
    implicit def truncateTime: LocalDateTime = target.truncatedTo(ChronoUnit.MILLIS)
  }

  def getCurrentTime: LocalDateTime = LocalDateTime.now().truncateTime
}
