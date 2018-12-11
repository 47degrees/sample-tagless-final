package com.fortysevendeg.tagless.sample

import java.util.UUID

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, ValidatedNel}
import cats.effect.IO
import cats.syntax.apply._
import com.fortysevendeg.http._
import com.fortysevendeg.tagless.sample.models._
import com.fortysevendeg.tagless.sample.services._
import org.scalatest.FunSuite

class RequestServiceSpec extends FunSuite {

  val defaultClient: HTTPClient[IO] = new HTTPClient[IO] {
    def loadInfoRequest(request: HTTPUserRequest): IO[HTTPUserResponse] =
      IO.raiseError(new RuntimeException("Test exception"))
  }

  def httpServiceBuild(implicit httpClient: HTTPClient[IO]): IO[HTTPService[IO]]  = HTTPService.build[IO](httpClient)
  def encryptionServiceBuild: IO[EncryptionService[IO]]                           = EncryptionService.build[IO]
  def validationServiceBuild: IO[ValidationService[IO]]                           = ValidationService.build[IO]

  def requestServiceBuild(implicit httClient: HTTPClient[IO]): IO[RequestService[IO]] =
    (encryptionServiceBuild, httpServiceBuild, validationServiceBuild).mapN(RequestService.build[IO])

  val validPrefs: String              = "{ notifications: false }"
  val validHTTPUser: HTTPUserResponse = HTTPUserResponse("Norma", 34, 'F', "173x62")
  val validUser: UserInformation      = UserInformation("Norma", 34, Female, 173, 62)

  test("return a valid response if the data is valid") {

    implicit val client: HTTPClient[IO] = new HTTPClient[IO] {
      def loadInfoRequest(request: HTTPUserRequest): IO[HTTPUserResponse] = IO(validHTTPUser)
    }

    val result: ValidatedNel[ValidationError, UserInformation] =
      requestServiceBuild.flatMap(_.makeRequest(UUID.randomUUID(), validPrefs)).unsafeRunSync()

    assertResult(Valid(validUser))(result)
  }

  test("raise an EncryptionError if the preferences are empty") {

    implicit val client: HTTPClient[IO] = defaultClient
    val result: Either[Throwable, ValidatedNel[ValidationError, UserInformation]] =
      requestServiceBuild.flatMap(_.makeRequest(UUID.randomUUID(), "")).attempt.unsafeRunSync()

    assertResult(Left(EncryptionError))(result)
  }

  test("raise an EncryptionError if the preferences are too big") {

    implicit val client: HTTPClient[IO] = defaultClient
    val result: Either[Throwable, ValidatedNel[ValidationError, UserInformation]] =
      requestServiceBuild
        .flatMap(_.makeRequest(UUID.randomUUID(), (1 to 500).map(_ => "x").mkString("")))
        .attempt
        .unsafeRunSync()

    assertResult(Left(EncryptionError))(result)
  }

  test("raise a NotAdultError if the user is 17 years old") {

    implicit val client: HTTPClient[IO] = new HTTPClient[IO] {
      def loadInfoRequest(request: HTTPUserRequest): IO[HTTPUserResponse] =
        IO(validHTTPUser.copy(age = 17))
    }

    val result: ValidatedNel[ValidationError, UserInformation] =
      requestServiceBuild.flatMap(_.makeRequest(UUID.randomUUID(), validPrefs)).unsafeRunSync()

    assertResult(Invalid(NonEmptyList.one(NotAdultError)))(result)
  }

  test("raise a InvalidGenderError if the user gender is invalid") {

    implicit val client: HTTPClient[IO] = new HTTPClient[IO] {
      def loadInfoRequest(request: HTTPUserRequest): IO[HTTPUserResponse] =
        IO(validHTTPUser.copy(gender = 'X'))
    }

    val result: ValidatedNel[ValidationError, UserInformation] =
      requestServiceBuild.flatMap(_.makeRequest(UUID.randomUUID(), validPrefs)).unsafeRunSync()

    assertResult(Invalid(NonEmptyList.one(InvalidGenderError)))(result)
  }

  test("raise a InvalidHeightAndWeight if the user heightAndWeight is invalid") {

    implicit val client: HTTPClient[IO] = new HTTPClient[IO] {
      def loadInfoRequest(request: HTTPUserRequest): IO[HTTPUserResponse] =
        IO(validHTTPUser.copy(heightAndWeight = "xxx"))
    }

    val result: ValidatedNel[ValidationError, UserInformation] =
      requestServiceBuild.flatMap(_.makeRequest(UUID.randomUUID(), validPrefs)).unsafeRunSync()

    assertResult(Invalid(NonEmptyList.one(InvalidHeightAndWeight)))(result)
  }

  test("raise all validation errors if the user has different invalid data") {

    implicit val client: HTTPClient[IO] = new HTTPClient[IO] {
      def loadInfoRequest(request: HTTPUserRequest): IO[HTTPUserResponse] =
        IO(validHTTPUser.copy(age = 17, gender = 'X', heightAndWeight = "xxx"))
    }

    val result: ValidatedNel[ValidationError, UserInformation] =
      requestServiceBuild.flatMap(_.makeRequest(UUID.randomUUID(), validPrefs)).unsafeRunSync()

    assertResult(Invalid(Set(NotAdultError, InvalidGenderError, InvalidHeightAndWeight)))(
      result.leftMap(_.toList.toSet))
  }

}
