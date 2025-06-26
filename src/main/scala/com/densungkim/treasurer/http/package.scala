package com.densungkim.treasurer

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.{MalformedRequestContentRejection, RejectionHandler}

package object http {
  implicit def rejectionHandler: RejectionHandler =
    RejectionHandler
      .newBuilder()
      .handle { case MalformedRequestContentRejection(msg, _) =>
        complete(StatusCodes.BadRequest -> s"Invalid JSON: $msg")
      }
      .result()
}
