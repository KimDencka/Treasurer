package com.densungkim.treasurer.generators

import com.densungkim.treasurer.model.user._
import org.scalacheck.Gen

trait UserGenerators extends CommonGenerators {

  def genUserRequest: Gen[UserRequest] =
    for {
      username <- genNonEmptyString
      password <- genNonEmptyString
    } yield UserRequest(username, password)

  def genUser: Gen[User] =
    for {
      id        <- Gen.uuid
      username  <- genString(15)
      password  <- genString(15).map(PasswordHash.apply)
      createdAt <- genLocalDateTime
    } yield User(id, username, password, createdAt)

  def genUserResponse: Gen[UserResponse] = genUser.map(_.toResponse)

}
