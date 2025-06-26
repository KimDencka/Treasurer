package com.densungkim.treasurer

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.ExecutionContext

package object service {
  val testExecutor: ExecutorService = Executors.newFixedThreadPool(4)
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(testExecutor)
}
