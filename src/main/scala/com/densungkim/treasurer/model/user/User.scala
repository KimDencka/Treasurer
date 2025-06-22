package com.densungkim.treasurer.model.user

import io.scalaland.chimney.dsl._

import java.time.LocalDateTime
import java.util.UUID

final case class User(
  id: UUID,
  username: String,
  password: PasswordHash,
  createdAt: LocalDateTime,
) {
  def toResponse: UserResponse = this.transformInto[UserResponse]
}
