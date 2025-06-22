package com.densungkim.treasurer.service

import com.densungkim.treasurer.model.ErrorModels.UserNotFound
import com.densungkim.treasurer.model.user.{UserRequest, UserResponse}
import com.densungkim.treasurer.repository.UserRepository
import org.slf4j.LoggerFactory

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait UserService {
  def create(request: UserRequest): Future[UserResponse]
  def update(id: UUID, request: UserRequest): Future[UserResponse]
  def getById(id: UUID): Future[UserResponse]
  def getByUsername(username: String): Future[UserResponse]
  def delete(id: UUID): Future[Unit]
}

final class UserServiceImpl(
  repository: UserRepository,
  cryptoService: CryptoService,
)(implicit ec: ExecutionContext)
  extends UserService {
  private val logger = LoggerFactory.getLogger(getClass.getName)

  override def create(request: UserRequest): Future[UserResponse] =
    for {
      passwordHash <- cryptoService.hashPassword(request.password.value)
      user         <- repository.create(request.toDomain(passwordHash))
      _             = logger.info(s"User created: ${user.username}")
    } yield UserResponse(user.id, user.username, user.createdAt)

  override def update(id: UUID, request: UserRequest): Future[UserResponse] =
    for {
      passwordHash <- cryptoService.hashPassword(request.password.value)
      user         <- repository.getById(id).flatMap {
                        case Some(u) =>
                          Future.successful(u.copy(username = request.username.value, password = passwordHash))
                        case None    =>
                          logger.warn(s"Update failed: user '$id' not found")
                          Future.failed(UserNotFound(s"User with ID '$id' not found"))
                      }
      updated      <- repository.update(user).map {
                        case Some(u) => UserResponse(u.id, u.username, u.createdAt)
                        case None    =>
                          logger.error(s"Update failed unexpectedly for user $id")
                          throw UserNotFound(s"User with ID $id not found")
                      }
      _             = logger.info(s"User updated: ${updated.username}")
    } yield updated

  override def getById(id: UUID): Future[UserResponse] =
    repository.getById(id).flatMap {
      case Some(user) => Future.successful(UserResponse(user.id, user.username, user.createdAt))
      case None       =>
        logger.warn(s"Get failed: user $id not found")
        Future.failed(UserNotFound(s"User with ID $id not found"))
    }

  override def getByUsername(username: String): Future[UserResponse] =
    repository.getByUsername(username).flatMap {
      case Some(user) => Future.successful(UserResponse(user.id, user.username, user.createdAt))
      case None       =>
        logger.warn(s"Get failed: user $username not found")
        Future.failed(UserNotFound(s"User with username $username not found"))
    }

  override def delete(id: UUID): Future[Unit] =
    repository.delete(id).flatMap {
      case true  =>
        logger.info(s"User deleted: $id")
        Future.unit
      case false =>
        logger.warn(s"Delete failed: user $id not found")
        Future.failed(UserNotFound(s"User with ID $id not found"))
    }

}
