package com.densungkim.treasurer

import eu.timepit.refined.api.Refined
import eu.timepit.refined.types.string.NonEmptyString
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

import java.time.temporal.ChronoUnit
import java.time.{LocalDate, LocalDateTime}

trait CommonArbitraries {

  def genString(n: Int): Gen[String] = Gen.listOfN(n, Gen.alphaChar).map(_.mkString)

  def genBoolean: Gen[Boolean] = Gen.oneOf(true, false)

  def genStringNoShorterThan(n: Int): Gen[String] =
    for {
      delta  <- Gen.choose(0, 10)
      result <- genString(n + delta)
    } yield result

  def genStringNoLongerThan(n: Int): Gen[String] =
    for {
      length <- Gen.choose(0, n - 1)
      result <- if (length == 0) Gen.const("") else genString(length)
    } yield result

  def genNonNegativeInt: Gen[Int] = arbitrary[Int].map(_.abs)

  def genNonNegativeLong: Gen[Long] = arbitrary[Long].map(_.abs)

  def genNonNegativeDouble: Gen[Double] = arbitrary[Double].map(_.abs)

  def genNonNegativeBigDecimal: Gen[BigDecimal] = genNonNegativeInt.map(v => BigDecimal(s"$v.00"))

  def genSmallInt: Gen[Int] = Gen.choose(1, 64)

  def genSmallLong: Gen[Long] = Gen.choose[Long](1, 64)

  def genDoubleRange(min: Double = 1, max: Double): Gen[Double] = Gen.choose[Double](min, max)

  def genShortString: Gen[String] =
    for {
      n   <- Gen.choose(1, 16)
      str <- genString(n)
    } yield str

  def genRefinedNEString: Gen[NonEmptyString] = genStringNoShorterThan(15).map(Refined.unsafeApply)

  def genLocalDateTime: Gen[LocalDateTime] =
    Gen
      .choose(
        LocalDateTime.now().minusYears(10),
        LocalDateTime.now(),
      )
      .map(_.truncatedTo(ChronoUnit.MILLIS))

  def genLocalDate: Gen[LocalDate] =
    Gen.choose(
      LocalDate.now().minusYears(10),
      LocalDate.now(),
    )

}
