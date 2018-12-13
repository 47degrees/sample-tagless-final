package com.fortysevendeg.tagless.sample
package services

import java.util.UUID

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.fortysevendeg.http.{HTTPClient, HTTPUserRequest, HTTPUserResponse}
import org.slf4j.{Logger, LoggerFactory}

trait HTTPService[F[_]] {

  def loadInfoRequest(id: UUID, encryptedPreferences: Array[Byte]): F[HTTPUserResponse]

}

object HTTPService {

  def build[F[_]: Sync](client: HTTPClient[F]) = Sync[F].delay(LoggerFactory.getLogger("HTTPService")).map { logger =>
    new HttpServiceImpl[F](client, logger)
  }


  class HttpServiceImpl[F[_]](client: HTTPClient[F], log: Logger)(implicit S: Sync[F]) extends HTTPService[F] {
    def loadInfoRequest(id: UUID, encryptedPreferences: Array[Byte]): F[HTTPUserResponse] = {
      for {
        _ <- S.delay(log.debug(s"Calling API with ID: $id"))
        response <- client.loadInfoRequest(HTTPUserRequest(id, encryptedPreferences))
        _ <- S.delay(log.debug(s"Received response with ID: $id"))
      } yield response
    }
  }

}