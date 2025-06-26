package com.densungkim.treasurer.http.auth

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.densungkim.treasurer.http.rejectionHandler
import com.densungkim.treasurer.model.ErrorModels.{IncorrectPassword, UserNotFound}
import com.densungkim.treasurer.model.user.UserRequest
import com.densungkim.treasurer.service.AuthService
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.util.{Failure, Success}

final class AuthRoutes(authService: AuthService) {

  val routes: Route =
    pathPrefix("auth") {
      handleRejections(rejectionHandler) {
        post {
          path("register") {
            entity(as[UserRequest]) { request =>
              onComplete(authService.register(request)) {
                case Success(userResponse) =>
                  complete(StatusCodes.Created -> userResponse)
                case Failure(e)            =>
                  complete(StatusCodes.BadRequest -> e.getMessage)
              }
            }
          } ~
            path("login") {
              entity(as[UserRequest]) { request =>
                onComplete(authService.login(request)) {
                  case Success(authResponse)         =>
                    complete(StatusCodes.OK -> authResponse)
                  case Failure(e: IncorrectPassword) =>
                    complete(StatusCodes.Unauthorized -> e.message)
                  case Failure(e: UserNotFound)      =>
                    complete(StatusCodes.Unauthorized -> e.message)
                  case Failure(e)                    =>
                    complete(StatusCodes.InternalServerError -> e.getMessage)
                }
              }
            }
        }
      }
    }
}
