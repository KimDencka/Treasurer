package com.densungkim.treasurer.repository

import com.densungkim.treasurer.model.transaction.{Transaction, TransactionType}
import slick.jdbc.PostgresProfile.api._

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait TransactionRepository {
  def create(transaction: Transaction): Future[Transaction]
  def update(transaction: Transaction): Future[Option[Transaction]]
  def getById(id: UUID): Future[Option[Transaction]]
  def getAll(userId: UUID): Future[Seq[Transaction]]
  def getByType(userId: UUID, transactionType: String): Future[Seq[Transaction]]
  def delete(id: UUID): Future[Boolean]
}

final class TransactionRepositoryImpl(db: Database)(implicit ec: ExecutionContext) extends TransactionRepository {
  private class TransactionsTable(tag: Tag) extends Table[Transaction](tag, "transactions") {
    def id          = column[UUID]("id", O.PrimaryKey)
    def userId      = column[UUID]("user_id")
    def amount      = column[BigDecimal]("amount")
    def `type`      = column[String]("type")
    def description = column[Option[String]]("description")
    def createdAt   = column[LocalDateTime]("created_at")
    def *           = (id, userId, amount, `type`, description, createdAt) <> ({
      case (id, userId, amount, typee, description, createdAt) =>
        Transaction(id, userId, amount, TransactionType.withName(typee), description, createdAt)
    },
    (transaction: Transaction) =>
      Some(
        (
          transaction.id,
          transaction.userId,
          transaction.amount,
          transaction.`type`.entryName,
          transaction.description,
          transaction.createdAt,
        ),
      ))
  }

  private val transactions = TableQuery[TransactionsTable]

  override def create(transaction: Transaction): Future[Transaction] =
    db.run(transactions += transaction).map(_ => transaction)

  override def update(transaction: Transaction): Future[Option[Transaction]] =
    db.run {
      transactions
        .filter(_.id === transaction.id)
        .map(t => (t.userId, t.amount, t.`type`, t.description, t.createdAt))
        .update(
          (
            transaction.userId,
            transaction.amount,
            transaction.`type`.entryName,
            transaction.description,
            transaction.createdAt,
          ),
        )
        .map {
          case 0 => None
          case _ => Some(transaction)
        }
    }

  override def getById(id: UUID): Future[Option[Transaction]] =
    db.run(transactions.filter(_.id === id).result.headOption)

  override def getAll(userId: UUID): Future[Seq[Transaction]] =
    db.run(transactions.filter(_.userId === userId).result)

  override def getByType(userId: UUID, transactionType: String): Future[Seq[Transaction]] =
    db.run(transactions.filter(t => t.userId === userId && t.`type` === transactionType).result)

  override def delete(id: UUID): Future[Boolean] =
    db.run(transactions.filter(_.id === id).delete).map(_ > 0)

}
