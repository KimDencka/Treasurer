package com.densungkim.treasurer.service

import com.densungkim.treasurer.TestUtils
import com.densungkim.treasurer.model.ErrorModels.{CryptoException, UserNotFound}
import com.densungkim.treasurer.model.user.{PasswordHash, User}
import com.densungkim.treasurer.repository.UserRepository
import org.scalacheck.Gen

import scala.concurrent.Future

class UserServiceImplSpec extends TestUtils {
  override def afterAll(): Unit = testExecutor.shutdown()

  val mockCryptoService: CryptoService   = mock[CryptoService]
  val mockUserRepository: UserRepository = mock[UserRepository]
  val passwordHash: PasswordHash         = PasswordHash("password-hash")

  val userService: UserService = new UserServiceImpl(mockUserRepository, mockCryptoService)

  "UserServiceImpl" should {

    "create a new user and return UserResponse" in {
      forAll(genUserRequest) { request =>
        reset(mockCryptoService, mockUserRepository)
        val user = request.toDomain(passwordHash)
        mockCryptoService.hashPassword(request.password.value) returns Future.successful(passwordHash)
        mockUserRepository.create(any[User]) returns Future.successful(user)

        whenReady(userService.create(request)) { response =>
          response shouldBe user.toResponse
          mockCryptoService.hashPassword(request.password.value) was called
          mockUserRepository.create(any[User]) was called
        }
      }
    }

    "fail create with CryptoException if hashPassword fails" in {
      forAll(genUserRequest) { request =>
        reset(mockCryptoService, mockUserRepository)
        val exception = CryptoException("Failed to hash password")
        mockCryptoService.hashPassword(request.password.value) returns Future.failed(exception)

        whenReady(userService.create(request).failed) { response =>
          response shouldBe exception
          mockCryptoService.hashPassword(request.password.value) was called
        }
      }
    }

    "update an existing user and return UserResponse" in {
      forAll(genUserRequest, Gen.uuid) { (request, userId) =>
        reset(mockCryptoService, mockUserRepository)
        val existingUser = User(userId, "old-username", passwordHash, genLocalDateTime.sample.get)
        val updatedUser  = existingUser.copy(username = request.username.value, password = passwordHash)
        mockCryptoService.hashPassword(request.password.value) returns Future.successful(passwordHash)
        mockUserRepository.getById(userId) returns Future.successful(Some(existingUser))
        mockUserRepository.update(updatedUser) returns Future.successful(Some(updatedUser))

        whenReady(userService.update(userId, request)) { response =>
          response shouldBe updatedUser.toResponse
          mockCryptoService.hashPassword(request.password.value) was called
          mockUserRepository.getById(userId) was called
          mockUserRepository.update(updatedUser) was called
        }
      }
    }

    "fail update with UserNotFound if user does not exist" in {
      forAll(genUserRequest, Gen.uuid) { (request, userId) =>
        reset(mockCryptoService, mockUserRepository)
        mockCryptoService.hashPassword(request.password.value) returns Future.successful(passwordHash)
        mockUserRepository.getById(userId) returns Future.successful(None)

        whenReady(userService.update(userId, request).failed) { response =>
          response shouldBe a[UserNotFound]
          response.asInstanceOf[UserNotFound].message shouldBe s"User with ID '$userId' not found"
          mockCryptoService.hashPassword(request.password.value) was called
          mockUserRepository.getById(userId) was called
        }
      }
    }

    "fail update with CryptoException if hashPassword fails" in {
      forAll(genUserRequest, Gen.uuid) { (request, userId) =>
        reset(mockCryptoService, mockUserRepository)
        val exception = CryptoException("Failed to hash password")
        mockCryptoService.hashPassword(request.password.value) returns Future.failed(exception)

        whenReady(userService.update(userId, request).failed) { response =>
          response shouldBe exception
          mockCryptoService.hashPassword(request.password.value) was called
        }
      }
    }

    "getById returns UserResponse for existing user" in {
      forAll(genUser) { user =>
        reset(mockUserRepository)
        mockUserRepository.getById(user.id) returns Future.successful(Some(user))

        whenReady(userService.getById(user.id)) { response =>
          response shouldBe user.toResponse
          mockUserRepository.getById(user.id) was called
        }
      }
    }

    "fail getById with UserNotFound if user does not exist" in {
      forAll(Gen.uuid) { userId =>
        reset(mockUserRepository)
        mockUserRepository.getById(userId) returns Future.successful(None)

        whenReady(userService.getById(userId).failed) { response =>
          response shouldBe a[UserNotFound]
          response.asInstanceOf[UserNotFound].message shouldBe s"User with ID '$userId' not found"
          mockUserRepository.getById(userId) was called
        }
      }
    }

    "getByUsername returns UserResponse for existing user" in {
      forAll(genUser) { user =>
        reset(mockUserRepository)
        mockUserRepository.getByUsername(user.username) returns Future.successful(Some(user))

        whenReady(userService.getByUsername(user.username)) { response =>
          response shouldBe user.toResponse
          mockUserRepository.getByUsername(user.username) was called
        }
      }
    }

    "fail getByUsername with UserNotFound if user does not exist" in {
      forAll(genShortString) { username =>
        reset(mockUserRepository)
        mockUserRepository.getByUsername(username) returns Future.successful(None)

        whenReady(userService.getByUsername(username).failed) { response =>
          response shouldBe a[UserNotFound]
          response.asInstanceOf[UserNotFound].message shouldBe s"User with username $username not found"
          mockUserRepository.getByUsername(username) was called
        }
      }
    }

    "delete an existing user" in {
      forAll(Gen.uuid) { userId =>
        reset(mockUserRepository)
        mockUserRepository.delete(userId) returns Future.successful(true)

        whenReady(userService.delete(userId)) { response =>
          response shouldBe ()
          mockUserRepository.delete(userId) was called
        }
      }
    }

    "fail delete with UserNotFound if user does not exist" in {
      forAll(Gen.uuid) { userId =>
        reset(mockUserRepository)
        mockUserRepository.delete(userId) returns Future.successful(false)

        whenReady(userService.delete(userId).failed) { response =>
          response shouldBe a[UserNotFound]
          response.asInstanceOf[UserNotFound].message shouldBe s"User with ID '$userId' not found"
          mockUserRepository.delete(userId) was called
        }
      }
    }

  }

}
