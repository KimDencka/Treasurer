package com.densungkim.treasurer.repository

import com.densungkim.treasurer.model.user.User
import slick.jdbc.PostgresProfile.api._

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait UserRepository {
  def create(user: User): Future[User]
  def update(user: User): Future[Option[User]]
  def getById(id: UUID): Future[Option[User]]
  def getByUsername(username: String): Future[Option[User]]
  def delete(id: UUID): Future[Boolean]
}

final class UserRepositoryImpl(db: Database)(implicit ec: ExecutionContext) extends UserRepository {
  private class UsersTable(tag: Tag) extends Table[User](tag, "users") {
    def id           = column[UUID]("id", O.PrimaryKey)
    def username     = column[String]("username", O.Unique)
    def passwordHash = column[String]("password_hash")
    def createdAt    = column[LocalDateTime]("created_at")
    def *            = (id, username, passwordHash, createdAt).mapTo[User]
  }

  private val users = TableQuery[UsersTable]

  override def create(user: User): Future[User] =
    db.run(users += user).map(_ => user)

  override def update(user: User): Future[Option[User]] =
    db.run {
      users
        .filter(_.id === user.id)
        .map(u => (u.username, u.passwordHash, u.createdAt))
        .update((user.username, user.password.value, user.createdAt))
        .map {
          case 0 => None
          case _ => Some(user)
        }
    }

  override def getById(id: UUID): Future[Option[User]] =
    db.run(users.filter(_.id === id).result.headOption)

  override def getByUsername(username: String): Future[Option[User]] =
    db.run(users.filter(_.username === username).result.headOption)

  override def delete(id: UUID): Future[Boolean] =
    db.run(users.filter(_.id === id).delete).map(_ > 0)

}
