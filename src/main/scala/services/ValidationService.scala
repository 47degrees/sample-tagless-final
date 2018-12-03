package com.fortysevendeg.tagless.sample
package services

import cats.data.{ValidatedNec, ValidatedNel}
import cats.effect.Async
import cats.implicits._
import com.fortysevendeg.tagless.sample.models._
import org.slf4j.LoggerFactory

import scala.util.matching.Regex

trait ValidationService[F[_]] {

  def validateUser(
      name: String,
      age: Int,
      gender: Char,
      heightAndWeight: String): F[ValidatedNel[ValidationError, UserInformation]]

}

object ValidationService {

  def apply[F[_]](implicit A: Async[F]): F[ValidationService[F]] =
    A.delay(LoggerFactory.getLogger("HTTPService")).map { logger =>
      new ValidationService[F] {

        type ValidationResult[A] = ValidatedNec[ValidationError, A]

        val HeightAndWeightRegEx: Regex = "(\\d+)x(\\d+)".r

        def validateUser(
            name: String,
            age: Int,
            gender: Char,
            heightAndWeight: String): F[ValidatedNel[ValidationError, UserInformation]] = {

          def validateAge: F[ValidationResult[Unit]] =
            if (age >= 18) A.pure(().validNec)
            else {
              A.delay(logger.error(s"Error validating age of user $name. Value: $age"))
                .as(NotAdultError.invalidNec)
            }

          def asValidatedGender(gender: Gender): ValidationResult[Gender] =
            gender.validNec

          def validateGender: F[ValidationResult[Gender]] = gender match {
            case 'M' => asValidatedGender(Male).pure[F]
            case 'F' => asValidatedGender(Female).pure[F]
            case _ =>
              A.delay(logger.error(s"Error validating gender of user $name. Value: $age"))
                .as(InvalidGenderError.invalidNec)
          }

          def validateHeightAndWeight: F[ValidationResult[(Int, Int)]] =
            heightAndWeight match {
              case HeightAndWeightRegEx(height, weight) =>
                (height.toInt, weight.toInt).validNec[ValidationError].pure[F]
              case _ =>
                A.delay(logger.error(
                    s"Error validating height and weight of user $name. Value: $heightAndWeight"))
                  .as(InvalidHeightAndWeight.invalidNec)
            }

          (validateAge, validateGender, validateHeightAndWeight).mapN {
            (validatedAge, validatedGender, validatesHW) =>
              (validatedAge, validatedGender, validatesHW).mapN {
                (_, g, hAndW) => UserInformation(name, age, g, hAndW._1, hAndW._2)
              }.leftMap(_.toNonEmptyList)
          }
        }

      }
    }

}
