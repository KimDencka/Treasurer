package com.densungkim.treasurer.model.transaction

import com.densungkim.treasurer.util.JsonUtils
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.{Decoder, Encoder}

import java.time.LocalDateTime
import java.util.UUID

final case class TransactionResponse(
  id: UUID,
  userId: UUID,
  amount: BigDecimal,
  `type`: TransactionType,
  description: Option[String],
  createdAt: LocalDateTime,
)
object TransactionResponse extends JsonUtils {
  implicit val encoder: Encoder[TransactionResponse] = deriveConfiguredEncoder
  implicit val decoder: Decoder[TransactionResponse] = deriveConfiguredDecoder
}
