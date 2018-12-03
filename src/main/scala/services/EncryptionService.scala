package com.fortysevendeg.tagless.sample
package services

trait EncryptionService[F[_]] {
  def encryptInfo(info: String): F[Array[Byte]]
}