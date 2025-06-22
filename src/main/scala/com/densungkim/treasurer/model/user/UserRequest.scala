package com.densungkim.treasurer.model.user

import com.densungkim.treasurer.util.JsonUtils
import com.densungkim.treasurer.util.TimeUtils.getCurrentTime
import eu.timepit.refined.types.all.NonEmptyString
import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.refined._

import java.util.UUID

final case class UserRequest(
  username: NonEmptyString,
  password: NonEmptyString,
) {
  def toDomain(passwordHash: PasswordHash): User =
    User(
      id = UUID.randomUUID(),
      username = username.value,
      password = passwordHash,
      createdAt = getCurrentTime,
    )
}
object UserRequest extends JsonUtils {
  implicit val encoder: Encoder[UserRequest] = deriveConfiguredEncoder
  implicit val decoder: Decoder[UserRequest] = deriveConfiguredDecoder
}
