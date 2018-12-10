### EncryptionService

* This service should use a Java library with an static method.
* The static method returns the encrypted array but can also throw some runtime exception.
* We need to implement an algebra that call to this method but logging the exception and raising our exception into the `F` context:

`case object EncryptionError extends RuntimeException with NoStackTrace`

* This is the signature of the Java method:

`com.sample.encryption.Encryptor.encryptString(s: String): Array[Byte]`

### HTTPService

* The company already has a HTTPClient for interacting with their API. We need to add a log call before and after the call.

* We need to implement the algebra through a builder receiving the HTTPClient. 

* The client looks like following:

```scala
class SampleClient[F[_]: Async] {
  def loadInfoRequest(id: UUID, prefs: Preferences): F[UserInformation] = ???
}
```

### ValidationService

We need to add an implementation for this algebra. It needs to validate the following things:

 * The age should be >= 18.
 * The gender need to be `M` (Male) or `F` (Female)
 * The heightAndWeight comes in the form: 175x80 (cm and kg). We need to split the value and put in two int fields.

The errors need to be accumulated in a NonEmptyList but we don’t give details about the original values, we need to log them as errors.

* Logging

We will use `org.slf4j` for logging but we should treat the logger instantiation and the logging message as a side effect.