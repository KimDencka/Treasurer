package com.densungkim.treasurer.model

import io.circe.{Decoder, Encoder}
import io.circe.syntax._

/**
 * Represents a paginated response for a collection of items.
 *
 * @param items
 *   the list of items on the current page
 * @param total
 *   the total number of items across all pages
 * @param limit
 *   the maximum number of items per page
 * @param offset
 *   the starting position of items for the current page
 * @tparam T
 *   the type of items in the response
 */
case class Paginated[+T](items: List[T], total: Long, limit: Int, offset: Int) {

  /** the total number of pages */
  val totalPages: Int = (total.toDouble / limit).ceil.toInt
}

object Paginated {
  implicit def paginatedEncoder[T: Encoder]: Encoder[Paginated[T]] =
    Encoder.forProduct5("items", "total", "limit", "offset", "total_pages") { p =>
      (p.items.asJson, p.total, p.limit, p.offset, p.totalPages)
    }
  implicit def paginatedDecoder[T: Decoder]: Decoder[Paginated[T]] =
    Decoder.instance { h =>
      for {
        items  <- h.downField("items").as[List[T]]
        total  <- h.downField("total").as[Long]
        limit  <- h.downField("limit").as[Int]
        offset <- h.downField("offset").as[Int]
      } yield Paginated(items, total, limit, offset)
    }
}
