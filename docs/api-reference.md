---
id: api-reference
title: API Reference
slug: /api-reference
---

# API Reference

This page documents the core operations and syntax provided by Sealed Monad. The library offers a rich set of operators tailored to different use cases and scenarios.

## Core Type

The fundamental type in Sealed Monad is `Sealed[F[_], +A, +ADT]` with three type parameters:

- `F[_]`: The effect type (e.g., `Future`, `IO`, `Id`)
- `A`: The intermediate value type 
- `ADT`: The final value or "result" type (typically a sealed trait hierarchy)

Sealed Monad is designed around the principle that all possible outcomes of an operation should be represented as part of a single ADT (Algebraic Data Type). In this design, successful outcomes are typically represented as case classes that extend the result ADT, while error conditions are represented as case objects of the same ADT.

For example:
```scala
sealed trait UserResponse
case class Success(user: User) extends UserResponse
case object NotFound extends UserResponse
case object Unauthorized extends UserResponse
```

This design allows the `run` method to return a single, unified type that can represent all possible outcomes of the operation.

## Creating Sealed Instances

### From Effects

```scala
// From F[A]
def seal[ADT]: Sealed[F, A, ADT]
// Example:
val sealedValue: Sealed[IO, Int, String] = IO.pure(42).seal[String]

// From pure values 
def liftSealed[F[_], ADT]: Sealed[F, A, ADT]
// Example:
val sealedValue: Sealed[IO, Int, String] = 42.liftSealed[IO, String]

// From ADT values
def seal[F[_]]: Sealed[F, Nothing, A]
// Example:
val sealedError: Sealed[IO, Nothing, String] = "error".seal[IO]
```

### From Options

```scala
// valueOr - extract value from Option or return ADT
def valueOr[ADT](orElse: => ADT): Sealed[F, A, ADT]
// Example:
val maybeUser: IO[Option[User]] = userRepository.findById(userId)
val sealedUser: Sealed[IO, User, MyError] = maybeUser.valueOr(MyError.UserNotFound)

// valueOrF - extract value from Option or return effectful ADT
def valueOrF[ADT](orElse: => F[ADT]): Sealed[F, A, ADT]
// Example:
val sealedUser: Sealed[IO, User, MyError] = 
  maybeUser.valueOrF(Logger[IO].error("User not found") *> IO.pure(MyError.UserNotFound))
```

### From Either

```scala
// Convert F[Either[A, B]] to Sealed[F, B, A]
def fromEither: Sealed[F, B, A]
// Example:
val result: IO[Either[String, Int]] = validateInput(data)
val sealedResult: Sealed[IO, Int, String] = result.fromEither

// handleError - convert F[Either[A, B]] to Sealed[F, B, ADT]
def handleError[ADT](f: A => ADT): Sealed[F, B, ADT]
// Example:
val sealedResult: Sealed[IO, Int, MyError] = 
  result.handleError(msg => MyError.ValidationFailed(msg))
```

## Transformations

Once you have a `Sealed` instance, you can use these operations:

### Basic Transformations

```scala
// Transform intermediate value
def map[B](f: A => B): Sealed[F, B, ADT]
// Example:
val user: Sealed[IO, User, MyError] = getUserById(id)
val username: Sealed[IO, String, MyError] = user.map(_.name)

// Monadic binding (flatMap)
def flatMap[B, ADT1 >: ADT](f: A => Sealed[F, B, ADT1]): Sealed[F, B, ADT1]
// Example:
val user: Sealed[IO, User, MyError] = getUserById(id)
val orders: Sealed[IO, List[Order], MyError] = user.flatMap(u => getOrdersByUser(u.id))

// Transform with effects
def semiflatMap[B](f: A => F[B]): Sealed[F, B, ADT]
// Example:
val enrichedUser: Sealed[IO, EnrichedUser, MyError] = 
  user.semiflatMap(u => fetchUserPreferences(u.id).map(prefs => EnrichedUser(u, prefs)))
```

### Validation Operations

