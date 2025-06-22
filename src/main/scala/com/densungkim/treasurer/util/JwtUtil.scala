package com.densungkim.treasurer.util

import com.densungkim.treasurer.config.JwtConfig
import io.circe.parser._
import io.circe.syntax._
import pdi.jwt._

import java.time.Instant
import java.util.UUID
import scala.util.Try

object JwtUtil {
  def createToken(userId: UUID, config: JwtConfig): String = {
    val claim = JwtClaim(
      content = Map("user_id" -> userId.toString).asJson.noSpaces,
      expiration = Some(Instant.now.plusSeconds(config.expiration.toSeconds).getEpochSecond),
      issuedAt = Some(Instant.now.getEpochSecond),
    )
    JwtCirce.encode(claim, config.secret.value, JwtAlgorithm.HS256)
  }

  def validateToken(token: String, config: JwtConfig): Option[UUID] =
    for {
      claim     <- JwtCirce.decode(token, config.secret.value, Seq(JwtAlgorithm.HS256)).toOption
      json      <- parse(claim.content).toOption
      userIdStr <- json.hcursor.downField("user_id").as[String].toOption
      result    <- Try(UUID.fromString(userIdStr)).toOption
    } yield result

}
