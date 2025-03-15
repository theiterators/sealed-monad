# Advanced Features of Sealed Monad

This document provides a comprehensive guide to the advanced features of the Sealed Monad library. Sealed Monad is a powerful, business-oriented abstraction for elegant error handling and control flow in Scala. In this guide, we elaborate on techniques for composing nested operations, integrating with other monadic structures, debugging, performance optimization, and advanced use cases. Concrete Scala code examples illustrate each feature in a real-world context.

---

## Table of Contents

1. [Nested Operations and For-Comprehensions](#nested-operations-and-for-comprehensions)
2. [Composing with Other Monadic Structures](#composing-with-other-monadic-structures)
3. [Debugging and Performance Optimization](#debugging-and-performance-optimization)
4. [Advanced Use Cases and Common Pitfalls](#advanced-use-cases-and-common-pitfalls)
5. [Core Functionality and API Examples](#core-functionality-and-api-examples)
6. [Conclusion](#conclusion)

---

## Nested Operations and For-Comprehensions

Sealed Monad simplifies the expression of complex business logic that involves multiple sequential operations by leveraging Scala's for-comprehension. Instead of nesting flatMaps and pattern matching calls, you can use a linear, top-down style that cleanly propagates errors.

Below is an example that demonstrates how to retrieve a user from a database, validate the user state, and then fetch additional authentication data. In this example, a missing user or an archived user short-circuits the computation:

```scala
import cats.Monad
import pl.iterators.sealedmonad.syntax._
import pl.iterators.sealedmonad.examples.{User, AuthMethod, LoginResponse}
import cats.effect.IO

object UserService {
  def sealedLogin[M[_]: Monad](
      email: String,
      // Simulated functions returning M[Option[_]] from the database
      findUser: String => M[Option[User]],
      findAuthMethod: (Long, String) => M[Option[AuthMethod]],
      issueTokenFor: User => String,
      checkAuthMethod: AuthMethod => Boolean,
      authMethodFromUserId: Long => AuthMethod,
      mergeAccountsAction: (AuthMethod, User) => M[LoginResponse]
  ): M[LoginResponse] = {
    val s = for {
      user <- findUser(email)
        .valueOr(LoginResponse.InvalidCredentials)
        .ensure(!_.archived, LoginResponse.Deleted)
      userAuthMethod = authMethodFromUserId(user.id)
      authMethod <- findAuthMethod(user.id, userAuthMethod.providerName)
        .valueOrF(mergeAccountsAction(userAuthMethod, user))
    } yield {
      if (checkAuthMethod(authMethod))
        LoginResponse.LoggedIn(issueTokenFor(user))
      else
        LoginResponse.InvalidCredentials
    }
    s.run
  }
}

// Models used in the example
package pl.iterators.sealedmonad.examples

trait User {
  def id: Long
  def archived: Boolean
}

trait AuthMethod {
  def providerName: String
}

sealed trait LoginResponse
object LoginResponse {
  final case class LoggedIn(token: String) extends LoginResponse
  case object InvalidCredentials         extends LoginResponse
  case object Deleted                    extends LoginResponse
}
```

In the above code:
- The helper methods **valueOr** and **ensure** capture common error handling patterns.
- The entire operation is expressed within a for-comprehension, making the logic easy to follow.
- When a value is missing (or the condition fails) the computation "short-circuits" and returns the appropriate error response.

---

## Composing with Other Monadic Structures

Sealed Monad is designed to work effortlessly with other monadic abstractions such as IO, OptionT, and EitherT from libraries like Cats. Below are examples of such integrations.

### Example: Integrating with IO

When working with Cats Effect's IO, you can lift IO values into the Sealed Monad context using the **seal** and **liftF** methods.

```scala
import cats.effect.IO
import pl.iterators.sealedmonad.syntax._
import pl.iterators.sealedmonad.examples._
import cats.Monad

object AuthServiceIO {
  // A helper to lift a pure IO value into the Sealed Monad context
  def loginWithIO(
      email: String,
      findUser: String => IO[Option[User]],
      findAuthMethod: (Long, String) => IO[Option[AuthMethod]],
      issueTokenFor: User => String,
      checkAuthMethod: AuthMethod => Boolean,
      authMethodFromUserId: Long => AuthMethod,
      mergeAccountsAction: (AuthMethod, User) => IO[LoginResponse]
  ): IO[LoginResponse] = {
    implicit val M: Monad[IO] = IO.catsEffectMonadForIO
    val s = for {
      user <- findUser(email)
        .valueOr(LoginResponse.InvalidCredentials)
        .ensure(!_.archived, LoginResponse.Deleted)
      userAuthMethod = authMethodFromUserId(user.id)
      authMethod <- findAuthMethod(user.id, userAuthMethod.providerName)
        .valueOrF(mergeAccountsAction(userAuthMethod, user))
    } yield {
      if (checkAuthMethod(authMethod))
        LoginResponse.LoggedIn(issueTokenFor(user))
      else
        LoginResponse.InvalidCredentials
    }
    s.run
  }
}
```

### Example: Working with OptionT

If you have operations in the shape of `F[Option[A]]`, you can streamline their composition with the help of **valueOr** and **valueOrF**:

```scala
import cats.effect.IO
import pl.iterators.sealedmonad.syntax._
import pl.iterators.sealedmonad.examples._

object CacheService {
  def getCachedData(key: String): IO[Option[String]] = IO.pure(Some("CachedValue"))

  def processData(data: String): IO[String] = IO.pure(data.toUpperCase)

  def getOrProcessData(key: String): IO[String] = {
    implicit val M = IO.catsEffectMonadForIO
    val s = for {
      data <- getCachedData(key)
        .valueOrF(IO.pure("NO_DATA"))
      result <- processData(data).seal  // lifting pure process result into Sealed Monad context
    } yield result
    s.run
  }
}
```

In this example, an optional cache value is handled seamlessly: if absent, a default "NO_DATA" value is used.

---

## Debugging and Performance Optimization

Sealed Monad includes several operators that help you inspect and trace the flow of computations without altering their results. These operators are especially useful for debugging complex monad chains and for logging.

### Example: Using `inspect` and `tap`

You can use the **inspect** method to log intermediate outputs, as well as **tap** and **flatTap** to execute side-effects (such as logging or metric collection) without disrupting the main computation.

```scala
import pl.iterators.sealedmonad.syntax._
import cats.Id
import pl.iterators.sealedmonad.examples._

object DebugExample {
  def debugLoginResult(input: Option[Int]): Id[String] = {
    // Simulate a computation that either produces a value or an error message.
    val result = input
      .valueOr("MissingValue")
      .inspect {
        case Left("MissingValue") => println("Warning: No value found!")
        case Right(v)             => println(s"Value retrieved: $v")
      }
      .map(v => s"Processed: $v")
    result.run  // Evaluates the Sealed Monad computation.
  }
  
  // Example: Using tap for logging
  def computeWithLogging(x: Int): Id[Int] =
    x.liftSealed[Id, String]
      .tap(v => println(s"Before transformation: $v"))
      .map(_ + 10)
      .tap(v => println(s"After transformation: $v"))
      .run
}
```

These debugging examples show how you can sprinkle logging throughout your monadic flow while preserving purity and control flow integrity.

---

## Advanced Use Cases and Common Pitfalls

In real-world applications, complex business logic often necessitates multiple validation steps, fallback strategies, and integration with external services. Sealed Monad shines in such contexts—for instance, in an access control service that carefully models all possible outcomes.

### Example: Access Control Service

Below is a simplified version of an access control service. This service checks whether a user is an administrator, verifies membership, consults a permission service, and then returns one of several outcomes defined as an ADT. Notice how each step is clearly expressed and how error handling is performed consistently.

```scala
import cats.effect.IO
import pl.iterators.sealedmonad.syntax._
import pl.iterators.sealedmonad.examples._

object AccessControlService {
  // ADT representing possible outcomes
  sealed trait AccessControlResult
  object AccessControlResult {
    case object Allowed extends AccessControlResult
    final case class Restricted(details: String) extends AccessControlResult
    case object NotAllowed extends AccessControlResult
  }

  // Simulated service functions
  def isAdmin(userId: Long): IO[Boolean] = IO.pure(userId % 2 == 0)
  def isMember(userId: Long, orgId: Long): IO[Boolean] = IO.pure(true)
  def getPermissions(userId: Long, orgId: Long): IO[Option[String]] = IO.pure(Some("read,write"))
  
  def resolveAccessControl(
      userId: Long,
      orgId: Long
  ): IO[AccessControlResult] = {
    implicit val M = IO.catsEffectMonadForIO
    val s = for {
      admin <- isAdmin(userId).seal
      // If admin then immediately allow access
      _ <- admin.ensure(identity, AccessControlResult.Allowed)
      member <- isMember(userId, orgId).seal
        .ensure(identity, AccessControlResult.NotAllowed)
      perm <- getPermissions(userId, orgId)
        .valueOr(AccessControlResult.Restricted("No permissions"))
    } yield {
      perm match {
        case "read,write" => AccessControlResult.Allowed
        case _            => AccessControlResult.Restricted("Partial access")
      }
    }
    s.run
  }
}
```

### Common Pitfalls Avoided by Sealed Monad

- **Nested Callback Hell:** Instead of deeply nested `flatMap` chains, Sealed Monad's for-comprehension encapsulates the control flow, enhancing readability.
- **Inconsistent Error Handling:** With a unified ADT for errors (or outcomes), the library forces explicit handling of each potential error state.
- **Side Effect Leakage:** Operators like **tap** and **inspect** let you perform logging without unintentionally changing the computation's flow.

---

## Core Functionality and API Examples

Under the hood, Sealed Monad distinguishes between intermediate and final states using the `Transform`, `Pure`, and `Suspend` constructs. Below is a minimal example that demonstrates core API operations like `map`, `flatMap`, and `attempt`.

```scala
import cats.Monad
import pl.iterators.sealedmonad.syntax._
import cats.effect.IO

object CoreFunctionalityDemo {
  // Example of transforming an intermediate value
  def basicTransform[M[_]: Monad](x: M[Int]): M[String] = {
    val s = for {
      value <- x.valueOr("Error: No value")
      // Multiply the value and then convert to string
      result = (value * 2).toString
    } yield s"Result: $result"
    s.run
  }

  // Example using attempt to convert errors to a default value
  def safeDivide[M[_]: Monad](num: Int, denom: Int): M[Int] = {
    val s = for {
      result <- Monad[M].pure {
        if (denom == 0) Left("Division by zero")
        else Right(num / denom)
      }.attempt(identity)
    } yield result.fold(
      error => 0, // default on error
      identity
    )
    s.run
  }
}
```

This example shows how core combinators of Sealed Monad let you transform data and handle errors in a unified, type‑safe manner.

---

## Conclusion

Sealed Monad provides a robust and expressive framework for handling business logic in Scala applications. By embracing for‑comprehension, clear ADT-based error handling, and seamless integration with other functional libraries, developers can craft code that is concise, type‑safe, and easy to maintain.

The examples in this document illustrate how Sealed Monad can simplify complex control flows, avoid common pitfalls, and enhance both debugging and performance. With its human‑readable API, Sealed Monad empowers you to write business logic that is not only correct and efficient but also easily understood by both technical and non‑technical stakeholders.

Happy coding!
