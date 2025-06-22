package com.densungkim.treasurer.http.auth

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import com.densungkim.treasurer.service.AuthService

import java.util.UUID

trait AuthDirectives {
  def authService_ : AuthService

  def authenticate: Directive1[UUID] =
    extract(_.request.headers.find(_.name == "Authorization")).flatMap {
      case Some(authHeader) =>
        authHeader.value.stripPrefix("Bearer ") match {
          case token if token.nonEmpty =>
            authService_.validateToken(token) match {
              case Some(userId) => provide(userId)
              case None         => complete(StatusCodes.Unauthorized -> "Invalid or expired token")
            }
          case _                       => complete(StatusCodes.Unauthorized -> "Missing or invalid Bearer token")
        }
      case None             => complete(StatusCodes.Unauthorized -> "Missing Authorization header")
    }
}
