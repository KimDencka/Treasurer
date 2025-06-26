package com.densungkim.treasurer.http.user

import akka.http.javadsl.model.headers.HttpCredentials
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.densungkim.treasurer.TestUtils
import com.densungkim.treasurer.config.JwtConfig
import com.densungkim.treasurer.http.TestConfig.database
import com.densungkim.treasurer.model.user.{UserRequest, UserResponse}
import com.densungkim.treasurer.repository.UserRepositoryImpl
import com.densungkim.treasurer.service.{AuthServiceImpl, CryptoService, UserServiceImpl}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Json
import io.circe.syntax._

import java.util.UUID
import scala.concurrent.duration._

class UserRoutesItSpec extends TestUtils with ScalatestRouteTest {

  val cryptoService: CryptoService       = new CryptoService()
  val userRepository: UserRepositoryImpl = new UserRepositoryImpl(database)
  val jwtConfig: JwtConfig               = JwtConfig(NonEmptyString.unsafeFrom("test-secret"), 60.minutes)
  val authService: AuthServiceImpl       = new AuthServiceImpl(cryptoService, userRepository, jwtConfig)
  val userService: UserServiceImpl       = new UserServiceImpl(userRepository, cryptoService)
  val routes: Route                      = new UserRoutes(userService, authService).routes

  "UserRoutes" should {

    def createUserAndToken(request: UserRequest): (UserResponse, Authorization) = {
      val (user, token) = (
        for {
          user    <- authService.register(request)
          newToken = authService.createToken(user.id, jwtConfig)
        } yield (user, newToken)
      ).futureValue(timeout(1.seconds))
      (user, Authorization(HttpCredentials.createOAuth2BearerToken(token)))
    }

    "POST /users should fail with BadRequest for duplicate username" in {
      val request         = genUserRequest.sample.get
      val (_, authHeader) = createUserAndToken(request)
      Post("/users", request.asJson).addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[String] should include("already exists")
      }
    }

    "POST /users should fail with BadRequest for invalid JSON" in {
      val request         = genUserRequest.sample.get
      val (_, authHeader) = createUserAndToken(request)
      val invalidJson     = Json.obj("username" -> "test".asJson)
      Post("/users", invalidJson).addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.BadRequest
        val body = response.entity.toStrict(2.seconds).futureValue.data.utf8String
        body should include("DecodingFailure")
      }
    }

    "GET /users should return UserResponse for authenticated user" in {
      val request            = genUserRequest.sample.get
      val (user, authHeader) = createUserAndToken(request)
      Get("/users").addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[UserResponse]
        response.username shouldBe request.username.value
        response.id shouldBe user.id
      }
    }

    "GET /users/{username} should return UserResponse for existing username" in {
      val request            = genUserRequest.sample.get
      val (user, authHeader) = createUserAndToken(request)
      Get(s"/users/${request.username.value}").addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[UserResponse]
        response.username shouldBe request.username.value
        response.id shouldBe user.id
      }
    }

    "GET /users/{username} should fail with NotFound for non-existing username" in {
      val request         = genUserRequest.sample.get
      val (_, authHeader) = createUserAndToken(request)
      Get("/users/nonexistent").addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[String] should include("not found")
      }
    }

    "PUT /users/{id} should update user and return UserResponse" in {
      val request            = genUserRequest.sample.get
      val (user, authHeader) = createUserAndToken(request)
      val updatedRequest     = genUserRequest.sample.get
      Put(s"/users/${user.id}", updatedRequest.asJson).addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[UserResponse]
        response.username shouldBe updatedRequest.username.value
        response.id shouldBe user.id
      }
    }

    "PUT /users/{id} should fail with NotFound for non-existing user" in {
      val request         = genUserRequest.sample.get
      val updatedRequest  = genUserRequest.sample.get
      val (_, authHeader) = createUserAndToken(request)
      Put(s"/users/${UUID.randomUUID()}", updatedRequest.asJson).addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[String] should include("not found")
      }
    }

    "DELETE /users/{id} should delete user and return NoContent" in {
      val request            = genUserRequest.sample.get
      val (user, authHeader) = createUserAndToken(request)
      Delete(s"/users/${user.id}").addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.NoContent
      }
    }

    "DELETE /users/{id} should fail with NotFound for non-existing user" in {
      val request         = genUserRequest.sample.get
      val (_, authHeader) = createUserAndToken(request)
      Delete(s"/users/${UUID.randomUUID()}").addHeader(authHeader) ~> routes ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[String] should include("not found")
      }
    }
  }
}
