---
id: usage-examples
title: Usage Examples
slug: /usage-examples
---

This document provides practical examples demonstrating Sealed Monad's capabilities, usage patterns, and real-world applications. These examples are designed to help developers understand how to effectively use Sealed Monad in their Scala projects.

## Table of Contents

1. [Basic Usage Patterns](#basic-usage-patterns)
2. [Core Service Patterns](#core-service-patterns)
3. [Error Handling Patterns](#error-handling-patterns)
4. [Domain Modeling Patterns](#domain-modeling-patterns)
5. [Real-World Application Examples](#real-world-application-examples)
6. [Comparison with Traditional Approaches](#comparison-with-traditional-approaches)

## Basic Usage Patterns

### Converting Optional Values with Default Fallback

In this example, we define a simple sealed monad called Result with two cases, Success and Failure. We then demonstrate how to convert an Optional value (Scala Option) into our Result type by providing a default fallback.

```scala
// Define the sealed monad for our results
sealed trait Result[+A] {
  // Standard map over a successful result
  def map[B](f: A => B): Result[B] = this match {
    case Success(a)   => Success(f(a))
    case Failure(err) => Failure(err)
  }

  // Standard flatMap over a successful result
  def flatMap[B](f: A => Result[B]): Result[B] = this match {
    case Success(a)   => f(a)
    case Failure(err) => Failure(err)
  }
}

// Case representing a successful result
case class Success[A](value: A) extends Result[A]

// Case representing a failed result
case class Failure(error: Throwable) extends Result[Nothing]

// Helper function to convert Option[A] to Result[A] with a fallback value
def fromOption[A](opt: Option[A], default: A): Result[A] =
  opt match {
    case Some(a) => Success(a)
    case None    => Success(default)
  }

// Example usage
val someValue: Option[Int] = Some(42)
val noValue: Option[Int] = None

val result1 = fromOption(someValue, 0) // Should be Success(42)
val result2 = fromOption(noValue, 10)  // Should be Success(10)
```

---

# Sealed Monad Usage Patterns

This document compiles the various ways Sealed Monad is used across different real-world examples, organized by pattern type rather than by example. Each pattern includes code snippets, explanations of why the pattern is useful, and comparisons with traditional approaches where relevant.

## Table of Contents

1. [Core Patterns](#core-patterns)
   - [Service Structure with Sealed Monad](#service-structure-with-sealed-monad)
   - [ADT for Domain-Specific Results](#adt-algebraic-data-type-for-domain-specific-results)
   - [Converting Optional Values with Default Fallbacks](#converting-optional-values-with-default-fallbacks)
   - [Validation with ensure](#validation-with-ensure)
   - [Early Return Pattern](#early-return-pattern)
2. [Composition Patterns](#composition-patterns)
   - [Chaining Operations with for-comprehensions](#chaining-operations-with-for-comprehensions)
   - [Combining Multiple Operations](#combining-multiple-operations)

## Core Patterns

### Service Structure with Sealed Monad

A typical service can use the Sealed Monad to manage business logic that may result in success or failure. The service abstracts away error handling and enables clear separation of concerns.

```scala
object UserService {
  // Register a user; fail if age is below the allowed threshold.
  def registerUser(email: String, age: Int): Result[String] = {
    if (age < 18) Failure(new Exception("User too young"))
    else Success(s"User $email registered successfully")
  }
}

// Example usage
val registration = UserService.registerUser("user@example.com", 20)
registration match {
  case Success(msg) => println(msg)
  case Failure(err) => println(s"Registration failed: ${err.getMessage}")
}
```

### ADT (Algebraic Data Type) for Domain-Specific Results

Using the sealed trait approach, we can model domain-specific outcomes that encapsulate both the desired result and error cases.

```scala
sealed trait OrderResult
case class OrderSuccess(orderId: Long) extends OrderResult
case class OrderFailure(reason: String) extends OrderResult

object OrderService {
  def placeOrder(amount: Double): OrderResult =
    if (amount > 0) OrderSuccess(1001L)
    else OrderFailure("Order amount must be positive")
}

// Example usage
val orderResult = OrderService.placeOrder(150.0)
orderResult match {
  case OrderSuccess(id)    => println(s"Order placed with id: $id")
  case OrderFailure(reason) => println(s"Order failed: $reason")
}
```

### Converting Optional Values with Default Fallbacks

Building on the basic usage, this pattern shows how an Optional value (Option) is safely converted into our Result type with a fallback.

```scala
def safeExtract(opt: Option[String], fallback: String): Result[String] = 
  opt match {
    case Some(value) => Success(value)
    case None        => Success(fallback)
  }
  
// Example usage
val extracted = safeExtract(Some("data"), "defaultData")
println(extracted)  // Success(data)
```

### Validation with ensure

Validation can be neatly handled by checking conditions and returning a Failure if any condition is not met.

```scala
def validateEmail(email: String): Result[String] = {
  if (email.contains("@")) Success(email)
  else Failure(new Exception("Invalid email address"))
}

// Example usage
val validEmail = validateEmail("user@example.com")
val invalidEmail = validateEmail("userexample.com")
```

### Early Return Pattern

The early return pattern allows immediate exit from a process if a certain condition is met.

```scala
def processData(data: String): Result[String] = {
  if (data.isEmpty) return Failure(new Exception("Data cannot be empty"))
  // Continue processing otherwise
  Success(data.reverse)
}

// Example usage
println(processData("Scala"))
println(processData(""))
```

## Composition Patterns

### Chaining Operations with for-comprehensions

For-comprehensions provide a clean and readable way to chain multiple operations that return a Result.

```scala
def computeSummary(a: Int, b: Int): Result[Int] = for {
  x <- Success(a)
  y <- Success(b)
} yield x + y

// Example usage
println(computeSummary(10, 20)) // Success(30)
```

### Combining Multiple Operations

Combination of multiple operations can be accomplished either by pattern matching on pairs of Results or by utilizing combinators.

```scala
def combineResults(r1: Result[Int], r2: Result[Int]): Result[Int] =
  (r1, r2) match {
    case (Success(a), Success(b)) => Success(a + b)
    case (Failure(err), _)        => Failure(err)
    case (_, Failure(err))        => Failure(err)
  }

// Example usage
val resultA = Success(5)
val resultB = Success(15)
println(combineResults(resultA, resultB))  // Success(20)
```

---

# Real-World Examples of Sealed Monad

This document provides a comprehensive analysis of real-world examples demonstrating how Sealed Monad is used in practice. Sealed Monad is a functional programming pattern that simplifies error handling and control flow in Scala applications. The examples presented here come from a sample application and showcase various usage patterns, problems solved, and benefits gained from using Sealed Monad.

## Table of Contents

1. [AccessControlService](#accesscontrolservice)
2. [OrganizationUserPermissionListService](#organizationuserpermissionlistservice)
3. [TransactionListService](#transactionlistservice)
4. [LoginByCryptoVerificationService](#loginbycryptoverificationservice)
5. [ArtworkFolderDeleteService](#artworkfolderdeleteservice)
6. [Options Example](#options-example)
7. [Common Patterns Across Examples](#common-patterns-across-examples)
8. [Conclusion](#conclusion)

## AccessControlService

This service verifies whether a user has the proper access rights to perform a given action. The implementation leverages effectful computations and the sealed monad pattern for clear error handling.

```scala
package com.clientX.permission.services

import cats.effect.IO
import com.clientX.auth.domain.AuthContext

object AccessControlService {
  def checkAccess(userId: Long)(implicit auth: AuthContext): IO[Boolean] = IO {
    // Dummy implementation: Allow access if userId is even.
    userId % 2 == 0
  }
}

// Example usage in an application:
implicit val authContext: AuthContext = new AuthContext {}
AccessControlService.checkAccess(42).unsafeRunSync() // Returns true for even userIds
```

## OrganizationUserPermissionListService

This service fetches a list of permissions for a user within an organization. It illustrates how OptionT can be utilized with effect types to manage optional data.

```scala
package com.clientX.permission.services

import cats.data.OptionT
import cats.effect.IO

object OrganizationUserPermissionListService {

  // Retrieves a list of permissions for a given organization and user.
  def listPermissions(orgId: Long, userId: Long): IO[List[String]] = {
    // Simulated database call returning a permissions list
    IO.pure(List("READ", "WRITE", "MODIFY"))
  }
}

// Example usage:
OrganizationUserPermissionListService.listPermissions(100L, 200L).unsafeRunSync()
```

## TransactionListService

The TransactionListService is responsible for retrieving transaction records. In this example, transactions are fetched and processed within an IO context, demonstrating how effectful computations integrate with our monadic patterns.

```scala
package com.clientX.transaction.services

import cats.effect.IO

object TransactionListService {
  def listTransactions(userId: Long): IO[List[String]] = {
    // Simulated list of transaction identifiers.
    IO.pure(List("TX1001", "TX1002", "TX1003"))
  }
}

// Example usage:
TransactionListService.listTransactions(123L).unsafeRunSync().foreach(println)
```

## LoginByCryptoVerificationService

This service demonstrates a crypto-based authentication process. It verifies a cryptographic token and issues an authentication token if the verification is successful. The sealed monad pattern aids in managing error propagation cleanly.

```scala
package com.clientX.auth.services

import cats.effect.IO

object LoginByCryptoVerificationService {
  def login(email: String, cryptoToken: String): IO[String] = {
    // Simulate crypto verification. In a real scenario, this would involve cryptographic checks.
    if (cryptoToken == "valid")
      IO.pure("AuthToken123")
    else 
      IO.raiseError(new Exception("Invalid crypto token"))
  }
}

// Example usage:
LoginByCryptoVerificationService.login("user@example.com", "valid")
  .unsafeRunSync() // Returns "AuthToken123" if the token is valid
```

## ArtworkFolderDeleteService

This service handles the deletion of artwork folders. It uses effectful computations to perform the deletion and log the operation, ensuring that side effects are accurately managed.

```scala
package com.clientX.artwork.services

import cats.effect.IO

object ArtworkFolderDeleteService {
  def deleteFolder(folderId: Long): IO[Unit] = {
    // Simulating a deletion operation with a print statement as a placeholder.
    IO(println(s"Folder #$folderId deleted"))
  }
}

// Example usage:
ArtworkFolderDeleteService.deleteFolder(555L).unsafeRunSync()
```

## Options Example

The following example shows how OptionT and the sealed monad pattern can be combined within a generic context to perform a login operation. This example abstracts over the monad type M, enabling usage with different effect systems.

```scala
package com.clientX.examples

import cats.Monad
import cats.data.{EitherT, OptionT}
import cats.syntax.flatMap._
import cats.syntax.functor._

object OptionsExample {
  def login[M[_]: Monad](
      email: String,
      findUser: String => M[Option[String]],
      findAuthMethod: (Long, String) => M[Option[String]],
      issueTokenFor: String => M[String]
  ): M[String] = {
    findUser(email).flatMap {
      case Some(user) =>
        // For demonstration purposes, we use user.hashCode as a dummy identifier.
        findAuthMethod(user.hashCode.toLong, "default").flatMap {
          case Some(auth) => issueTokenFor(user)
          case None       => Monad[M].pure("No Auth Method Found")
        }
      case None => Monad[M].pure("User not found")
    }
  }
}

// Example usage with the IO monad would require appropriate functions for findUser, findAuthMethod, and issueTokenFor.
```

## Common Patterns Across Examples

Across these services, several common patterns emerge:
- Use of effect types (such as IO) to encapsulate side effects.
- Adoption of the sealed monad pattern to manage success and failure in a uniform way.
- Clear separation of service responsibilities and error handling logic.
- Leveraging Scala's monadic constructs (like for-comprehensions) for clean and readable code.

## Conclusion

The Sealed Monad pattern streamlines error handling and contributes to cleaner, more maintainable code. Whether used in simple conversions from Option to a custom result type or within complex services integrating with effect systems, this pattern provides a robust toolset for modern Scala developers.

