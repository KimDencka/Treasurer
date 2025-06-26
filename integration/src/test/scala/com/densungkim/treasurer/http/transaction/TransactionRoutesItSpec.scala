package com.densungkim.treasurer.http.transaction

import akka.http.javadsl.model.headers.HttpCredentials
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.densungkim.treasurer.TestUtils
import com.densungkim.treasurer.config.JwtConfig
import com.densungkim.treasurer.http.TestConfig.database
import com.densungkim.treasurer.model.transaction.{TransactionResponse, TransactionType}
import com.densungkim.treasurer.repository.{TransactionRepositoryImpl, UserRepositoryImpl}
import com.densungkim.treasurer.service.{AuthServiceImpl, CryptoService, TransactionServiceImpl}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Json
import io.circe.syntax._

import java.util.UUID
import scala.concurrent.duration._

class TransactionRoutesItSpec extends TestUtils with ScalatestRouteTest {

  val cryptoService: CryptoService                     = new CryptoService()
  val userRepository: UserRepositoryImpl               = new UserRepositoryImpl(database)
  val transactionRepository: TransactionRepositoryImpl = new TransactionRepositoryImpl(database)
  val jwtConfig: JwtConfig                             = JwtConfig(NonEmptyString.unsafeFrom("test-secret"), 60.minutes)
  val authService: AuthServiceImpl                     = new AuthServiceImpl(cryptoService, userRepository, jwtConfig)
  val transactionService: TransactionServiceImpl       = new TransactionServiceImpl(transactionRepository)
  val routes: Route                                    = new TransactionRoutes(transactionService, authService).routes

  "TransactionRoutes" should {

    def createAuthHeader: Authorization = {
      val token = (
        for {
          user    <- authService.register(genUserRequest.sample.get)
          newToken = authService.createToken(user.id, jwtConfig)
        } yield newToken
      ).futureValue(timeout(1.seconds))
      Authorization(HttpCredentials.createOAuth2BearerToken(token))
    }

    "POST /transactions should create a new transaction and return TransactionResponse" in {
      val authHeader: Authorization = createAuthHeader
      val transactionRequest        = genTransactionRequest.sample.get
      Post("/transactions", transactionRequest.asJson).addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.Created
        val response = responseAs[TransactionResponse]
        response.amount shouldBe transactionRequest.amount.value
        response.description shouldBe transactionRequest.description.map(_.value)
        response.`type` shouldBe transactionRequest.`type`
        response.id should not be null
        response.createdAt should not be null
      }
    }

    "POST /transactions should fail with BadRequest for invalid JSON" in {
      val authHeader: Authorization = createAuthHeader
      val invalidJson               = Json.obj("amount" -> genSmallInt.sample.get.asJson)
      Post("/transactions", invalidJson).addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.BadRequest
        val body = response.entity.toStrict(2.seconds).futureValue.data.utf8String
        body should include("DecodingFailure")
      }
    }

    "GET /transactions should return all transactions for authenticated user" in {
      val authHeader: Authorization = createAuthHeader
      val transactionRequest        = genTransactionRequest.sample.get
      Post("/transactions", transactionRequest.asJson).addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.Created
      }
      Get("/transactions").addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Seq[TransactionResponse]]
        response should have size 1
        response.head.amount shouldBe transactionRequest.amount.value
      }
    }

    "GET /transactions should return empty list for user with no transactions" in {
      val authHeader: Authorization = createAuthHeader
      Get("/transactions").addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Seq[TransactionResponse]]
        response shouldBe empty
      }
    }

    "GET /transactions/type/{type} should return transactions of specified type" in {
      val authHeader: Authorization = createAuthHeader
      val transactionRequest        = genTransactionRequest.sample.get.copy(`type` = TransactionType.INCOME)
      Post("/transactions", transactionRequest.asJson).addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.Created
      }
      Get("/transactions/type/INCOME").addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Seq[TransactionResponse]]
        response should have size 1
        response.head.`type` shouldBe TransactionType.INCOME
      }
    }

    "GET /transactions/type/{type} should fail with BadRequest for invalid type" in {
      val authHeader: Authorization = createAuthHeader
      Get("/transactions/type/InvalidType").addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[String] shouldBe s"InvalidType is not a member of Enum (${TransactionType.values.mkString(",")})"
      }
    }

    "GET /transactions/{id} should return TransactionResponse for existing transaction" in {
      val authHeader: Authorization = createAuthHeader
      val transactionRequest        = genTransactionRequest.sample.get
      val transactionResponse       =
        Post("/transactions", transactionRequest.asJson).addHeader(authHeader) ~> routes ~> check {
          responseAs[TransactionResponse]
        }
      Get(s"/transactions/${transactionResponse.id}").addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[TransactionResponse]
        response.id shouldBe transactionResponse.id
        response.amount shouldBe transactionRequest.amount.value
      }
    }

    "GET /transactions/{id} should fail with NotFound for non-existing transaction" in {
      val authHeader: Authorization = createAuthHeader
      Get(s"/transactions/${UUID.randomUUID()}").addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[String] should include("not found")
      }
    }

    "PUT /transactions/{id} should update transaction and return TransactionResponse" in {
      val authHeader: Authorization = createAuthHeader
      val transactionRequest        = genTransactionRequest.sample.get
      val transactionResponse       =
        Post("/transactions", transactionRequest.asJson).addHeader(authHeader) ~> routes ~> check {
          responseAs[TransactionResponse]
        }
      val updatedRequest            = genTransactionRequest.sample.get
      Put(s"/transactions/${transactionResponse.id}", updatedRequest.asJson)
        .addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[TransactionResponse]
        response.id shouldBe transactionResponse.id
        response.amount shouldBe updatedRequest.amount.value
        response.`type` shouldBe updatedRequest.`type`
      }
    }

    "PUT /transactions/{id} should fail with NotFound for non-existing transaction" in {
      val authHeader: Authorization = createAuthHeader
      val updatedRequest            = genTransactionRequest.sample.get
      Put(s"/transactions/${UUID.randomUUID()}", updatedRequest.asJson)
        .addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[String] should include("not found")
      }
    }

    "DELETE /transactions/{id} should delete transaction and return NoContent" in {
      val authHeader: Authorization = createAuthHeader
      val transactionResponse       =
        Post("/transactions", genTransactionRequest.sample.get.asJson).addHeader(authHeader) ~> routes ~> check {
          responseAs[TransactionResponse]
        }
      Delete(s"/transactions/${transactionResponse.id}").addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }

    "DELETE /transactions/{id} should fail with NotFound for non-existing transaction" in {
      val authHeader: Authorization = createAuthHeader
      Delete(s"/transactions/${UUID.randomUUID()}").addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[String] should include("not found")
      }
    }
  }
}
