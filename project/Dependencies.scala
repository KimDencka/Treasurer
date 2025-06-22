import sbt.*

object Dependencies {

  private object Versions {
    val akka          = "2.8.8"
    val akkaHttp      = "10.5.3"
    val circe         = "0.14.14"
    val chimney       = "1.8.1"
    val doobie        = "1.0.0-RC9"
    val enumeratum    = "1.9.0"
    val flyway        = "11.9.2"
    val logback       = "1.5.18"
    val mockito       = "2.0.0"
    val pureconfig    = "0.17.9"
    val postgresql    = "42.7.7"
    val refined       = "0.11.3"
    val scalatest     = "3.2.19"
    val scalatestPlus = "3.2.19.0"
    val slf4j         = "2.0.17"
  }

  private object Libraries {
    // Akka dependencies
    def akka(artifact: String, version: String = Versions.akka): ModuleID =
      "com.typesafe.akka" %% s"akka-$artifact" % version

    val akkaStreams     = akka("stream")
    val akkaSlf4j       = akka("slf4j")
    val akkaHttp        = akka("http", Versions.akkaHttp)
    val akkaHttpTestkit = akka("http-testkit", Versions.akkaHttp) % Test

    // Circe dependencies
    def circe(artifact: String, version: String = Versions.circe): ModuleID =
      "io.circe" %% s"circe-$artifact" % version

    val circeCore    = circe("core")
    val circeGeneric = circe("generic")
    val circeParser  = circe("parser")

    // Doobie dependencies
    def doobie(artifact: String, version: String = Versions.doobie): ModuleID =
      "org.tpolecat" %% s"doobie-$artifact" % version

    val doobieCore     = doobie("core")
    val doobiePostgres = doobie("postgres")
    val doobieHikari   = doobie("hikari")

    // Chimney dependency (for transformation)
    val chimney = "io.scalaland" %% "chimney" % Versions.chimney

    // Enumeratum dependencies (for enums)
    val enumeratum      = "com.beachape" %% "enumeratum"       % Versions.enumeratum
    val enumeratumCirce = "com.beachape" %% "enumeratum-circe" % Versions.enumeratum

    // Flyway dependency (for database migration)
    val flyway         = "org.flywaydb" % "flyway-core"                % Versions.flyway
    val flywayPostgres = "org.flywaydb" % "flyway-database-postgresql" % Versions.flyway % Runtime

    // Refined dependencies (for input validation)
    val refined           = "eu.timepit" %% "refined"            % Versions.refined
    val refinedPureconfig = "eu.timepit" %% "refined-pureconfig" % Versions.refined

    // PureConfig dependency (for configuration management)
    val pureconfig = "com.github.pureconfig" %% "pureconfig" % Versions.pureconfig

    // PostgreSQL dependency (driver for PostgreSQL)
    val postgresql = "org.postgresql" % "postgresql" % Versions.postgresql

    // Logging dependencies (SLF4J + Logback)
    val logback  = "ch.qos.logback" % "logback-classic" % Versions.logback
    val slf4jApi = "org.slf4j"      % "slf4j-api"       % Versions.slf4j

    // Testing dependencies
    val scalatest     = "org.scalatest"     %% "scalatest"               % Versions.scalatest     % Test
    val scalatestPlus = "org.scalatestplus" %% "scalacheck-1-18"         % Versions.scalatestPlus % Test
    val mockito       = "org.mockito"       %% "mockito-scala-scalatest" % Versions.mockito       % Test
  }

  private val akka: Seq[ModuleID] = Seq(
    Libraries.akkaStreams,
    Libraries.akkaSlf4j,
    Libraries.akkaHttp
  )

  private val circe: Seq[ModuleID] = Seq(
    Libraries.circeCore,
    Libraries.circeGeneric,
    Libraries.circeParser
  )

  private val doobie: Seq[ModuleID] = Seq(
    Libraries.doobieCore,
    Libraries.doobiePostgres,
    Libraries.doobieHikari
  )

  private val enumeratum: Seq[ModuleID] = Seq(
    Libraries.enumeratum,
    Libraries.enumeratumCirce
  )

  private val refined: Seq[ModuleID] = Seq(
    Libraries.refined,
    Libraries.refinedPureconfig
  )

  private val logging: Seq[ModuleID] = Seq(
    Libraries.logback,
    Libraries.slf4jApi
  )

  private val testing: Seq[ModuleID] = Seq(
    Libraries.akkaHttpTestkit,
    Libraries.scalatest,
    Libraries.scalatestPlus,
    Libraries.mockito
  )

  val librariesDependencies: Seq[ModuleID] = Seq(
    Libraries.chimney,
    Libraries.flyway,
    Libraries.flywayPostgres,
    Libraries.pureconfig,
    Libraries.postgresql
  ) ++ akka ++ circe ++ doobie ++ enumeratum ++ refined ++ logging ++ testing

}
