package com.fortysevendeg.tagless.sample
package services

import cats.data.ValidatedNel
import com.fortysevendeg.tagless.sample.models._

trait ValidationService[F[_]] {

  def validateUser(name: String, age: Int, gender: Char, heightAndWeight: String): F[ValidatedNel[ValidationError, UserInformation]]

}
