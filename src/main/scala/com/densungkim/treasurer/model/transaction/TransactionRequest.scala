package com.densungkim.treasurer.model.transaction

import com.densungkim.treasurer.util.JsonUtils
import com.densungkim.treasurer.util.TimeUtils.getCurrentTime
import eu.timepit.refined.types.all._
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.{Decoder, Encoder}
import io.circe.refined._

import java.util.UUID

final case class TransactionRequest(
  amount: PosBigDecimal,
  `type`: TransactionType,
  description: Option[NonEmptyString],
) {
  def toDomain(userId: UUID): Transaction =
    Transaction(
      id = UUID.randomUUID(),
      userId = userId,
      amount = amount.value,
      `type` = `type`,
      description = description.map(_.value),
      createdAt = getCurrentTime,
    )
}
object TransactionRequest extends JsonUtils {
  val request                                       = TransactionRequest(
    PosBigDecimal.unsafeFrom(BigDecimal("100000")),
    TransactionType.INCOME,
    Some(NonEmptyString.unsafeFrom("Some description")),
  )
  implicit val encoder: Encoder[TransactionRequest] = deriveConfiguredEncoder
  implicit val decoder: Decoder[TransactionRequest] = deriveConfiguredDecoder

  import io.circe.syntax._
  val json = request.asJson
  val decoded = json.as[TransactionRequest]
  println(s"JSON: $json")
  println(s"DECODED: $decoded")
}
