package com.fortysevendeg.tagless.sample
package services

import cats.data.{ValidatedNec, ValidatedNel}
import cats.effect.Sync
import cats.implicits._
import com.fortysevendeg.tagless.sample.models._
import org.slf4j.{Logger, LoggerFactory}

trait ValidationService[F[_]] {

  def validateUser(name: String, age: Int, gender: Char, heightAndWeight: String): F[ValidatedNel[ValidationError, UserInformation]]

}

object ValidationService {

  type ValidationResult[A] = ValidatedNec[ValidationError, A]

  private def toGender(gender: Char): Either[ValidationError, Gender] = gender match {
    case 'M' => Either.right(Male)
    case 'F' => Either.right(Female)
    case _ => Either.left(InvalidGenderError)
  }

  def validateAgeLogged[F[_]: Sync](age: Int)(logger: Logger): F[ValidationResult[Int]] = Sync[F].delay {
    Either.cond[ValidationError, Int](age >= 18, age, NotAdultError)
      .toValidatedNec
  }.handleErrorWith { _ =>
    Sync[F].delay(logger.error("Invalid date found"))
      .flatMap(_ => Sync[F].raiseError(EncryptionError))
  }

  def validateHeightWeight[F[_]: Sync](source: String)(logger: Logger): F[ValidationResult[(Int, Int)]] = Sync[F].delay {
    (for {
      ints <- Either.catchNonFatal(source.split('x').map(_.toInt)).leftMap(e => InvalidHeightAndWeight)
      dimensions <- Either.cond[ValidationError, (Int, Int)](ints.length == 2, (ints(0), ints(1)), InvalidHeightAndWeight)
    } yield dimensions)
      .toValidatedNec
  }.handleErrorWith { _ =>
    Sync[F].delay(logger.error("Invalid height/weight format"))
      .flatMap(_ => Sync[F].raiseError(EncryptionError))
  }

  def validateGenderLogged[F[_]: Sync](gender: Char)(logger: Logger): F[ValidationResult[Gender]] = Sync[F].delay {
    toGender(gender)
      .toValidatedNec
  }.handleErrorWith { _ =>
    Sync[F].delay(logger.error("Invalid gender found"))
      .flatMap(_ => Sync[F].raiseError(EncryptionError))
  }

  def build[F[_]](implicit S: Sync[F]): F[ValidationService[F]] =
    S.delay(LoggerFactory.getLogger("ValidationService")).map { logger =>
      new ValidationServiceImpl[F](logger)
    }

  class ValidationServiceImpl[F[_]: Sync](logger: Logger) extends ValidationService[F] {
    override def validateUser(name: String, age: Int, gender: Char, heightAndWeight: String): F[ValidatedNel[ValidationError, UserInformation]] =
      for {
          ageValidation <- validateAgeLogged(age)(logger)
          dimensionsValidation <- validateHeightWeight(heightAndWeight)(logger)
          genderValidation <- validateGenderLogged(gender)(logger)
        } yield {
          (ageValidation, dimensionsValidation, genderValidation).mapN { case (validAge, (validHeight, validWeight), validGender) =>
            UserInformation(name, validAge, validGender, validHeight, validWeight)
          }
            .leftMap(_.toNonEmptyList)
        }
      }
}