package com.densungkim.treasurer.http.auth

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import com.densungkim.treasurer.service.AuthService

import java.util.UUID
import scala.concurrent.ExecutionContext

trait AuthDirectives {
  def authService: AuthService
  implicit def ec: ExecutionContext

  def authenticate: Directive1[UUID] =
    headerValueByName("Authorization").flatMap { authHeader =>
      authHeader.stripPrefix("Bearer ") match {
        case token if token.nonEmpty =>
          onSuccess(authService.validateToken(token)) {
            case Some(userId) => provide(userId)
            case None         => complete(StatusCodes.Unauthorized -> "Invalid or expired token")
          }
        case _                       => complete(StatusCodes.Unauthorized -> "Missing or invalid Bearer token")
      }
    }
}
