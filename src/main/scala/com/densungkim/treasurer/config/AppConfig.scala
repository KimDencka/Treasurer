package com.densungkim.treasurer.config

import pureconfig.ConfigSource
import pureconfig.generic.auto._
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.pureconfig._ // DON'T REMOVE THIS LINE

final case class AppConfig(
  http: HttpConfig,
  postgres: PostgresConfig,
  executionContexts: ExecutionContextsConfig,
  jwt: JwtConfig,
) {

  /**
   * Returns all credentials. Used to replace credentials with *****.
   */
  private val credentials: Set[String] = Set(postgres.user, postgres.password, jwt.secret)

  override def toString: String = {
    def productToString(product: Product): String = {
      val className = product.productPrefix
      val fields    = product.productIterator
        .map {
          case value: Product => productToString(value)
          case value          => s"${if (credentials.contains(s"$value")) "*****" else value}"
        }
        .mkString(",")

      s"$className($fields)"
    }

    productToString(this)
  }

}
object AppConfig {
  import org.slf4j.Logger
  import org.slf4j.LoggerFactory
  import pureconfig.error.ConfigReaderFailures

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def loadConfig: AppConfig =
    ConfigSource.default.load[AppConfig] match {
      case Right(config)                      =>
        logger.info(s"AppConfig loaded successfully: $config")
        config
      case Left(errors: ConfigReaderFailures) =>
        val message = s"Failed to load ServiceConfig: ${errors.toList.map(_.description).mkString(", ")}"
        logger.error(message)
        throw new RuntimeException(message)
    }

}
