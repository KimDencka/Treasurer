package com.densungkim.treasurer

import com.densungkim.treasurer.generators.UserGenerators
import org.mockito.scalatest.IdiomaticMockito
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.ExecutionContext

trait TestUtils
  extends AnyWordSpec
  with UserGenerators
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures
  with IdiomaticMockito
  with ScalaCheckDrivenPropertyChecks {

  private val testExecutor: ExecutorService = Executors.newFixedThreadPool(4)
  implicit val ec: ExecutionContext         = ExecutionContext.fromExecutor(testExecutor)

  override def afterAll(): Unit =
    testExecutor.shutdown()

}
