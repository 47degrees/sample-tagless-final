package com.fortysevendeg.tagless.sample
package services

import cats.effect.Async
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.functor._
import com.fortysevendeg.Encryptor
import com.fortysevendeg.tagless.sample.models.EncryptionError
import org.slf4j.LoggerFactory

trait EncryptionService[F[_]] {
  def encryptInfo(info: String): F[Array[Byte]]
}

object EncryptionService {

  def apply[F[_]](implicit A: Async[F]): F[EncryptionService[F]] =
    A.delay(LoggerFactory.getLogger("EncryptionService")).map { logger =>
      new EncryptionService[F] {
        def encryptInfo(info: String): F[Array[Byte]] =
          A.delay(Encryptor.encryptString(info)).handleErrorWith { error =>
            A.delay(logger.error("Error decrypting", error)) *> A.raiseError(EncryptionError)
          }
      }
    }

}
