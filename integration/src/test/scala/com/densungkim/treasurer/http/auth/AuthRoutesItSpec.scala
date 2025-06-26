package com.densungkim.treasurer.http.auth

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.densungkim.treasurer.TestUtils
import com.densungkim.treasurer.config.JwtConfig
import com.densungkim.treasurer.http.TestConfig.database
import com.densungkim.treasurer.model.user.{AuthResponse, UserRequest, UserResponse}
import com.densungkim.treasurer.repository.UserRepositoryImpl
import com.densungkim.treasurer.service.{AuthServiceImpl, CryptoService}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Json
import io.circe.syntax._

import scala.concurrent.duration._

class AuthRoutesItSpec extends TestUtils with ScalatestRouteTest {

  val cryptoService: CryptoService       = new CryptoService()
  val userRepository: UserRepositoryImpl = new UserRepositoryImpl(database)
  val jwtConfig: JwtConfig               = JwtConfig(NonEmptyString.unsafeFrom("test-secret"), 60.minutes)
  val authService: AuthServiceImpl       = new AuthServiceImpl(cryptoService, userRepository, jwtConfig)
  val routes: Route                      = new AuthRoutes(authService).routes

  "AuthRoutes" should {

    "POST /auth/register should register a new user and return UserResponse" in {
      val request = genUserRequest.sample.get
      Post("/auth/register", request.asJson) ~> routes ~> check {
        status shouldBe StatusCodes.Created
        val response = responseAs[UserResponse]
        response.username shouldBe request.username.value
        response.id should not be null
        response.createdAt should not be null
      }
    }

    "POST /auth/register should fail with BadRequest for duplicate username" in {
      val request = genUserRequest.sample.get
      Post("/auth/register", request.asJson) ~> routes ~> check {
        status shouldBe StatusCodes.Created
      }

      Post("/auth/register", request.asJson) ~> routes ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[String] should include("already exists")
      }
    }

    "POST /auth/register should fail with BadRequest for invalid JSON" in {
      val invalidJson = Json.obj("username" -> "test".asJson)
      Post("/auth/register", invalidJson) ~> routes ~> check {
        status shouldBe StatusCodes.BadRequest
        val body = response.entity.toStrict(2.seconds).futureValue.data.utf8String
        body should include("DecodingFailure")
      }
    }

    "POST /auth/register should handle long username and password" in {
      val username = genStringNoShorterThan(50).sample.get
      val request  = UserRequest(
        username = NonEmptyString.unsafeFrom(username),
        password = NonEmptyString.unsafeFrom(genStringNoShorterThan(50).sample.get),
      )
      Post("/auth/register", request.asJson) ~> routes ~> check {
        status shouldBe StatusCodes.Created
        val response = responseAs[UserResponse]
        response.username shouldBe username
      }
    }

    "POST /auth/login should return AuthResponse with token for valid credentials" in {
      val request = genUserRequest.sample.get
      Post("/auth/register", request.asJson) ~> routes ~> check {
        status shouldBe StatusCodes.Created
      }

      Post("/auth/login", request.asJson) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        import io.circe.generic.auto._
        val response = responseAs[AuthResponse]
        response.token should not be empty

        authService.validateToken(response.token) shouldBe defined
      }
    }

    "POST /auth/login should fail with Unauthorized for incorrect password" in {
      val request = genUserRequest.sample.get
      Post("/auth/register", request.asJson) ~> routes ~> check {
        status shouldBe StatusCodes.Created
      }

      val wrongJsonRequest = request.copy(password = genNonEmptyString.sample.get)
      Post("/auth/login", wrongJsonRequest) ~> routes ~> check {
        status shouldBe StatusCodes.Unauthorized
        responseAs[String] shouldBe "The password is incorrect; Please, try again."
      }
    }

    "POST /auth/login should fail with Unauthorized for non-existing user" in {
      val request = genUserRequest.sample.get
      Post("/auth/login", request.asJson) ~> routes ~> check {
        status shouldBe StatusCodes.Unauthorized
        responseAs[String] shouldBe s"The user '${request.username}' not found."
      }
    }

    "POST /auth/login should fail with BadRequest for invalid JSON" in {
      val invalidJson = Json.obj("username" -> "test".asJson)
      Post("/auth/login", invalidJson) ~> routes ~> check {
        status shouldBe StatusCodes.BadRequest
        val body = response.entity.toStrict(2.seconds).futureValue.data.utf8String
        body should include("DecodingFailure")
      }
    }

  }

}
