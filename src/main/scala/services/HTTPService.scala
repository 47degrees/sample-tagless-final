package com.fortysevendeg.tagless.sample
package services

import cats.effect.Sync
import java.util.UUID
import cats.syntax.functor._
import cats.syntax.flatMap._
import com.fortysevendeg.http._
import org.slf4j.{Logger, LoggerFactory}

trait HTTPService[F[_]] {

  def loadInfoRequest(id: UUID, encryptedPreferences: Array[Byte]): F[HTTPUserResponse]

}
object HTTPService {
  def apply[F[_]](client: HTTPClient[F], logger: Logger)(implicit S: Sync[F]): HTTPService[F] =
    new HTTPService[F] {
      def loadInfoRequest(id: UUID, encryptedPreferences: Array[Byte]): F[HTTPUserResponse] = for{
        _ <- S.delay(logger.debug(s"Calling to the API with id $id"))
        response <- client.loadInfoRequest(HTTPUserRequest(id, encryptedPreferences))
        _ <- S.delay(logger.debug(s"Got response from the API with id $id"))
      } yield response
    }

  def build[F[_]](client: HTTPClient[F])(implicit S: Sync[F]) =
    S.delay(LoggerFactory.getLogger("EncryptionService")).map(HTTPService(client, _))
}