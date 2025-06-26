package com.densungkim.treasurer.http.transaction

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.densungkim.treasurer.http.auth.AuthDirectives
import com.densungkim.treasurer.http.rejectionHandler
import com.densungkim.treasurer.model.transaction.{TransactionRequest, TransactionType}
import com.densungkim.treasurer.service.{AuthService, TransactionService}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

import scala.util.{Failure, Success}

final class TransactionRoutes(
  transactionService: TransactionService,
  authService: AuthService,
) extends AuthDirectives {
  override def authService_ : AuthService = authService

  val routes: Route =
    handleRejections(rejectionHandler) {
      pathPrefix("transactions") {
        authenticate { userId =>
          pathEndOrSingleSlash {
            post {
              entity(as[TransactionRequest]) { request =>
                onComplete(transactionService.create(userId, request)) {
                  case Success(transactionResponse) =>
                    complete(StatusCodes.Created -> transactionResponse)
                  case Failure(e)                   =>
                    complete(StatusCodes.BadRequest -> e.getMessage)
                }
              }
            } ~
              get {
                onComplete(transactionService.getAll(userId)) {
                  case Success(transactions) =>
                    complete(StatusCodes.OK -> transactions)
                  case Failure(e)            =>
                    complete(StatusCodes.NotFound -> e.getMessage)
                }
              }
          } ~
            path("type" / Segment) { transactionTypeStr =>
              get {
                onComplete(transactionService.getByType(userId, TransactionType.withName(transactionTypeStr))) {
                  case Success(transactions) =>
                    complete(StatusCodes.OK -> transactions)
                  case Failure(e)            =>
                    complete(StatusCodes.BadRequest -> e.getMessage)
                }
              }
            } ~
            path(JavaUUID) { id =>
              get {
                onComplete(transactionService.getById(id, userId)) {
                  case Success(transactionResponse) =>
                    complete(StatusCodes.OK -> transactionResponse)
                  case Failure(e)                   =>
                    complete(StatusCodes.NotFound -> e.getMessage)
                }
              } ~
                put {
                  entity(as[TransactionRequest]) { request =>
                    onComplete(transactionService.update(id, userId, request)) {
                      case Success(transactionResponse) =>
                        complete(StatusCodes.OK -> transactionResponse)
                      case Failure(e)                   =>
                        complete(StatusCodes.NotFound -> e.getMessage)
                    }
                  }
                } ~
                delete {
                  onComplete(transactionService.delete(id, userId)) {
                    case Success(_) =>
                      complete(StatusCodes.NoContent)
                    case Failure(e) =>
                      complete(StatusCodes.NotFound -> e.getMessage)
                  }
                }
            }
        }
      }
    }
}
