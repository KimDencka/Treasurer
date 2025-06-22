package com.densungkim.treasurer.model.user

import com.densungkim.treasurer.util.JsonUtils
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.{Decoder, Encoder}

import java.time.LocalDateTime
import java.util.UUID

final case class UserResponse(
  id: UUID,
  username: String,
  createdAt: LocalDateTime,
)
object UserResponse extends JsonUtils {
  implicit val encoder: Encoder[UserResponse] = deriveConfiguredEncoder
  implicit val decoder: Decoder[UserResponse] = deriveConfiguredDecoder
}
