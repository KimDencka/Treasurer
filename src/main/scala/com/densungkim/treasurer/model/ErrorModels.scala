package com.densungkim.treasurer.model

import scala.util.control.NoStackTrace

object ErrorModels {
  final case class CryptoException(message: String)          extends NoStackTrace
  final case class UserNotFound(message: String)             extends NoStackTrace
  final case class IncorrectPassword(message: String)        extends NoStackTrace
  final case class TransactionNotFound(message: String)      extends NoStackTrace
  final case class IncorrectTransactionType(message: String) extends NoStackTrace
}
