# Sealed Monad: Core Concepts

Sealed Monad is a Scala library designed for business-oriented error handling using a concise for-comprehension style. It extends and improves upon traditional monads such as EitherT, offering a human‐readable API with controlled error handling tailored to business logic domains.

Below is an enhanced overview of the library’s core concepts, enriched with concrete Scala code examples. Each feature is paired with a code snippet illustrating its usage.

---

## What Is Sealed Monad?

At its core, the Sealed Monad encapsulates computations that can either produce a successful business value or fail with a domain‐specific error (an ADT). It separates technical exceptions (handled externally by your effect type) from business errors.

```scala
// Define a business error ADT
sealed trait BusinessError
object BusinessError {
  case object UserNotFound extends BusinessError
  case object ValidationFailed extends BusinessError
  case object Unauthorized extends BusinessError
}

// A simplified representation of the Sealed Monad type alias
import cats.Monad
import cats.syntax.all._

final case class Sealed[F[_], A, E](run: F[Either[E, A]]) {
  def map[B](f: A => B)(implicit F: Monad[F]): Sealed[F, B, E] =
    Sealed(F.map(run)(_.map(f)))

  def flatMap[B, E1 >: E](f: A => Sealed[F, B, E1])(implicit F: Monad[F]): Sealed[F, B, E1] =
    Sealed(F.flatMap(run) {
      case Left(err)  => F.pure(Left(err))
      case Right(a) => f(a).run
    })

  def valueOr[F2[_]](defaultError: => E)(implicit F: Monad[F]): Sealed[F, A, E] =
    Sealed(F.map(run) {
      case None    => Left(defaultError)
      case Some(a) => Right(a)
      case other   => other
    })

  def ensure(predicate: A => Boolean, error: => E)(implicit F: Monad[F]): Sealed[F, A, E] =
    flatMap { a =>
      if (predicate(a)) this
      else Sealed(F.pure(Left(error)))
    }
}

object Sealed {
  // Lifts a pure value into a Sealed monad
  def pure[F[_]: Monad, A](a: A): Sealed[F, A, Nothing] =
    Sealed(implicitly[Monad[F]].pure(Right(a)))

  // Lifts an effectful value into a Sealed monad assuming a successful computation
  def liftF[F[_]: Monad, A](fa: F[A]): Sealed[F, A, Nothing] =
    Sealed(implicitly[Monad[F]].map(fa)(Right(_)))

  // Constructs a Sealed instance from an effectful computation that might produce an Option value.
  def fromOption[F[_]: Monad, A](foa: F[Option[A]], error: => BusinessError): Sealed[F, A, BusinessError] =
    Sealed(implicitly[Monad[F]].map(foa) {
      case Some(a) => Right(a)
      case None    => Left(error)
    })
}
```

---

## Business-Oriented Error Handling

Sealed Monad encourages errors to be captured as first-class citizens using domain-specific ADTs. In contrast to exception throwing, it forces the developer to consider error cases explicitly.

### Code Example

```scala
// Business logic using Sealed Monad
import cats.Id
import cats.instances.id._  // Provide instance for Monad[Id]

// Imagine a function to find a user
def findUser(userId: String): Id[Option[String]] = 
  if(userId == "123") Some("Alice") else None

// Using Sealed.fromOption to turn an Option into a Sealed value
val sealedUser: Sealed[Id, String, BusinessError] =
  Sealed.fromOption(findUser("123"), BusinessError.UserNotFound)

// Now chain with further business validations using for-comprehension
val result: Sealed[Id, String, BusinessError] = for {
  user <- sealedUser
  // Ensure that the user name starts with "A"
  validated <- Sealed.pure[Id, String](user).ensure(_.startsWith("A"), BusinessError.ValidationFailed)
} yield validated

// Running the computation returns either an error or the computed value.
println(result.run)  // Output: Right("Alice")
```

---

## For-Comprehension Style

Sealed Monad enables the use of Scala’s for-comprehensions to succinctly sequence computations. This leads to more linear and readable error handling flows.

### Code Example

```scala
import cats.Id

def getUser(id: String): Id[Option[String]] = if (id == "1") Some("Bob") else None
def validateUser(name: String): Id[Boolean] = Id.pure(name.nonEmpty)

// Wrap our logic into a Sealed Monad for better flow
val program: Sealed[Id, String, BusinessError] = for {
  user   <- Sealed.fromOption(getUser("1"), BusinessError.UserNotFound)
  valid  <- Sealed.liftF(validateUser(user))
  _      <- if (valid) Sealed.pure[Id, Unit](()) 
            else Sealed.liftF(Id.pure(throw new Exception("Unexpected"))) // Business failure path
} yield user

println(program.run)  // Output: Right("Bob")
```

