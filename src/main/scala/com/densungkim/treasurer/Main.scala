package com.densungkim.treasurer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.densungkim.treasurer.config.AppConfig
import com.densungkim.treasurer.http.Routes
import com.densungkim.treasurer.http.auth.AuthRoutes
import com.densungkim.treasurer.http.transaction.TransactionRoutes
import com.densungkim.treasurer.http.user.UserRoutes
import com.densungkim.treasurer.repository.{TransactionRepositoryImpl, UserRepositoryImpl}
import com.densungkim.treasurer.service.{AuthServiceImpl, CryptoService, TransactionServiceImpl, UserServiceImpl}
import com.densungkim.treasurer.util.{ExecutionContextProvider, FlywayMigrator}
import org.slf4j.LoggerFactory
import slick.jdbc.PostgresProfile.api._

import scala.util.{Failure, Success}

object Main extends App {
  private val logger     = LoggerFactory.getLogger(getClass.getName)
  private val config     = AppConfig.loadConfig
  private val ecProvider = ExecutionContextProvider(config.executionContexts)

  private val database = Database.forURL(
    url = config.postgres.jdbcUrl.value,
    user = config.postgres.user.value,
    password = config.postgres.password.value,
    driver = config.postgres.driver.value,
    executor = AsyncExecutor(
      name = "slick-postgres-executor",
      minThreads = config.postgres.connections.poolSize.value,
      maxThreads = config.postgres.connections.poolSize.value,
      maxConnections = config.postgres.connections.poolSize.value,
      queueSize = 1000,
    ),
  )

  FlywayMigrator.migrateDatabase(config.postgres) match {
    case Success(_) => logger.info("Database migration completed successfully")
    case Failure(e) =>
      logger.error("Failed to migrate database", e)
      database.close()
      ecProvider.shutdown()
      sys.exit(1)
  }

  private val userRepository        = new UserRepositoryImpl(database)(ecProvider.database)
  private val transactionRepository = new TransactionRepositoryImpl(database)(ecProvider.database)

  private val cryptoService      = new CryptoService()(ecProvider.cpu)
  private val authService        = new AuthServiceImpl(cryptoService, userRepository, config.jwt)(ecProvider.database)
  private val userService        = new UserServiceImpl(userRepository, cryptoService)(ecProvider.database)
  private val transactionService = new TransactionServiceImpl(transactionRepository)(ecProvider.database)

  private val authRoutes        = new AuthRoutes(authService)
  private val userRoutes        = new UserRoutes(userService, authService)
  private val transactionRoutes = new TransactionRoutes(transactionService, authService)

  private val routes = new Routes(authRoutes, userRoutes, transactionRoutes).routes

  implicit private val system: ActorSystem = ActorSystem("TreasurerSystem")

  private val bindingFuture = Http().newServerAt(config.http.host.value, config.http.port.value).bind(routes)

  bindingFuture.onComplete {
    case Success(binding) =>
      logger.info(s"Server started at http://${binding.localAddress.getHostString}:${binding.localAddress.getPort}")
    case Failure(e)       =>
      logger.error("Failed to start server", e)
      database.close()
      ecProvider.shutdown()
  }(ecProvider.http)

}
