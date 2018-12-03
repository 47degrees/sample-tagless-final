package com.fortysevendeg.tagless.sample
package services

import java.util.UUID

import cats.effect.Async
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.fortysevendeg.http.{HTTPClient, HTTPUserRequest, HTTPUserResponse}
import org.slf4j.LoggerFactory

trait HTTPService[F[_]] {

  def loadInfoRequest(id: UUID, encryptedPreferences: Array[Byte]): F[HTTPUserResponse]

}

object HTTPService {

  def apply[F[_]](client: HTTPClient[F])(implicit A: Async[F]): F[HTTPService[F]] =
    A.delay(LoggerFactory.getLogger("HTTPService")).map { logger =>
      new HTTPService[F] {
        def loadInfoRequest(id: UUID, encryptedPreferences: Array[Byte]): F[HTTPUserResponse] =
          for {
            _        <- A.delay(logger.info(s"HTTP request for id $id"))
            response <- client.loadInfoRequest(HTTPUserRequest(id, encryptedPreferences))
            _        <- A.delay(logger.info(s"Received an HTTP response for id $id"))
          } yield response
      }
    }

}
