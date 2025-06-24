package com.densungkim.treasurer.util

import com.densungkim.treasurer.config.JwtConfig
import io.circe.parser._
import io.circe.syntax._
import pdi.jwt._

import java.time.Instant
import java.util.UUID
import scala.util.Try

trait JwtUtil {
  def createToken(userId: UUID, config: JwtConfig): String
  def validateToken(token: String, config: JwtConfig): Option[UUID]
}
object JwtUtil {

}
