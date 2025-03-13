---
sidebar_position: 2
---

# Motivations & Core Concepts

## Why We Created Sealed Monad

We created Sealed Monad after observing patterns and challenges in our codebase. We noticed that well-designed business logic often follows certain principles, but traditional error handling approaches made implementation verbose and hard to read.

Let's first define some boilerplate and then dive into the key observations that led to Sealed Monad:

```scala mdoc:reset-object
  import scala.concurrent.Future
  import cats.instances.future._
  import cats.Monad
  import cats.data.OptionT
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val M: Monad[Future] = implicitly[Monad[Future]]

  sealed trait Provider

  final case class EmailAddress(value: String) extends AnyVal
  final case class User(id: Long, email: EmailAddress, archived: Boolean)
  final case class AuthMethod(provider: Provider) extends AnyVal
```

### 1. Operation Results as ADTs

We represent operation results as Algebraic Data Types (ADTs), usually with a sealed trait and several case classes/objects. This approach models different business outcomes explicitly and comprehensively:

```scala mdoc
sealed trait LoginResponse

  object LoginResponse {
      final case class LoggedIn(token: String)             extends LoginResponse
      case object AccountsMergeRequested                   extends LoginResponse
      final case class AccountsMerged(token: String)       extends LoginResponse
      case object InvalidCredentials                       extends LoginResponse
      case object Deleted                                  extends LoginResponse
      case object ProviderAuthFailed                       extends LoginResponse
  }
```

### 2. Methods as Self-Contained Units

Service methods are designed as closed units of code, each returning one value from the result ADT:

```scala mdoc
  def login(email: String,
            findUser: String => Future[Option[User]],
            findAuthMethod: (Long, Provider) => Future[Option[AuthMethod]],
            issueTokenFor: User => String,
            checkAuthMethodAction: AuthMethod => Boolean,
            authMethodFromUserIdF: Long => AuthMethod,
            mergeAccountsAction: (AuthMethod, User) => Future[LoginResponse]): Future[LoginResponse] = ???
```

### 3. No Explicit Error Type Distinction

We found that distinguishing between "errors" and "valid results" is often arbitrary in business logic. For example, when a user tries to log in with a deleted account, is "Deleted" an error or a legitimate response? With Sealed Monad, everything is simply a response.

### 4. Method-Local Error Handling

Global or module-based error handling can be harmful to application architecture. Different operations need different error-handling strategies. Sealed Monad encourages handling business outcomes at the method level where context is clear.

### 5. For-Comprehension Friendly

For-comprehensions provide a clean, sequential way to express business logic. Sealed Monad is designed to work seamlessly with for-comprehensions.

### 6. Linear vs. Branching Logic

Traditional if-else or pattern-matching creates branching logic that becomes hard to follow. Sealed Monad aims to linearize the flow, making code more readable.

## Core Concepts of Sealed Monad

### The Sealed Type

The core type in Sealed Monad is `Sealed[F[_], +A, +ADT]` with three type parameters:

- `F[_]`: The effect type (e.g., `Future`, `IO`, `Id`)
- `A`: The intermediate value type (values you work with in the "happy path")
- `ADT`: The final value or "result" type (typically a sealed trait hierarchy)

Conceptually, `Sealed` is like `EitherT` but oriented toward a workflow that:

1. Works with intermediate values (`A`) through map/flatMap
2. Can short-circuit to a final result (`ADT`) at any point
3. Must ultimately evaluate to a final value of type `ADT`

### The Execution Flow

A typical Sealed Monad workflow:

1. Start with values wrapped in effects (`F[A]`, `F[Option[A]]`, etc.)
2. Process these values, potentially short-circuiting with an `ADT` value if validation fails
3. Continue processing until reaching a final result
4. Call `.run` to evaluate the computation to `F[ADT]`

### Key Operations

Sealed Monad provides several categories of operations:

1. **Extraction operations**: Like `valueOr` for working with `Option` types
2. **Validation operations**: Like `ensure` for conditional validation
3. **Transformation operations**: Like `map`/`flatMap` for working with intermediate values
4. **Composition operations**: For combining different Sealed instances
5. **Side-effect operations**: Like `tap` and `inspect` for debugging or logging
6. **Completion operations**: Like `complete` to finish with a final ADT value

These operations work together to create clean, readable business logic that handles errors in a declarative way.
