---
sidebar_position: 1
---

# Introduction to Sealed Monad

Sealed Monad is a Scala library designed to provide elegant, business logic-oriented, for-comprehension-style error handling that improves code readability and maintainability.

## What is Sealed Monad?

Sealed Monad allows you to write clear, linear business logic that handles errors in a declarative way, without nested pattern matching or complex monad transformer stacks. It's particularly valuable when working with:

- Complex validation workflows
- API responses with multiple possible outcomes
- Business logic with branching decisions
- Error-prone operations that need clean error handling

If you're familiar with Cats' `EitherT`, you can think of Sealed Monad as "EitherT on steroids" with a focus on developer ergonomics and readability.

## Core Benefits

- **Linear Code Flow**: Write top-down, sequential code that's easy to follow
- **Descriptive Error Handling**: No more nested pattern matching or complex conditional structures
- **Business-Logic Focus**: Error handling that focuses on representing business outcomes, not technical errors
- **For-Comprehension Friendly**: Leverage Scala's for-comprehensions for clean, pipeline-style code
- **ADT-Driven Design**: Naturally works with sealed trait hierarchies to model operation results

## Installation

Add the following to your `build.sbt`:

```scala
libraryDependencies += "pl.iterators" %% "sealed-monad" % "@VERSION@"
```

Sealed Monad is available for:
- Scala 2.13.x and 3.x
- JVM, ScalaNative, and Scala.js platforms

## Quick Example

Here's a simple example showing how Sealed Monad can clean up a typical validation workflow:

```scala
import pl.iterators.sealedmonad.syntax._
import cats.effect.IO

// Response ADT
sealed trait CreateUserResponse
object CreateUserResponse {
  case class Created(id: UserId) extends CreateUserResponse
  case object EmailAlreadyExists extends CreateUserResponse
  case object InvalidEmail extends CreateUserResponse
  case object PasswordTooWeak extends CreateUserResponse
}

def createUser(request: CreateUserRequest): IO[CreateUserResponse] = {
  (for {
    // Validate email format
    email <- validateEmail(request.email)
              .valueOr[CreateUserResponse](CreateUserResponse.InvalidEmail)
    
    // Check if email already exists
    _ <- userRepository.findByEmail(email)
          .ensure(_.isEmpty, CreateUserResponse.EmailAlreadyExists)
    
    // Validate password strength
    _ <- validatePassword(request.password)
          .valueOr[CreateUserResponse](CreateUserResponse.PasswordTooWeak)
    
    // All validations passed, create the user
    userId <- userRepository.create(email, request.password).seal
  } yield CreateUserResponse.Created(userId)).run
}
```

In the following sections, we'll explore the core concepts, operators, and best practices for using Sealed Monad effectively in your applications.