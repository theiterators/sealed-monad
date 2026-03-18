---
slug: /
sidebar_position: 1
title: Introduction to Sealed Monad
---

# Introduction to Sealed Monad

[![Maven Central](https://img.shields.io/maven-central/v/pl.iterators/sealed-monad_2.13.svg)](https://search.maven.org/artifact/pl.iterators/sealed-monad_2.13)
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

Without Sealed Monad — nested pattern matching:

```scala
def login(email: String, ...): Future[LoginResponse] = {
  findUser(email).flatMap {
    case None =>
      Future.successful(LoginResponse.InvalidCredentials)
    case Some(user) if user.archived =>
      Future.successful(LoginResponse.Deleted)
    case Some(user) =>
      findAuthMethod(user.id, Provider.EmailPass).map {
        case None => LoginResponse.ProviderAuthFailed
        case Some(am) if !checkAuthMethod(am) => LoginResponse.InvalidCredentials
        case Some(_) => LoginResponse.LoggedIn(issueTokenFor(user))
      }
  }
}
```

With Sealed Monad — linear flow:

```scala
import pl.iterators.sealedmonad.syntax._

def login(email: String, ...): Future[LoginResponse] = {
  (for {
    user <- findUser(email)
      .valueOr(LoginResponse.InvalidCredentials)
      .ensure(!_.archived, LoginResponse.Deleted)
    authMethod <- findAuthMethod(user.id, Provider.EmailPass)
      .valueOr(LoginResponse.ProviderAuthFailed)
      .ensure(checkAuthMethod, LoginResponse.InvalidCredentials)
  } yield LoginResponse.LoggedIn(issueTokenFor(user))).run
}
```

See [Motivations & Core Concepts](motivations) for the full explanation.

## Installation

Add the following to your `build.sbt`:

```scala
libraryDependencies += "pl.iterators" %% "sealed-monad" % "2.0.1"
```

For more detailed installation instructions and examples, see the [Installation Guide](installation).

## Documentation

- [Motivations & Core Concepts](motivations)
- [Practical Use Cases](usecases)
- [API Reference](api-reference)
- [Best Practices](best-practices)
- [Comparison with Other Approaches](comparison)
- [Migration Guide](migration-guide)
- [FAQ](faq)

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](https://github.com/theiterators/sealed-monad/blob/master/LICENSE) file for details.
