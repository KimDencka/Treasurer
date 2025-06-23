package com.densungkim.treasurer.service

import com.densungkim.treasurer.model.ErrorModels.TransactionNotFound
import com.densungkim.treasurer.model.transaction.{TransactionRequest, TransactionResponse, TransactionType}
import com.densungkim.treasurer.repository.TransactionRepository
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
)(implicit ec: ExecutionContext)
  extends TransactionService {
  private val logger = LoggerFactory.getLogger(getClass.getName)

  override def create(userId: UUID, request: TransactionRequest): Future[TransactionResponse] =
    for {
      transaction <- repository.create(request.toDomain(userId))
      _            = logger.info(s"Transaction created: ${transaction.id} for user $userId")
    } yield transaction.toResponse

  override def update(id: UUID, userId: UUID, request: TransactionRequest): Future[TransactionResponse] =
    for {
      existing <- repository.getById(id).map {
                    case Some(transaction) if transaction.userId == userId => transaction
                    case Some(_)                                           =>
                      logger.warn(s"Update failed: transaction '$id' does not belong to user $userId")
                      throw TransactionNotFound(s"Transaction with ID '$id' not found")
                    case None                                              =>
                      logger.warn(s"Update failed: transaction '$id' not found")
                      throw TransactionNotFound(s"Transaction with ID '$id' not found")
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
    repository.getById(id).map {
      case Some(transaction) if transaction.userId == userId =>
        transaction.toResponse
      case Some(_)                                           =>
        logger.warn(s"Get failed: transaction '$id' does not belong to user $userId")
        throw TransactionNotFound(s"Transaction with ID '$id' not found")
      case None                                              =>
        logger.warn(s"Get failed: transaction '$id' not found")
        throw TransactionNotFound(s"Transaction with ID '$id' not found")
    }

  override def getAll(userId: UUID): Future[Seq[TransactionResponse]] =
    repository.getAll(userId).map(_.map(_.toResponse))

  override def getByType(userId: UUID, `type`: TransactionType): Future[Seq[TransactionResponse]] =
    repository.getByType(userId, `type`).map(_.map(_.toResponse))

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
        throw TransactionNotFound(s"Transaction with ID '$id' not found")
      case None                          =>
        logger.warn(s"Delete failed: transaction '$id' not found")
        throw TransactionNotFound(s"Transaction with ID '$id' not found")
    }

}
