package com.densungkim.treasurer.model.transaction

final case class StatsResponse(
  totalIncome: BigDecimal,
  totalExpense: BigDecimal,
)
