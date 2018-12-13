package com.fortysevendeg.tagless.sample
package models

import scala.util.control.NoStackTrace

case object EncryptionError extends RuntimeException with NoStackTrace

sealed trait ValidationError extends Product with Serializable
case class NotAdultError(source: Int) extends ValidationError
case class InvalidGenderError(source: Char) extends ValidationError
case class InvalidHeightAndWeight(source: String) extends ValidationError

sealed trait Gender extends Product with Serializable
case object Female extends Gender
case object Male extends Gender

final case class UserInformation(name: String, age: Int, gender: Gender, height: Int, weight: Int)
