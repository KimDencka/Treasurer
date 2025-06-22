package com.densungkim.treasurer.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.densungkim.treasurer.http.auth.AuthRoutes
import com.densungkim.treasurer.http.transaction.TransactionRoutes
import com.densungkim.treasurer.http.user.UserRoutes

final class Routes(
  authRoutes: AuthRoutes,
  userRoutes: UserRoutes,
  transactionRoutes: TransactionRoutes,
) {
  val routes: Route = pathPrefix("api") {
    authRoutes.routes ~
      userRoutes.routes ~
      transactionRoutes.routes
  }
}
