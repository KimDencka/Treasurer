package com.densungkim.treasurer.http.user

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.densungkim.treasurer.http.auth.AuthDirectives
import com.densungkim.treasurer.model.user.UserRequest
import com.densungkim.treasurer.service.{AuthService, UserService}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

import scala.util.{Failure, Success}

final class UserRoutes(
  userService: UserService,
  authService: AuthService,
) extends AuthDirectives {
  override def authService_ : AuthService = authService

  val routes: Route =
    pathPrefix("users") {
      authenticate { userId =>
        pathEndOrSingleSlash {
          post {
            entity(as[UserRequest]) { request =>
              onComplete(userService.create(request)) {
                case Success(userResponse) =>
                  complete(StatusCodes.Created -> userResponse)
                case Failure(e)            =>
                  complete(StatusCodes.BadRequest -> e.getMessage)
              }
            }
          } ~
            get {
              onComplete(userService.getById(userId)) {
                case Success(userResponse) =>
                  complete(StatusCodes.OK -> userResponse)
                case Failure(e)            =>
                  complete(StatusCodes.NotFound -> e.getMessage)
              }
            }
        }
      } ~
        path(Segment) { username =>
          get {
            onComplete(userService.getByUsername(username)) {
              case Success(userResponse) =>
                complete(StatusCodes.OK -> userResponse)
              case Failure(e)            =>
                complete(StatusCodes.NotFound -> e.getMessage)
            }
          }
        }
    } ~
      path(JavaUUID) { id =>
        put {
          entity(as[UserRequest]) { request =>
            onComplete(userService.update(id, request)) {
              case Success(userResponse) =>
                complete(StatusCodes.OK -> userResponse)
              case Failure(e)            =>
                complete(StatusCodes.NotFound -> e.getMessage)
            }
          }
        } ~
          delete {
            onComplete(userService.delete(id)) {
              case Success(_) =>
                complete(StatusCodes.NoContent)
              case Failure(e) =>
                complete(StatusCodes.NotFound -> e.getMessage)
            }
          }
      }
}
