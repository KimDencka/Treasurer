package com.densungkim.treasurer.model.transaction

import java.time.Instant

final case class TransactionFilters(
  `type`: Option[TransactionType] = None,
  startDate: Option[Instant] = None,
  endDate: Option[Instant] = None,
  minAmount: Option[BigDecimal] = None,
  maxAmount: Option[BigDecimal] = None,
)
