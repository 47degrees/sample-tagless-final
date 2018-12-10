package com.fortysevendeg.tagless.sample

import java.util.UUID

import cats.Monad
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.data.ValidatedNel
import com.fortysevendeg.tagless.sample.models._
import com.fortysevendeg.tagless.sample.services._

trait RequestService[F[_]] {

  def makeRequest(id: UUID, prefs: String): F[ValidatedNel[ValidationError, UserInformation]]

}

object RequestService {

  def build[F[_]: Monad](E: EncryptionService[F], H: HTTPService[F], V: ValidationService[F]): RequestService[F] =
    new RequestService[F] {
      def makeRequest(id: UUID, prefs: String): F[ValidatedNel[ValidationError, UserInformation]] =
        for {
          encryptedPrefs <- E.encryptInfo(prefs)
          httpUserInfo   <- H.loadInfoRequest(id, encryptedPrefs)
          validatedUser  <- V.validateUser(httpUserInfo.name, httpUserInfo.age, httpUserInfo.gender, httpUserInfo.heightAndWeight)
        } yield validatedUser
    }

}