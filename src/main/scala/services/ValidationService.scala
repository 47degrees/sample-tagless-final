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

  type ValidationResult[A] = ValidatedNec[ValidationError, A]

  private def logErrors[F[_]: Sync](logger: Logger)(errors: NonEmptyChain[ValidationError]): F[Unit] =
    errors.toList.map {
      case NotAdultError(source) => s"Invalid age: $source"
      case InvalidHeightAndWeight(source) => s"Invalid height/weight format: $source"
      case InvalidGenderError(source) => s"Invalid gender: $source"
    }.map(logger.error(_)).traverse(Sync[F].delay(_)).void

  private def validateAgeLogged(age: Int): ValidationResult[Int] =
    Either.cond[ValidationError, Int](age >= 18, age, NotAdultError(age))
      .toValidatedNec

  private def validateHeightWeight(source: String): ValidationResult[(Int, Int)] =
    (for {
      ints <- Either.catchNonFatal(source.split('x').map(_.toInt)).leftMap(e => InvalidHeightAndWeight(source))
      dimensions <- Either.cond[ValidationError, (Int, Int)](ints.length == 2, (ints(0), ints(1)), InvalidHeightAndWeight(source))
    } yield dimensions)
      .toValidatedNec

  private def validateGenderLogged(gender: Char): ValidationResult[Gender] = (gender match {
    case 'M' => Either.right(Male)
    case 'F' => Either.right(Female)
    case _ => Either.left(InvalidGenderError(gender))
  }).toValidatedNec

  def build[F[_]](implicit S: Sync[F]): F[ValidationService[F]] =
    S.delay(LoggerFactory.getLogger("ValidationService")).map { logger =>
      new ValidationServiceImpl[F](logger)
    }

  class ValidationServiceImpl[F[_]: Sync](logger: Logger) extends ValidationService[F] {
    override def validateUser(name: String, age: Int, gender: Char, heightAndWeight: String): F[ValidatedNel[ValidationError, UserInformation]] = {
      (validateAgeLogged(age), validateHeightWeight(heightAndWeight), validateGenderLogged(gender))
        .mapN {
          case (validAge, (validHeight, validWeight), validGender) =>
            UserInformation(name, validAge, validGender, validHeight, validWeight)
        }.fold(
          e => logErrors(logger)(e).flatMap {
            _ => Sync[F].pure(Validated.invalid(e.toNonEmptyList))
          },
          v => Sync[F].pure(Validated.valid(v))
      )
    }
  }
}