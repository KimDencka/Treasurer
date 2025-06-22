package com.densungkim.treasurer.model.transaction

import io.scalaland.chimney.dsl.TransformerOps

import java.time.LocalDateTime
import java.util.UUID

final case class Transaction(
  id: UUID,
  userId: UUID,
  amount: BigDecimal,
  `type`: TransactionType,
  description: Option[String],
  createdAt: LocalDateTime,
) {
  def toResponse: TransactionResponse = this.transformInto[TransactionResponse]
}
