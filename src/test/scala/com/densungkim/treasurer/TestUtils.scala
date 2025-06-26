package com.densungkim.treasurer

import com.densungkim.treasurer.generators.{TransactionGenerators, UserGenerators}
import org.mockito.scalatest.IdiomaticMockito
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

trait TestUtils
  extends AnyWordSpec
  with UserGenerators
  with TransactionGenerators
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures
  with IdiomaticMockito
  with ScalaCheckDrivenPropertyChecks
