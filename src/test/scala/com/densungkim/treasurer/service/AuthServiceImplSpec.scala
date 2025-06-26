package com.densungkim.treasurer.service

import com.densungkim.treasurer.TestUtils
import com.densungkim.treasurer.config.JwtConfig
import com.densungkim.treasurer.model.ErrorModels.{IncorrectPassword, UserNotFound}
import com.densungkim.treasurer.model.user.{AuthResponse, PasswordHash, User}
import com.densungkim.treasurer.repository.UserRepository
import eu.timepit.refined.types.string.NonEmptyString
import org.scalacheck.Gen

import java.util.UUID
import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class AuthServiceImplSpec extends TestUtils {
  private val testExecutor: ExecutorService = Executors.newFixedThreadPool(4)
  implicit private val ec: ExecutionContext = ExecutionContext.fromExecutor(testExecutor)

  override def afterAll(): Unit = testExecutor.shutdown()

  val mockCryptoService: CryptoService   = mock[CryptoService]
  val mockUserRepository: UserRepository = mock[UserRepository]

  val jwtConfig: JwtConfig       = JwtConfig(NonEmptyString.unsafeFrom("secret"), 60.minutes)
  val token: String              = "jwt-token"
  val passwordHash: PasswordHash = PasswordHash("password-hash")

  val authService: AuthService = new AuthServiceImpl(mockCryptoService, mockUserRepository, jwtConfig) {
    override def createToken(userId: UUID, config: JwtConfig): String = token
  }

  "AuthServiceImpl" should {

    "register a new user and return UserResponse" in {
      forAll(genUserRequest) { request =>
        reset(mockCryptoService, mockUserRepository)
        val user = request.toDomain(passwordHash)
        mockCryptoService.hashPassword(request.password.value) returns Future.successful(passwordHash)
        mockUserRepository.create(any[User]) returns Future.successful(user)

        whenReady(authService.register(request)) { response =>
          response shouldBe user.toResponse
          mockCryptoService.hashPassword(request.password.value) was called
          mockUserRepository.create(any[User]) was called
        }
      }
    }

    "login and return AuthResponse with token for valid credentials" in {
      forAll(genUserRequest) { request =>
        reset(mockCryptoService, mockUserRepository)
        val user = request.toDomain(passwordHash)
        mockUserRepository.getByUsername(request.username.value) returns Future.successful(Some(user))
        mockCryptoService.validate(request.password.value, user.password) returns Future.successful(true)

        whenReady(authService.login(request)) { response =>
          response shouldBe AuthResponse(token)
          mockUserRepository.getByUsername(request.username.value) was called
          mockCryptoService.validate(request.password.value, user.password) was called
        }
      }
    }

    "fail login with UserNotFound if user does not exist" in {
      forAll(genUserRequest) { request =>
        reset(mockCryptoService, mockUserRepository)
        mockUserRepository.getByUsername(request.username.value) returns Future.successful(None)

        whenReady(authService.login(request).failed) { response =>
          response shouldBe a[UserNotFound]
          val errorMessage = response.asInstanceOf[UserNotFound].message
          errorMessage shouldBe s"The user '${request.username}' not found."
          mockUserRepository.getByUsername(request.username.value) was called
        }
      }
    }

    "fail login with IncorrectPassword for invalid password" in {
      forAll(genUserRequest) { request =>
        val user = request.toDomain(passwordHash)
        mockUserRepository.getByUsername(request.username.value) returns Future.successful(Some(user))
        mockCryptoService.validate(request.password.value, user.password) returns Future.successful(false)

        whenReady(authService.login(request).failed) { response =>
          response shouldBe a[IncorrectPassword]
          val errorMessage = response.asInstanceOf[IncorrectPassword].message
          errorMessage shouldBe "The password is incorrect; Please, try again."
          mockUserRepository.getByUsername(request.username.value) was called
          mockCryptoService.validate(request.password.value, user.password) was called
        }
      }
    }

    "validateToken and return userId for valid token" in {
      forAll(Gen.uuid) { userId =>
        val authService: AuthService = new AuthServiceImpl(mockCryptoService, mockUserRepository, jwtConfig) {
          override def validateToken(token: String, config: JwtConfig): Option[UUID] = Some(userId)
        }
        authService.validateToken(token) shouldBe Some(userId)
      }
    }

    "validateToken and return None for invalid token" in {
      forAll(()) { _ =>
        val authService: AuthService = new AuthServiceImpl(mockCryptoService, mockUserRepository, jwtConfig) {
          override def validateToken(token: String, config: JwtConfig): Option[UUID] = None
        }
        authService.validateToken("invalid-token") shouldBe None
      }
    }

  }

}
