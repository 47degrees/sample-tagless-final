package com.fortysevendeg.tagless.sample
package services

import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import com.fortysevendeg.Encryptor
import com.fortysevendeg.tagless.sample.models.EncryptionError
import org.slf4j.{Logger, LoggerFactory}

trait EncryptionService[F[_]] {
  def encryptInfo(info: String): F[Array[Byte]]
}

object EncryptionService {

  def build[F[_]](implicit S: Sync[F]): F[EncryptionService[F]] =
    S.delay(LoggerFactory.getLogger("EncryptionService")).map { logger =>
      new EncryptionServiceImpl[F](logger)
    }


  class EncryptionServiceImpl[F[_]](log: Logger)(implicit S: Sync[F]) extends EncryptionService[F] {
    def encryptInfo(info: String): F[Array[Byte]] =
      S.delay(Encryptor.encryptString(info))
        .handleErrorWith { e =>
          S.delay(log.error("Error decrypting data"))
            .flatMap(_ => S.raiseError(EncryptionError))
        }
  }
}