---

## Human-Readable API

The method names in Sealed Monad are designed to be self-explanatory. Instead of using low-level combinators like flatMap or map directly, names such as `valueOr` and `ensure` clearly reflect intended business semantics.

### Code Example

```scala
// Using human-readable names in a registration service function
def registerUser(username: String): Id[Option[String]] =
  if (username == "admin") None else Some(username)

val registration: Sealed[Id, String, BusinessError] = for {
  name <- Sealed.fromOption(registerUser("user1"), BusinessError.UserNotFound)
  _    <- Sealed.pure[Id, Unit](()).ensure(_ => name.length >= 3, BusinessError.ValidationFailed)
} yield s"Registered user: $name"

println(registration.run)  // Output: Right("Registered user: user1")
```

---

## Computations as Trees (Short-Circuiting)

Sealed Monad represents computations as a tree. Branches in the computation short-circuit as soon as an error is encountered, much like an early return in imperative programming.

### Code Example

```scala
// Simulate multiple dependent operations with potential failures
def step1: Id[Option[Int]] = Some(42)
def step2(value: Int): Id[Option[String]] =
  if (value == 42) Some("Valid") else None

val process: Sealed[Id, String, BusinessError] = for {
  result1 <- Sealed.fromOption(step1, BusinessError.ValidationFailed)
  result2 <- Sealed.fromOption(step2(result1), BusinessError.UserNotFound)
} yield result2

println(process.run)  // Output: Right("Valid")
```

---

## Type Safety Against Unhandled Errors

The power of Sealed Monad is its ability to enforce error handling at compile time. By requiring you to deal with the error branch (an ADT), no failure escapes unnoticed.

### Code Example

```scala
def processPayment(amount: Double): Id[Option[String]] =
  if (amount > 0) Some("Payment Processed") else None

val paymentProcessing: Sealed[Id, String, BusinessError] = for {
  confirmation <- Sealed.fromOption(processPayment(100.0), BusinessError.ValidationFailed)
  _            <- Sealed.pure[Id, Unit](()).ensure(_ => confirmation.nonEmpty, BusinessError.Unauthorized)
} yield confirmation

println(paymentProcessing.run)  // Output: Right("Payment Processed")
```

---

## Integrating with Other Functional Libraries

Sealed Monad works seamlessly with Cats and other libraries. For example, you can use Cats’ Monad typeclass instances to work with various effect types like Future or IO.

### Code Example with Future

```scala
import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Success, Failure}
import cats.instances.future._
import cats.syntax.all._

implicit val ec: ExecutionContext = ExecutionContext.global

def fetchData(id: String): Future[Option[String]] =
  Future.successful(if (id == "10") Some("Data") else None)

val futureProcess: Sealed[Future, String, BusinessError] = for {
  data  <- Sealed.fromOption(fetchData("10"), BusinessError.UserNotFound)
  valid <- Sealed.liftF(Future.successful(data.nonEmpty))
  _     <- if (valid) Sealed.pure[Future, Unit](()) 
           else Sealed.liftF(Future.successful(throw new Exception("Invalid Data")))
} yield data

futureProcess.run.onComplete {
  case Success(result) => println(s"Future result: $result")
  case Failure(exception) => println(s"Error: ${exception.getMessage}")
}
// Expected output: Future result: Right("Data")
```

---

## Additional Utility Methods

Sealed Monad provides a rich set of utility methods such as:

- **`valueOr`**: Extracts a value from an Option or terminates with an error.
- **`ensure`**: Validates an intermediate result against a predicate.
- **`liftF`**: Lifts a value into the Sealed context.
- **`fromOption`**: Converts an Option into a Sealed instance with a provided error if missing.

### Code Example Demonstrating Utility Methods

```scala
// Suppose we have a configuration value that may be missing.
def getConfig(key: String): Id[Option[String]] =
  if (key == "url") Some("http://example.com") else None

val config: Sealed[Id, String, BusinessError] =
  Sealed.fromOption(getConfig("url"), BusinessError.UserNotFound)
    .ensure(url => url.startsWith("http"), BusinessError.ValidationFailed)

println(config.run)  // Output: Right("http://example.com")
```

---

## Conclusion

Sealed Monad offers an expressive and type-safe way to handle business logic errors in Scala. Its design focuses on clarity through self-explanatory naming, concise for-comprehension flows, and robust compile-time guarantees. By treating computations as trees that short-circuit on error, it encourages writing code that is not only easy to read but also easier to maintain.

Integrate Sealed Monad into your Scala projects to produce clear, maintainable, and error-safe business logic!

Happy coding!