```scala
// Ensure condition is met
def ensure[ADT1 >: ADT](pred: A => Boolean, orElse: => ADT1): Sealed[F, A, ADT1]
// Example:
val activeUser: Sealed[IO, User, MyError] = 
  user.ensure(u => !u.archived, MyError.UserInactive)

// Ensure with effectful orElse
def ensureF[ADT1 >: ADT](pred: A => Boolean, orElse: => F[ADT1]): Sealed[F, A, ADT1]
// Example:
val activeUser: Sealed[IO, User, MyError] = 
  user.ensureF(
    u => !u.archived, 
    logger.warn("Archived user access attempt") *> IO.pure(MyError.UserInactive)
  )

// Ensure condition is not met
def ensureNot[ADT1 >: ADT](pred: A => Boolean, orElse: => ADT1): Sealed[F, A, ADT1]
// Example:
val newUser: Sealed[IO, User, MyError] = 
  user.ensureNot(_.hasOrders, MyError.UserHasOrders)

// With access to A in orElse
def ensureOr[ADT1 >: ADT](pred: A => Boolean, orElse: A => ADT1): Sealed[F, A, ADT1]
// Example:
val validUser: Sealed[IO, User, MyError] = 
  user.ensureOr(
    _.emailConfirmed, 
    u => MyError.EmailNotConfirmed(u.email)
  )
```

### Either/Attempt Operations

```scala
// Process value through Either-returning function
def attempt[B, ADT1 >: ADT](f: A => Either[ADT1, B]): Sealed[F, B, ADT1]
// Example:
val processedOrder: Sealed[IO, ProcessedOrder, OrderError] =
  order.attempt { order =>
    if (order.items.isEmpty) Left(OrderError.EmptyOrder)
    else if (order.totalAmount <= 0) Left(OrderError.InvalidAmount)
    else Right(ProcessedOrder(order.id, order.totalAmount))
  }

// Effectful attempt
def attemptF[B, ADT1 >: ADT](f: A => F[Either[ADT1, B]]): Sealed[F, B, ADT1]
// Example:
val validatedOrder: Sealed[IO, ValidatedOrder, OrderError] =
  order.attemptF { order => 
    validateItems(order.items).map {
      case true => Right(ValidatedOrder(order))
      case false => Left(OrderError.InvalidItems)
    }
  }
```

### Side Effect Operations

```scala
// Apply side effects on intermediate value
def tap[B](f: A => B): Sealed[F, A, ADT]
// Example:
val loggedUser: Sealed[IO, User, UserError] = 
  user.tap(u => println(s"User found: ${u.email}"))

// Apply effectful side effects
def flatTap[B](f: A => F[B]): Sealed[F, A, ADT]
// Example:
val loggedUser: Sealed[IO, User, UserError] = 
  user.flatTap(u => logger.info(s"User ${u.id} accessed the system"))

// Inspect current state for debugging
def inspect(pf: PartialFunction[Either[ADT, A], Any]): Sealed[F, A, ADT]
// Example:
val debuggedLogin: Sealed[IO, User, LoginError] =
  user.inspect {
    case Right(u) => println(s"Success: Found user ${u.email}")
    case Left(LoginError.InvalidCredentials) => println("Error: Invalid credentials")
    case Left(err) => println(s"Error: $err")
  }
```

### Completion Operations

```scala
// Complete with final ADT value based on intermediate value
def complete[ADT1 >: ADT](f: A => ADT1): Sealed[F, Nothing, ADT1]
// Example:
val loginResponse: Sealed[IO, Nothing, LoginResponse] = 
  user.complete(u => LoginResponse.LoggedIn(generateToken(u)))

// Complete with effectful ADT value
def completeWith[ADT1 >: ADT](f: A => F[ADT1]): Sealed[F, Nothing, ADT1]
// Example:
val orderResponse: Sealed[IO, Nothing, OrderResponse] =
  order.completeWith(o => createOrderInDatabase(o).map(id => OrderResponse.Created(id)))

// Evaluate to final result
def run[ADT1 >: ADT](implicit ev: A <:< ADT1, F: Monad[F]): F[ADT1]
// Example:
val result: IO[LoginResponse] = loginFlow.run
```

## Syntax Extensions

Sealed Monad provides extension methods for various types through its syntax import:

```scala
import pl.iterators.sealedmonad.syntax._
```

This enables all the syntax shown in the examples above.

## Usage Best Practices

1. **Start with effects** - Begin your computation with extension methods like `.valueOr`, `.ensure`, or `.seal`
2. **Chain with for-comprehensions** - Use for-comprehensions to chain operations
3. **End with run** - Complete your computation with `.run` to get the final `F[ADT]`
4. **Structure complex logic in steps** - Break down complex flows into well-named helper methods
5. **Use descriptive ADTs** - Design your ADT to clearly communicate all possible outcomes

For complete API documentation, refer to the [Scaladoc](https://javadoc.io/doc/pl.iterators/sealed-monad_2.13/latest/index.html).
