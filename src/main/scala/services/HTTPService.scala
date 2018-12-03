package com.fortysevendeg.tagless.sample
package services

import java.util.UUID

import com.fortysevendeg.http.HTTPUserResponse

trait HTTPService[F[_]] {

  def loadInfoRequest(id: UUID, encryptedPreferences: Array[Byte]): F[HTTPUserResponse]

}