package com.densungkim.treasurer.service

import com.densungkim.treasurer.model.user.PasswordHash
import com.github.t3hnar.bcrypt._
import org.slf4j.LoggerFactory

import scala.util.{Failure, Try}

final class CryptoService {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def hashPassword(password: String): Try[PasswordHash] =
    password.bcryptSafeBounded(12).map(PasswordHash.apply).recoverWith { case e: Exception =>
      logger.error("Failed to hash password", e)
      Failure(e)
    }

  def isValid(password: String, passwordHash: PasswordHash): Try[Boolean] =
    password.isBcryptedSafeBounded(passwordHash.value).recoverWith { case e: Exception =>
      logger.error("Failed to validate password", e)
      Failure(e)
    }
}
