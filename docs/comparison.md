---
sidebar_position: 6
---

# Comparison with Other Approaches

Sealed Monad offers a distinctive approach to error handling in Scala. This page compares it with other common error handling techniques to help you understand where it fits in the ecosystem.

## Traditional Approaches to Error Handling

Let's implement the same example with different error handling approaches to see how they compare.

### The Example: User Authentication

We'll use a user authentication scenario that needs to:
1. Find a user by email
2. Check if the user's account is active (not archived)
3. Verify the authentication method
4. Generate a token or handle the appropriate error case

## Approach 1: Pattern Matching with Options/Either

```scala
def login(
  email: String,
  findUser: String => Future[Option[User]],
  findAuthMethod: (Long, Provider) => Future[Option[AuthMethod]],
  checkAuthMethod: AuthMethod => Boolean,
  issueTokenFor: User => String
): Future[LoginResponse] = {
  findUser(email).flatMap {
    case None => 
      Future.successful(LoginResponse.InvalidCredentials)
    case Some(user) if user.archived => 
      Future.successful(LoginResponse.Deleted)
    case Some(user) => 
      findAuthMethod(user.id, defaultProvider).flatMap {
        case None => 
          Future.successful(LoginResponse.ProviderAuthFailed)
        case Some(authMethod) if !checkAuthMethod(authMethod) => 
          Future.successful(LoginResponse.InvalidCredentials)
        case Some(authMethod) => 
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

```scala
def login(
  email: String,
  findUser: String => Future[Option[User]],
  findAuthMethod: (Long, Provider) => Future[Option[AuthMethod]],
  checkAuthMethod: AuthMethod => Boolean,
  issueTokenFor: User => String
): Future[LoginResponse] = {
  
  // Start with user lookup
  val result = for {
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
      findAuthMethod(user.id, defaultProvider), 
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
  } yield response
  
  result.value.map(_.merge)
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

```scala
def login(
  email: String,
  findUser: String => Future[Option[User]],
  findAuthMethod: (Long, Provider) => Future[Option[AuthMethod]],
  checkAuthMethod: AuthMethod => Boolean,
  issueTokenFor: User => String
): Future[LoginResponse] = {
  
  (for {
    // Get user or return InvalidCredentials
    user <- findUser(email)
      .valueOr[LoginResponse](LoginResponse.InvalidCredentials)
      .ensure(!_.archived, LoginResponse.Deleted)
    
    // Get auth method or return ProviderAuthFailed
    authMethod <- findAuthMethod(user.id, defaultProvider)
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

## Approach 4: ZIO with custom error types

```scala
sealed trait LoginError
object LoginError {
  case object UserNotFound extends LoginError
  case object AccountDeleted extends LoginError
  case object AuthMethodNotFound extends LoginError
  case object InvalidAuthMethod extends LoginError
}

def login(
  email: String,
  findUser: String => ZIO[Any, Nothing, Option[User]],
  findAuthMethod: (Long, Provider) => ZIO[Any, Nothing, Option[AuthMethod]],
  checkAuthMethod: AuthMethod => Boolean,
  issueTokenFor: User => String
): ZIO[Any, Nothing, LoginResponse] = {
  
  (for {
    // Get user or fail with UserNotFound
    user <- findUser(email).flatMap {
      case None => ZIO.fail(LoginError.UserNotFound)
      case Some(user) => ZIO.succeed(user)
    }
    
    // Check if user is archived
    _ <- ZIO.fail(LoginError.AccountDeleted).when(user.archived)
    
    // Get auth method or fail with AuthMethodNotFound
    authMethod <- findAuthMethod(user.id, defaultProvider).flatMap {
      case None => ZIO.fail(LoginError.AuthMethodNotFound)
      case Some(method) => ZIO.succeed(method)
    }
    
    // Check auth method validity
    _ <- ZIO.fail(LoginError.InvalidAuthMethod).when(!checkAuthMethod(authMethod))
    
    // Create success response
    response = LoginResponse.LoggedIn(issueTokenFor(user))
  } yield response
}.mapError {
  case LoginError.UserNotFound => LoginResponse.InvalidCredentials
  case LoginError.AccountDeleted => LoginResponse.Deleted
  case LoginError.AuthMethodNotFound => LoginResponse.ProviderAuthFailed
  case LoginError.InvalidAuthMethod => LoginResponse.InvalidCredentials
}
```

**Pros:**
- Powerful effect system
- Clear separation of error types and business logic errors
- Strong type safety throughout

**Cons:**
- Multiple transformations needed (error domain to response domain)
- More complex to set up
- Requires learning ZIO's specific error handling approach

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