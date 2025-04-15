---
id: comparison
title: Comparison with Other Approaches
slug: /comparison
---

# Comparison with Other Approaches

Sealed Monad offers a distinctive approach to error handling in Scala. This page compares it with other common error handling techniques to help you understand where it fits in the ecosystem.

## The Example: User Authentication

To provide a fair comparison, we'll implement the same example with different error handling approaches. Our example involves user authentication with the following requirements:

1. Find a user by email
2. Check if the user's account is active (not archived)
3. Verify the authentication method
4. Generate a token or handle the appropriate error case

First, let's define our domain models and result type:

```scala
import scala.concurrent.Future
import cats.Monad
import cats.effect.IO

// Domain models
case class User(id: Long, email: String, archived: Boolean)
case class AuthMethod(userId: Long, provider: String)

// Result ADT
sealed trait LoginResponse
object LoginResponse {
  final case class LoggedIn(token: String) extends LoginResponse
  case object InvalidCredentials extends LoginResponse
  case object Deleted extends LoginResponse
  case object ProviderAuthFailed extends LoginResponse
}
```

## Approach 1: Pattern Matching with Options

This is perhaps the most common approach in Scala applications:

```scala
def login(
  email: String,
  findUser: String => Future[Option[User]],
  findAuthMethod: (Long, String) => Future[Option[AuthMethod]],
  checkAuthMethod: AuthMethod => Boolean,
  issueTokenFor: User => String
): Future[LoginResponse] = {
  import scala.concurrent.ExecutionContext.Implicits.global
  
  findUser(email).flatMap {
    case None => 
      Future.successful(LoginResponse.InvalidCredentials)
    case Some(user) if user.archived => 
      Future.successful(LoginResponse.Deleted)
    case Some(user) => 
      findAuthMethod(user.id, "email").flatMap {
        case None => 
          Future.successful(LoginResponse.ProviderAuthFailed)
        case Some(authMethod) if !checkAuthMethod(authMethod) => 
          Future.successful(LoginResponse.InvalidCredentials)
        case Some(_) => 
          Future.successful(LoginResponse.LoggedIn(issueTokenFor(user)))
      }
  }
}
```

**Pros:**
- Straightforward and familiar to most Scala developers
- No external libraries required
- Explicit control flow

**Cons:**
- Nested pattern matching creates deeply indented code
- Hard to follow the "happy path" through the nested branches
- Error handling is mixed with the main flow
- Difficult to modify without introducing bugs
- The more conditions or steps, the more unwieldy it becomes

## Approach 2: Using Cats' EitherT

EitherT is a monad transformer that combines the Either monad with an arbitrary monad, allowing for composing operations that can return either success or error values:

```scala
import cats.data.EitherT
import cats.implicits._

def login(
  email: String,
  findUser: String => Future[Option[User]],
  findAuthMethod: (Long, String) => Future[Option[AuthMethod]],
  checkAuthMethod: AuthMethod => Boolean,
  issueTokenFor: User => String
): Future[LoginResponse] = {
  import scala.concurrent.ExecutionContext.Implicits.global
  
  // Start with user lookup
  (for {
    user <- EitherT.fromOptionF(
      findUser(email),
      LoginResponse.InvalidCredentials: LoginResponse
    )
    
    // Check if user is archived
    _ <- EitherT.cond(
      !user.archived,
      (),
      LoginResponse.Deleted: LoginResponse
    )
    
    // Get auth method
    authMethod <- EitherT.fromOptionF(
      findAuthMethod(user.id, "email"), 
      LoginResponse.ProviderAuthFailed: LoginResponse
    )
    
    // Check auth method validity
    _ <- EitherT.cond(
      checkAuthMethod(authMethod),
      (),
      LoginResponse.InvalidCredentials: LoginResponse
    )
    
    // Create success response
    response = LoginResponse.LoggedIn(issueTokenFor(user))
  } yield response).merge
}
```

**Pros:**
- Linear flow with for-comprehensions
- Clear separation of happy path and error cases
- Makes good use of Scala's type system

**Cons:**
- Requires understanding monad transformers
- More verbose for simpler cases
- Error outcomes and success types are treated differently
- `.merge` at the end is non-intuitive for new developers
- Type signatures can be complex

## Approach 3: Using Sealed Monad

Now let's implement the same logic using Sealed Monad:

