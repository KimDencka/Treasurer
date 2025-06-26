package com.densungkim.treasurer.service

import com.densungkim.treasurer.TestUtils
import com.densungkim.treasurer.model.ErrorModels.TransactionNotFound
import com.densungkim.treasurer.model.transaction.{Transaction, TransactionType}
import com.densungkim.treasurer.repository.TransactionRepository
import org.scalacheck.Gen

import scala.concurrent.Future

class TransactionServiceImplSpec extends TestUtils {
  val mockTransactionRepository: TransactionRepository = mock[TransactionRepository]
  val transactionService: TransactionService           = new TransactionServiceImpl(mockTransactionRepository)

  "TransactionServiceImpl" should {

    "create a new transaction and return TransactionResponse" in {
      forAll(genTransactionRequest, Gen.uuid) { (request, userId) =>
        reset(mockTransactionRepository)
        val transaction = request.toDomain(userId)
        mockTransactionRepository.create(any[Transaction]) returns Future.successful(transaction)

        whenReady(transactionService.create(userId, request)) { response =>
          response shouldBe transaction.toResponse
          mockTransactionRepository.create(any[Transaction]) was called
        }
      }
    }

    "update an existing transaction and return TransactionResponse" in {
      forAll(genTransactionRequest, Gen.uuid, Gen.uuid) { (request, transactionId, userId) =>
        reset(mockTransactionRepository)
        val existingTransaction = Transaction(
          transactionId,
          userId,
          request.amount.value,
          request.`type`,
          request.description.map(_.value),
          genLocalDateTime.sample.get,
        )
        val updatedTransaction  = existingTransaction.copy(
          amount = request.amount.value,
          `type` = request.`type`,
          description = request.description.map(_.value),
        )
        mockTransactionRepository.getById(transactionId) returns Future.successful(Some(existingTransaction))
        mockTransactionRepository.update(updatedTransaction) returns Future.successful(Some(updatedTransaction))

        whenReady(transactionService.update(transactionId, userId, request)) { response =>
          response shouldBe updatedTransaction.toResponse
          mockTransactionRepository.getById(transactionId) was called
          mockTransactionRepository.update(updatedTransaction) was called
        }
      }
    }

    "fail update with TransactionNotFound if transaction does not exist" in {
      forAll(genTransactionRequest, Gen.uuid, Gen.uuid) { (request, transactionId, userId) =>
        reset(mockTransactionRepository)
        mockTransactionRepository.getById(transactionId) returns Future.successful(None)

        whenReady(transactionService.update(transactionId, userId, request).failed) { response =>
          response shouldBe a[TransactionNotFound]
          response.asInstanceOf[TransactionNotFound].message shouldBe s"Transaction with ID '$transactionId' not found"
          mockTransactionRepository.getById(transactionId) was called
        }
      }
    }

    "fail update with TransactionNotFound if transaction belongs to another user" in {
      forAll(genTransactionRequest, Gen.uuid, Gen.uuid, Gen.uuid) { (request, transactionId, userId, otherUserId) =>
        reset(mockTransactionRepository)
        val transaction = Transaction(
          transactionId,
          otherUserId,
          request.amount.value,
          request.`type`,
          request.description.map(_.value),
          genLocalDateTime.sample.get,
        )
        mockTransactionRepository.getById(transactionId) returns Future.successful(Some(transaction))

        whenReady(transactionService.update(transactionId, userId, request).failed) { response =>
          response shouldBe a[TransactionNotFound]
          response.asInstanceOf[TransactionNotFound].message shouldBe s"Transaction with ID '$transactionId' not found"
          mockTransactionRepository.getById(transactionId) was called
        }
      }
    }

    "getById returns TransactionResponse for existing transaction" in {
      forAll(genTransaction) { transaction =>
        reset(mockTransactionRepository)
        mockTransactionRepository.getById(transaction.id) returns Future.successful(Some(transaction))

        whenReady(transactionService.getById(transaction.id, transaction.userId)) { response =>
          response shouldBe transaction.toResponse
          mockTransactionRepository.getById(transaction.id) was called
        }
      }
    }

    "fail getById with TransactionNotFound if transaction does not exist" in {
      forAll(Gen.uuid, Gen.uuid) { (transactionId, userId) =>
        reset(mockTransactionRepository)
        mockTransactionRepository.getById(transactionId) returns Future.successful(None)

        whenReady(transactionService.getById(transactionId, userId).failed) { response =>
          response shouldBe a[TransactionNotFound]
          response.asInstanceOf[TransactionNotFound].message shouldBe s"Transaction with ID '$transactionId' not found"
          mockTransactionRepository.getById(transactionId) was called
        }
      }
    }

    "fail getById with TransactionNotFound if transaction belongs to another user" in {
      forAll(genTransaction, Gen.uuid) { (transaction, otherUserId) =>
        reset(mockTransactionRepository)
        mockTransactionRepository.getById(transaction.id) returns Future.successful(Some(transaction))

        whenReady(transactionService.getById(transaction.id, otherUserId).failed) { response =>
          response shouldBe a[TransactionNotFound]
          response
            .asInstanceOf[TransactionNotFound]
            .message shouldBe s"Transaction with ID '${transaction.id}' not found"
          mockTransactionRepository.getById(transaction.id) was called
        }
      }
    }

    "getAll returns a sequence of TransactionResponse for a user" in {
      forAll(Gen.uuid, Gen.listOf(genTransaction)) { (userId, transactions) =>
        reset(mockTransactionRepository)
        val userTransactions = transactions.map(t => t.copy(userId = userId))
        mockTransactionRepository.getAll(userId) returns Future.successful(userTransactions)

        whenReady(transactionService.getAll(userId)) { response =>
          response shouldBe userTransactions.map(_.toResponse)
          mockTransactionRepository.getAll(userId) was called
        }
      }
    }

    "getAll returns an empty sequence when no transactions exist" in {
      forAll(Gen.uuid) { userId =>
        reset(mockTransactionRepository)
        mockTransactionRepository.getAll(userId) returns Future.successful(Seq.empty)

        whenReady(transactionService.getAll(userId)) { response =>
          response shouldBe Seq.empty
          mockTransactionRepository.getAll(userId) was called
        }
      }
    }

    "getByType returns a sequence of TransactionResponse for a user and type" in {
      forAll(Gen.uuid, Gen.oneOf(TransactionType.values), Gen.listOf(genTransaction)) {
        (userId, transactionType, transactions) =>
          reset(mockTransactionRepository)
          val userTransactions = transactions.map(t => t.copy(userId = userId, `type` = transactionType))
          mockTransactionRepository.getByType(userId, transactionType) returns Future.successful(userTransactions)

          whenReady(transactionService.getByType(userId, transactionType)) { response =>
            response shouldBe userTransactions.map(_.toResponse)
            mockTransactionRepository.getByType(userId, transactionType) was called
          }
      }
    }

    "getByType returns an empty sequence when no transactions exist for type" in {
      forAll(Gen.uuid, Gen.oneOf(TransactionType.values)) { (userId, transactionType) =>
        reset(mockTransactionRepository)
        mockTransactionRepository.getByType(userId, transactionType) returns Future.successful(Seq.empty)

        whenReady(transactionService.getByType(userId, transactionType)) { response =>
          response shouldBe Seq.empty
          mockTransactionRepository.getByType(userId, transactionType) was called
        }
      }
    }

    "delete an existing transaction" in {
      forAll(genTransaction) { transaction =>
        reset(mockTransactionRepository)
        mockTransactionRepository.getById(transaction.id) returns Future.successful(Some(transaction))
        mockTransactionRepository.delete(transaction.id) returns Future.successful(true)

        whenReady(transactionService.delete(transaction.id, transaction.userId)) { response =>
          response shouldBe ()
          mockTransactionRepository.getById(transaction.id) was called
          mockTransactionRepository.delete(transaction.id) was called
        }
      }
    }

    "fail delete with TransactionNotFound if transaction does not exist" in {
      forAll(Gen.uuid, Gen.uuid) { (transactionId, userId) =>
        reset(mockTransactionRepository)
        mockTransactionRepository.getById(transactionId) returns Future.successful(None)

        whenReady(transactionService.delete(transactionId, userId).failed) { response =>
          response shouldBe a[TransactionNotFound]
          response.asInstanceOf[TransactionNotFound].message shouldBe s"Transaction with ID '$transactionId' not found"
          mockTransactionRepository.getById(transactionId) was called
        }
      }
    }

    "fail delete with TransactionNotFound if transaction belongs to another user" in {
      forAll(genTransaction, Gen.uuid) { (transaction, otherUserId) =>
        reset(mockTransactionRepository)
        mockTransactionRepository.getById(transaction.id) returns Future.successful(Some(transaction))

        whenReady(transactionService.delete(transaction.id, otherUserId).failed) { response =>
          response shouldBe a[TransactionNotFound]
          response
            .asInstanceOf[TransactionNotFound]
            .message shouldBe s"Transaction with ID '${transaction.id}' not found"
          mockTransactionRepository.getById(transaction.id) was called
        }
      }
    }

  }

}
