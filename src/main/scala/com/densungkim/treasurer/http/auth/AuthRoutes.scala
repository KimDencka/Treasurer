package com.densungkim.treasurer.http.auth

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.densungkim.treasurer.model.user.UserRequest
import com.densungkim.treasurer.service.AuthService
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext

class AuthRoutes(authService: AuthService)(implicit ec: ExecutionContext) {
  val routes: Route =
    pathPrefix("auth") {
      post {
        path("register") {
          entity(as[UserRequest]) { request =>
            onComplete(authService.register(request)) {
              case scala.util.Success(userResponse) =>
                complete(StatusCodes.Created -> userResponse)
              case scala.util.Failure(e)            =>
                complete(StatusCodes.BadRequest -> e.getMessage)
            }
          }
        } ~
          path("login") {
            entity(as[UserRequest]) { request =>
              onComplete(authService.login(request)) {
                case scala.util.Success(authResponse) =>
                  complete(StatusCodes.OK -> authResponse)
                case scala.util.Failure(e)            =>
                  complete(StatusCodes.Unauthorized -> e.getMessage)
              }
            }
          }
      }
    }
}
