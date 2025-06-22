package com.densungkim.treasurer.service

import com.densungkim.treasurer.config.JwtConfig
import com.densungkim.treasurer.model.ErrorModels.{IncorrectPassword, UserNotFound}
import com.densungkim.treasurer.model.user.{AuthResponse, UserRequest, UserResponse}
import com.densungkim.treasurer.repository.UserRepository
import com.densungkim.treasurer.util.JwtUtil

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait AuthService {
  def register(request: UserRequest): Future[UserResponse]
  def login(request: UserRequest): Future[AuthResponse]
  def validateToken(token: String): Future[Option[UUID]]
}

final class AuthServiceImpl(
  cryptoService: CryptoService,
  userRepository: UserRepository,
  jwtConfig: JwtConfig,
)(implicit val ec: ExecutionContext)
  extends AuthService {

  override def register(request: UserRequest): Future[UserResponse] =
    for {
      passwordHash <- cryptoService.hashPassword(request.password.value)
      created      <- userRepository.create(request.toDomain(passwordHash))
    } yield created.toResponse

  override def login(request: UserRequest): Future[AuthResponse] =
    for {
      user    <- userRepository
                   .getByUsername(request.username.value)
                   .map(_.getOrElse(throw UserNotFound(s"The user '${request.username}'  not found.")))
      isValid <- cryptoService.validate(request.password.value, user.password)
      result   = if (isValid) {
                   AuthResponse(JwtUtil.createToken(user.id, jwtConfig))
                 } else {
                   throw IncorrectPassword("The password is incorrect; Please, try again.")
                 }
    } yield result

  override def validateToken(token: String): Future[Option[UUID]] =
    Future(JwtUtil.validateToken(token, jwtConfig))
}
