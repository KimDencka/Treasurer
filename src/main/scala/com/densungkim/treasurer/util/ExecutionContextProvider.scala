package com.densungkim.treasurer.util

import com.densungkim.treasurer.config.ExecutionContextsConfig
import scala.concurrent.ExecutionContext
import java.util.concurrent.{ExecutorService, Executors}

object ExecutionContextProvider {
  def apply(config: ExecutionContextsConfig): ExecutionContextProvider = {
    val httpExecutor     = Executors.newFixedThreadPool(config.http.threads.value)
    val databaseExecutor = Executors.newFixedThreadPool(config.database.threads.value)
    val cpuExecutor      = Executors.newFixedThreadPool(config.cpu.threads.value)

    new ExecutionContextProvider(httpExecutor, databaseExecutor, cpuExecutor)
  }
}

class ExecutionContextProvider private (
  private val httpExecutor: ExecutorService,
  private val databaseExecutor: ExecutorService,
  private val cpuExecutor: ExecutorService,
) {
  val http: ExecutionContext     = ExecutionContext.fromExecutor(httpExecutor)
  val database: ExecutionContext = ExecutionContext.fromExecutor(databaseExecutor)
  val cpu: ExecutionContext      = ExecutionContext.fromExecutor(cpuExecutor)

  def shutdown(): Unit = {
    httpExecutor.shutdown()
    databaseExecutor.shutdown()
    cpuExecutor.shutdown()
  }
}
