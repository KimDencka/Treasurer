package com.densungkim.treasurer.generators

import com.densungkim.treasurer.model.transaction._
import eu.timepit.refined.types.numeric.PosBigDecimal
import org.scalacheck.Gen

trait TransactionGenerators extends CommonGenerators {

  def genTransactionRequest: Gen[TransactionRequest] =
    for {
      amount      <- genPosBigDecimal
      typee       <- Gen.oneOf(TransactionType.values)
      description <- Gen.option(genNonEmptyString)
    } yield TransactionRequest(amount, typee, description)

  def genTransaction: Gen[Transaction] =
    for {
      id          <- Gen.uuid
      userId      <- Gen.uuid
      amount      <- genNonNegativeBigDecimal
      typee       <- Gen.oneOf(TransactionType.values)
      description <- Gen.option(genShortString)
      createdAt   <- genLocalDateTime
    } yield Transaction(id, userId, amount, typee, description, createdAt)

  def genTransactionResponse: Gen[TransactionResponse] = genTransaction.map(_.toResponse)

}
