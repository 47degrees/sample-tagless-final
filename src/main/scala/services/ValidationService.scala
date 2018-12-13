package com.fortysevendeg.tagless.sample
package services

import cats.data._
import cats.effect.Sync
import cats.implicits._
import com.fortysevendeg.tagless.sample.models._
import org.slf4j.{Logger, LoggerFactory}

trait ValidationService[F[_]] {

  def validateUser(name: String, age: Int, gender: Char, heightAndWeight: String): F[ValidatedNel[ValidationError, UserInformation]]

}

object ValidationService {

  type ValidationResult[A] = ValidatedNel[ValidationError, A]

  private def validateAgeLogged[F[_]: Sync](logger: Logger)(age: Int): F[ValidationResult[Int]] =
    Either.cond[ValidationError, Int](age >= 18, age, NotAdultError).fold(
      e => Sync[F].delay {
        logger.error(s"Invalid age: $age")
        e.invalidNel
      },
      v => Sync[F].pure(v.validNel)
    )

  private def validateHeightWeightLogged[F[_]: Sync](logger: Logger)(source: String): F[ValidationResult[(Int, Int)]] =
    (for {
      ints <- Either.catchNonFatal(source.split('x').map(_.toInt)).leftMap(e => InvalidHeightAndWeight)
      dimensions <- Either.cond[ValidationError, (Int, Int)](ints.length == 2, (ints(0), ints(1)), InvalidHeightAndWeight)
    } yield dimensions).fold(
      e => Sync[F].delay {
        logger.error(s"Invalid height/weight format: $source")
        e.invalidNel
      },
      v => Sync[F].pure(v.validNel)
    )

  private def validateGenderLogged[F[_]: Sync](logger: Logger)(gender: Char): F[ValidationResult[Gender]] = {
    (gender match {
      case 'M' => Either.right(Male)
      case 'F' => Either.right(Female)
      case _ => Either.left(InvalidGenderError)
    }).fold(
      e => Sync[F].delay {
        logger.error(s"Invalid gender: $gender")
        e.invalidNel
      },
      v => Sync[F].pure(v.validNel)
    )
  }


  def build[F[_]](implicit S: Sync[F]): F[ValidationService[F]] =
    S.delay(LoggerFactory.getLogger("ValidationService")).map { logger =>
      new ValidationServiceImpl[F](logger)
    }

  class ValidationServiceImpl[F[_]: Sync](logger: Logger) extends ValidationService[F] {
    override def validateUser(name: String, age: Int, gender: Char, heightAndWeight: String): F[ValidationResult[UserInformation]] = {
      for {
        ageValidation <- validateAgeLogged(logger)(age)
        genderValidation <- validateGenderLogged(logger)(gender)
        dimensionsValidation <- validateHeightWeightLogged(logger)(heightAndWeight)
      } yield (ageValidation, dimensionsValidation, genderValidation)
        .mapN {
        case (validAge, (validHeight, validWeight), validGender) =>
          UserInformation(name, validAge, validGender, validHeight, validWeight)
      }
    }
  }
}