package com.fortysevendeg.tagless.sample
package models

import scala.util.control.NoStackTrace

case object EncryptionError extends RuntimeException with NoStackTrace

sealed trait ValidationError extends Product with Serializable
case object NotAdultError extends ValidationError
case object InvalidGenderError extends ValidationError
case object InvalidHeightAndWeight extends ValidationError

sealed trait Gender extends Product with Serializable
case object Female extends Gender
case object Male extends Gender

final case class UserInformation(name: String, age: Int, gender: Gender, height: Int, weight: Int)
