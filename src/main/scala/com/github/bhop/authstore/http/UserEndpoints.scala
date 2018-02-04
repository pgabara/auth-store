package com.github.bhop.authstore.http

import cats.effect.Effect
import cats.implicits._
import com.github.bhop.authstore.model.User
import com.github.bhop.authstore.repository.UserRepository
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedService, EntityDecoder}

class UserEndpoints[F[_]: Effect](userRepository: UserRepository[F]) extends Http4sDsl[F] {

  implicit val userDecoder: EntityDecoder[F, User] = jsonOf[F, User]

  def getUserEndpoint: AuthedService[String, F] =
    AuthedService[String, F] {
      case GET -> Root / "users" / LongVar(id) as _ =>
        userRepository.get(id).flatMap {
          case Some(u) => Ok(u.asJson)
          case None    => NotFound(s"User with id: $id not found")
        }
    }

  def postUserEndpoint: AuthedService[String, F] =
    AuthedService[String, F] {
      case req @ POST -> Root / "users" as _ =>
        for {
          user   <- req.req.as[User]
          result <- userRepository.put(user)
          resp   <- Created(result.asJson)
        } yield resp
    }

  def deleteUserEndpoint: AuthedService[String, F] =
    AuthedService[String, F] {
      case DELETE -> Root / "users" / LongVar(id) as _ =>
        userRepository.delete(id).flatMap {
          case Some(_) => Ok(s"User with id: $id deleted")
          case None    => NotFound(s"User with id: $id not found")
        }
    }

  def endpoints: AuthedService[String, F] =
    getUserEndpoint <+> postUserEndpoint <+> deleteUserEndpoint
}

object UserEndpoints {

  def endpoints[F[_]: Effect](userRepository: UserRepository[F]): AuthedService[String, F] =
    new UserEndpoints[F](userRepository).endpoints
}
