package com.densungkim.treasurer.model.transaction

import com.densungkim.treasurer.util.JsonUtils
import enumeratum._
//import io.circe.{Decoder, Encoder}

sealed trait TransactionType extends EnumEntry
object TransactionType       extends Enum[TransactionType] with CirceEnum[TransactionType] with JsonUtils {
  val values: IndexedSeq[TransactionType] = findValues
  case object INCOME  extends TransactionType
  case object EXPENSE extends TransactionType

//  implicit val encoder: Encoder[TransactionType] =
//    Encoder.forProduct1("type")(_.entryName)
//  implicit val decoder: Decoder[TransactionType] =
//    Decoder.instance(_.downField("type").as[String].map(TransactionType.withName))
}
