package com.fortysevendeg.tagless.sample
package services

import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.apply._
import com.fortysevendeg.Encryptor
import com.fortysevendeg.tagless.sample.models.EncryptionError
import org.slf4j.{Logger, LoggerFactory}

trait EncryptionService[F[_]] {
  def encryptInfo(info: String): F[Array[Byte]]
}
object EncryptionService {
  def apply[F[_]](logger: Logger)(implicit S: Sync[F]): EncryptionService[F] =
    new EncryptionService[F] {
      def encryptInfo(info: String): F[Array[Byte]] =
        S.handleErrorWith(S.delay(Encryptor.encryptString(info)))(ex =>
          S.delay(logger.error("EncryptionService", ex)) *> S.raiseError(EncryptionError))
    }

  def build[F[_]](implicit S: Sync[F]): F[EncryptionService[F]] =
    S.delay(LoggerFactory.getLogger("EncryptionService")).map(EncryptionService(_))
}
