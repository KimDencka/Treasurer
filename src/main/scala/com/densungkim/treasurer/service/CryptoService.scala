package com.densungkim.treasurer.service

import com.densungkim.treasurer.model.ErrorModels.CryptoException
import com.densungkim.treasurer.model.user.PasswordHash
import com.github.t3hnar.bcrypt._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

final class CryptoService(implicit ec: ExecutionContext) {
  private val logger       = LoggerFactory.getLogger(getClass.getName)
  private val BcryptRounds = 12

  def hashPassword(password: String): Future[PasswordHash] =
    Future
      .fromTry(password.bcryptSafeBounded(BcryptRounds))
      .map(PasswordHash.apply)
      .recover { case e: Exception =>
        logger.error("Failed to hash password", e)
        throw CryptoException(s"Failed to hash password; $e")
      }

  def validate(password: String, passwordHash: PasswordHash): Future[Boolean] =
    Future
      .fromTry(password.isBcryptedSafeBounded(passwordHash.value))
      .recover { case e: Exception =>
        logger.error("Failed to validate password", e)
        throw CryptoException(s"Failed to validate password; e")
      }
}