```scala
import pl.iterators.sealedmonad.syntax._

def login(
  email: String,
  findUser: String => Future[Option[User]],
  findAuthMethod: (Long, String) => Future[Option[AuthMethod]],
  checkAuthMethod: AuthMethod => Boolean,
  issueTokenFor: User => String
): Future[LoginResponse] = {
  import scala.concurrent.ExecutionContext.Implicits.global
  
  (for {
    // Get user or return InvalidCredentials
    user <- findUser(email)
      .valueOr[LoginResponse](LoginResponse.InvalidCredentials)
      .ensure(!_.archived, LoginResponse.Deleted)
    
    // Get auth method or return ProviderAuthFailed
    authMethod <- findAuthMethod(user.id, "email")
      .valueOr[LoginResponse](LoginResponse.ProviderAuthFailed)
      .ensure(checkAuthMethod, LoginResponse.InvalidCredentials)
  } yield LoginResponse.LoggedIn(issueTokenFor(user))).run
}
```

**Pros:**
- Concise, linear flow
- Declarative error handling directly in the main flow
- Familiar for-comprehension structure
- Method names clearly express intent (e.g., `valueOr`, `ensure`)
- Uniform treatment of all outcomes (both success and error cases)
- `run` clearly indicates when the computation is executed

**Cons:**
- Requires learning a library-specific API
- Introduced extra abstraction that needs to be understood

## Approach 4: Using ZIO

ZIO offers powerful error handling with a distinct approach:

```scala
import zio._

def login(
  email: String,
  findUser: String => Task[Option[User]],
  findAuthMethod: (Long, String) => Task[Option[AuthMethod]],
  checkAuthMethod: AuthMethod => Boolean,
  issueTokenFor: User => String
): Task[LoginResponse] = {
  
  // Find user
  ZIO.fromOption(findUser(email).orDie)
    .mapError(_ => LoginResponse.InvalidCredentials)
    
    // Check if user is archived
    .filterOrElseWith(
      user => !user.archived,
      _ => ZIO.succeed(LoginResponse.Deleted)
    )
    
    // Get auth method
    .flatMap(user => 
      ZIO.fromOption(findAuthMethod(user.id, "email").orDie)
        .mapError(_ => LoginResponse.ProviderAuthFailed)
        
        // Check auth method validity
        .filterOrElseWith(
          authMethod => checkAuthMethod(authMethod),
          _ => ZIO.succeed(LoginResponse.InvalidCredentials)
        )
        
        // Create success response
        .map(_ => LoginResponse.LoggedIn(issueTokenFor(user)))
    ).catchAll(ZIO.succeed(_))
}
```

**Pros:**
- Powerful effect system
- Strong type safety
- Comprehensive error handling capabilities

**Cons:**
- Steeper learning curve
- More verbose for simple cases
- Requires adopting the whole ZIO ecosystem

## When to Use Sealed Monad

Sealed Monad is particularly well-suited for:

1. **Business logic with multiple, well-defined outcomes**
   - When operations can have several "normal" outcomes (not just success/failure)
   - When modeling with ADTs/sealed traits is natural for your domain

2. **API implementations with predictable response types**
   - RESTful services with HTTP status codes mapping to business outcomes
   - GraphQL resolvers with structured error responses

3. **Workflows with sequential validation steps**
   - User registration flows
   - Payment processing pipelines 
   - Multi-step form submissions

4. **Teams looking for readable, self-documenting code**
   - When code needs to be understood by developers with varied experience levels
   - When business logic should be clear to non-technical stakeholders

## When to Consider Alternatives

You might prefer other approaches when:

1. **You need more advanced effect handling**
   - For complex concurrency patterns, ZIO may be more suitable
   - For reactive streaming, consider Fs2 or Akka Streams

2. **Error handling is an incidental concern**
   - For simple, mostly happy-path operations where errors are rare
   - For CRUD operations with minimal business logic

3. **Team familiarity is a primary concern**
   - If your team is already experienced with another approach

## Summary

|                     | Sealed Monad | Pattern Matching | EitherT     | ZIO           |
|---------------------|--------------|------------------|-------------|---------------|
| **Verbosity**       | Low          | Medium           | Medium      | High          |
| **Readability**     | High         | Low              | Medium      | Medium        |
| **Learning Curve**  | Low          | None             | Medium      | High          |
| **Type Safety**     | High         | Medium           | High        | Very High     |
| **ADT Support**     | Excellent    | Good             | Good        | Good          |
| **Pure FP**         | Yes          | No               | Yes         | Yes           |
| **Extensibility**   | Good         | Limited          | Good        | Excellent     |

Sealed Monad occupies a sweet spot for many business logic scenarios, offering a balance of readability, type safety, and expressiveness without the complexity of full-featured effect systems.