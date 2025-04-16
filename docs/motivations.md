---
id: motivations
title: Motivations & Core Concepts
slug: /motivations
---

## Why We Created Sealed Monad

We created Sealed Monad after observing patterns and challenges in real-world business logic. We noticed that well-designed business logic often follows certain principles, but traditional error handling approaches made implementation verbose and hard to read.

Let's first define some domain models we'll use in our examples:

```scala
import scala.concurrent.Future
import cats.Monad
import cats.instances.future._
import cats.data.OptionT

// Domain models
sealed trait Provider
object Provider {
  case object EmailPass extends Provider
  case object OAuth extends Provider
}

case class User(id: Long, email: String, archived: Boolean)
case class AuthMethod(userId: Long, provider: Provider)

// Result ADT for our login operation
sealed trait LoginResponse
object LoginResponse {
  final case class LoggedIn(token: String) extends LoginResponse
  case object InvalidCredentials extends LoginResponse
  case object Deleted extends LoginResponse
  case object ProviderAuthFailed extends LoginResponse
}
```

### Key Observations

#### 1. Operation Results as ADTs

Well-designed services represent operation results as Algebraic Data Types (ADTs), usually with a sealed trait and several case classes/objects. This approach models different business outcomes explicitly and comprehensively.

#### 2. Methods as Self-Contained Units

Service methods are designed as closed units of code, each returning one value from the result ADT:

```scala
def login(email: String,
          findUser: String => Future[Option[User]],
          findAuthMethod: (Long, Provider) => Future[Option[AuthMethod]],
          issueTokenFor: User => String,
          checkAuthMethod: AuthMethod => Boolean): Future[LoginResponse] = ???
```

#### 3. No Arbitrary Error/Success Distinction

We found that distinguishing between "errors" and "valid results" is often arbitrary in business logic. For example, when a user tries to log in with a deleted account, is "Deleted" an error or a legitimate response? With Sealed Monad, everything is simply a response.

#### 4. Method-Local Error Handling

Global or module-based error handling can be harmful to application architecture. Different operations need different error-handling strategies. Sealed Monad encourages handling business outcomes at the method level where context is clear.

#### 5. For-Comprehension Friendly

For-comprehensions provide a clean, sequential way to express business logic. Sealed Monad is designed to work seamlessly with for-comprehensions.

#### 6. Linear vs. Branching Logic

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

#### Extraction Operations

These help you work with `Option` and other container types:

```scala
// Extract a value from an Option or return a specified ADT
val user: Sealed[Future, User, LoginResponse] = 
  findUser(email).valueOr(LoginResponse.InvalidCredentials)
```

#### Validation Operations

These let you validate values and short-circuit on failure:

```scala
// Ensure user is not archived, or return Deleted response
val activeUser: Sealed[Future, User, LoginResponse] = 
  user.ensure(!_.archived, LoginResponse.Deleted)
```

#### Transformation Operations

These transform intermediate values:

```scala
// Map user to a token
val token: Sealed[Future, String, LoginResponse] = 
  user.map(u => issueTokenFor(u))
```

#### Side Effect Operations

These let you perform side effects without affecting the computation:

```scala
// Log the current state
val loggedUser: Sealed[Future, User, LoginResponse] = 
  user.tap(u => println(s"Found user: ${u.email}"))
```

All these operations work together to create clean, linear business logic that's easy to read and maintain.

### Comparing with Traditional Approaches

To demonstrate the value of Sealed Monad, let's compare two implementations of the same login logic:

#### Without Sealed Monad

```scala
def login(email: String,
          findUser: String => Future[Option[User]],
          findAuthMethod: (Long, Provider) => Future[Option[AuthMethod]],
          issueTokenFor: User => String,
          checkAuthMethod: AuthMethod => Boolean): Future[LoginResponse] = {
  findUser(email).flatMap {
    case None =>
      Future.successful(LoginResponse.InvalidCredentials)
    case Some(user) if user.archived =>
      Future.successful(LoginResponse.Deleted)
    case Some(user) =>
      findAuthMethod(user.id, Provider.EmailPass).map {
        case None => 
          LoginResponse.ProviderAuthFailed
        case Some(authMethod) if !checkAuthMethod(authMethod) => 
          LoginResponse.InvalidCredentials
        case Some(_) => 
          LoginResponse.LoggedIn(issueTokenFor(user))
      }
  }
}
```

#### With Sealed Monad

```scala
import pl.iterators.sealedmonad.syntax._

def login(email: String,
          findUser: String => Future[Option[User]],
          findAuthMethod: (Long, Provider) => Future[Option[AuthMethod]],
          issueTokenFor: User => String,
          checkAuthMethod: AuthMethod => Boolean): Future[LoginResponse] = {
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

The Sealed Monad version is more concise, easier to follow, and effectively communicates the business logic in a linear, step-by-step fashion.