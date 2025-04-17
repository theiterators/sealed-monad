# sealed-monad

[![Maven Central](https://img.shields.io/maven-central/v/pl.iterators/sealed-monad_2.13.svg)]()
[![GitHub license](https://img.shields.io/badge/license-Apache2.0-blue.svg)](https://raw.githubusercontent.com/theiterators/sealed-monad/master/COPYING)
[![sealed-monad Scala version support](https://index.scala-lang.org/theiterators/sealed-monad/sealed-monad/latest-by-scala-version.svg)](https://index.scala-lang.org/theiterators/sealed-monad/sealed-monad)

Sealed Monad is a Scala library that provides elegant, business logic-oriented, for-comprehension-style error handling. It enhances code readability and maintainability by allowing you to express business logic with clear error handling in a linear, declarative way.

## What is Sealed Monad?

Sealed Monad can be thought of as "EitherT on steroids, with more human-readable method names." It provides a clean, intuitive, and type-safe way to handle errors in functional programming, particularly in complex business logic scenarios.

If you've ever found yourself writing deeply nested pattern matching or complex conditional structures for error handling, Sealed Monad offers a more elegant approach.

## Key Benefits

- **Linear Code Flow**: Write top-down, sequential code that's easy to follow
- **Type Safety**: Enforce handling of all error cases at compile time
- **Reduced Boilerplate**: No need to map between different error types when combining functions
- **For-Comprehension Friendly**: Leverage Scala's for-comprehensions for clean, pipeline-style code
- **Business-Logic Oriented**: Error handling focused on representing business outcomes, not just technical errors

## Quick Example

Here's a simple example showing how Sealed Monad can clean up a typical validation workflow:

```scala
import pl.iterators.sealedmonad.syntax._
import cats.effect.IO

// Define our result ADT
sealed trait CreateUserResponse
object CreateUserResponse {
  case class Success(id: String) extends CreateUserResponse
  case object EmailAlreadyExists extends CreateUserResponse
  case object InvalidEmail extends CreateUserResponse
  case object PasswordTooWeak extends CreateUserResponse
}

def createUser(request: CreateUserRequest): IO[CreateUserResponse] = {
  (for {
    // Validate email format
    _ <- validateEmail(request.email)
          .ensure(isValid => isValid, CreateUserResponse.InvalidEmail)
    
    // Check if email already exists
    emailExists <- userRepository.emailExists(request.email).seal
    _ <- (!emailExists).pure[IO]
          .ensure(identity, CreateUserResponse.EmailAlreadyExists)
    
    // Validate password strength
    _ <- validatePassword(request.password)
          .ensure(isStrong => isStrong, CreateUserResponse.PasswordTooWeak)
    
    // All validations passed, create the user
    userId <- userRepository.create(request.email, request.password).seal
  } yield CreateUserResponse.Success(userId)).run
}
```

## Installation

Add the following to your `build.sbt`:

```scala
libraryDependencies += "pl.iterators" %% "sealed-monad" % "1.3"
```

For more detailed installation instructions and examples, see the [Installation Guide](installation).

## Documentation

- [Motivations & Core Concepts](https://theiterators.github.io/sealed-monad/motivations)
- [Installation Guide](https://theiterators.github.io/sealed-monad/installation)
- [Practical Use Cases](https://theiterators.github.io/sealed-monad/usecases)
- [API Reference](https://theiterators.github.io/sealed-monad/api-reference)
- [Best Practices](https://theiterators.github.io/sealed-monad/best-practices)
- [Comparison with Other Approaches](https://theiterators.github.io/sealed-monad/comparison)
- [Migration Guide](https://theiterators.github.io/sealed-monad/migration-guide)
- [FAQ](https://theiterators.github.io/sealed-monad/faq)

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
