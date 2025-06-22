package com.densungkim.treasurer.service

import com.densungkim.treasurer.model.ErrorModels.{TransactionNotFound, UserNotFound}
import com.densungkim.treasurer.model.transaction.{TransactionRequest, TransactionResponse, TransactionType}
import com.densungkim.treasurer.repository.{TransactionRepository, UserRepository}
import org.slf4j.LoggerFactory

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait TransactionService {
  def create(userId: UUID, request: TransactionRequest): Future[TransactionResponse]
  def update(id: UUID, userId: UUID, request: TransactionRequest): Future[TransactionResponse]
  def getById(id: UUID, userId: UUID): Future[TransactionResponse]
  def getAll(userId: UUID): Future[Seq[TransactionResponse]]
  def getByType(userId: UUID, `type`: TransactionType): Future[Seq[TransactionResponse]]
  def delete(id: UUID, userId: UUID): Future[Unit]
}

final class TransactionServiceImpl(
  repository: TransactionRepository,
  userRepository: UserRepository,
)(implicit ec: ExecutionContext)
  extends TransactionService {
  private val logger     = LoggerFactory.getLogger(getClass.getName)
  private val ValidTypes = Set("income", "expense")

  private def ensureUserExists(userId: UUID): Future[Unit] =
    userRepository.getById(userId).flatMap {
      case Some(_) => Future.unit
      case None    =>
        logger.warn(s"Operation failed: user $userId not found")
        Future.failed(UserNotFound(s"User with ID $userId not found"))
    }

  override def create(userId: UUID, request: TransactionRequest): Future[TransactionResponse] =
    for {
      _           <- ensureUserExists(userId)
      transaction <- repository.create(request.toDomain(userId))
      _            = logger.info(s"Transaction created: ${transaction.id} for user $userId")
    } yield transaction.toResponse

  override def update(id: UUID, userId: UUID, request: TransactionRequest): Future[TransactionResponse] =
    for {
      _        <- ensureUserExists(userId)
      existing <- repository.getById(id).flatMap {
                    case Some(t) if t.userId == userId => Future.successful(t)
                    case Some(_)                       =>
                      logger.warn(s"Update failed: transaction '$id' does not belong to user $userId")
                      Future.failed(TransactionNotFound(s"Transaction with ID '$id' not found"))
                    case None                          =>
                      logger.warn(s"Update failed: transaction '$id' not found")
                      Future.failed(TransactionNotFound(s"Transaction with ID '$id' not found"))
                  }
      updated  <- repository
                    .update(
                      existing.copy(
                        amount = request.amount.value,
                        description = request.description.map(_.value),
                        `type` = request.`type`,
                      ),
                    )
                    .map {
                      case Some(transaction) => transaction.toResponse
                      case None              =>
                        logger.error(s"Update failed unexpectedly for transaction '$id''")
                        throw TransactionNotFound(s"Transaction with ID '$id' not found")
                    }
      _         = logger.info(s"Transaction updated: '$id' for user $userId")
    } yield updated

  override def getById(id: UUID, userId: UUID): Future[TransactionResponse] =
    repository.getById(id).flatMap {
      case Some(t) if t.userId == userId =>
        Future.successful(t.toResponse)
      case Some(_)                       =>
        logger.warn(s"Get failed: transaction '$id' does not belong to user $userId")
        Future.failed(TransactionNotFound(s"Transaction with ID '$id' not found"))
      case None                          =>
        logger.warn(s"Get failed: transaction '$id' not found")
        Future.failed(TransactionNotFound(s"Transaction with ID '$id' not found"))
    }

  override def getAll(userId: UUID): Future[Seq[TransactionResponse]] =
    for {
      _            <- ensureUserExists(userId)
      transactions <- repository.getAll(userId)
    } yield transactions.map(_.toResponse)

  override def getByType(userId: UUID, `type`: TransactionType): Future[Seq[TransactionResponse]] =
    for {
      _            <- ensureUserExists(userId)
      transactions <- repository.getByType(userId, `type`)
    } yield transactions.map(_.toResponse)

  override def delete(id: UUID, userId: UUID): Future[Unit] =
    repository.getById(id).flatMap {
      case Some(t) if t.userId == userId =>
        repository.delete(id).map { success =>
          if (success) logger.info(s"Transaction deleted: '$id' for user $userId")
          else {
            logger.error(s"Delete failed unexpectedly for transaction '$id''")
            throw TransactionNotFound(s"Transaction with ID '$id' not found")
          }
        }
      case Some(_)                       =>
        logger.warn(s"Delete failed: transaction '$id' does not belong to user $userId")
        Future.failed(TransactionNotFound(s"Transaction with ID '$id' not found"))
      case None                          =>
        logger.warn(s"Delete failed: transaction '$id' not found")
        Future.failed(TransactionNotFound(s"Transaction with ID '$id' not found"))
    }

}
