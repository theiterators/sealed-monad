---
sidebar_position: 3
---

# Practical Use Cases

Sealed Monad shines in real-world business logic scenarios. This section demonstrates practical examples comparing traditional approaches to using Sealed Monad, illustrating how it improves readability and maintainability.

## Prerequisites

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

## Use Case 1: User Login Flow

### Traditional Approach

Let's look at a typical authentication flow implemented with traditional pattern matching and monad transformers:

```scala mdoc:nest
def login(email: String,
          findUser: String => Future[Option[User]],
          findAuthMethod: (Long, Provider) => Future[Option[AuthMethod]],
          issueTokenFor: User => String,
          checkAuthMethodAction: AuthMethod => Boolean,
          authMethodFromUserIdF: Long => AuthMethod,
          mergeAccountsAction: (AuthMethod, User) => Future[LoginResponse]): Future[LoginResponse] =
  findUser(email).flatMap {
    case None =>
      M.pure(LoginResponse.InvalidCredentials)
    case Some(user) if user.archived =>
      M.pure(LoginResponse.Deleted)
    case Some(user) =>
      val authMethod = authMethodFromUserIdF(user.id)
      val actionT = OptionT(findAuthMethod(user.id, authMethod.provider))
        .map(checkAuthMethodAction(_))
      actionT.value flatMap {
        case Some(true) =>
          M.pure(LoginResponse.LoggedIn(issueTokenFor(user)))
        case Some(false) =>
          M.pure(LoginResponse.InvalidCredentials)
        case None =>
          mergeAccountsAction(authMethod, user)
      }
  }
```

**Problems with this approach:**
- Nested pattern matching creates deeply indented code
- Control flow is fragmented across multiple branches
- Hard to visually trace the "happy path"
- Difficult to modify without introducing bugs
- Monad transformer usage adds complexity
- The business logic isn't immediately clear to readers

### Sealed Monad Approach

Now let's implement the same logic using Sealed Monad:

```scala mdoc
import pl.iterators.sealedmonad.syntax._

def sealedLogin(email: String,
                findUser: String => Future[Option[User]],
                findAuthMethod: (Long, Provider) => Future[Option[AuthMethod]],
                issueTokenFor: User => String,
                checkAuthMethodAction: AuthMethod => Boolean,
                authMethodFromUserIdF: Long => AuthMethod,
                mergeAccountsAction: (AuthMethod, User) => Future[LoginResponse]): Future[LoginResponse] = {
  val s = for {
    user <- findUser(email)
      .valueOr[LoginResponse](LoginResponse.InvalidCredentials)
      .ensure(!_.archived, LoginResponse.Deleted)
    userAuthMethod = authMethodFromUserIdF(user.id)
    authMethod <- findAuthMethod(user.id, userAuthMethod.provider).valueOrF(mergeAccountsAction(userAuthMethod, user))
  } yield if (checkAuthMethodAction(authMethod)) LoginResponse.LoggedIn(issueTokenFor(user)) else LoginResponse.InvalidCredentials

  s.run
}
```

**Benefits of this approach:**
- Linear flow is easy to follow from top to bottom
- Validations occur in place without breaking the flow
- For-comprehension structure clearly shows data dependencies
- Error handling is declarative rather than imperative
- Significantly fewer lines of code
- Business logic is immediately clear at a glance

## Use Case 2: Structuring Complex Business Logic

For complex business logic, Sealed Monad can be used to create a tiered structure that makes the main flow obvious while encapsulating implementation details:

```scala mdoc
class Example3[M[_]: Monad] {
  import pl.iterators.sealedmonad.Sealed
  import pl.iterators.sealedmonad.syntax._

  // High-level business flow with descriptive step names
  def sealedLogin(email: String): M[LoginResponse] =
    (for {
      user        <- findAndValidateUser(email)
      authMethod  <- findOrMergeAuthMethod(user)
      loginResult <- validateAuthMethodAction(user, authMethod)
    } yield loginResult).run

  // Mid-level methods with focused responsibilities
  private def findAndValidateUser(email: String): Sealed[M, User, LoginResponse] = 
    findUser(email)
      .valueOr(LoginResponse.InvalidCredentials)
      .ensure(!_.archived, LoginResponse.Deleted)

  private def findOrMergeAuthMethod(user: User): Sealed[M, AuthMethod, LoginResponse] = {
    val userAuthMethod = authMethodFromUserIdF(user.id)
    findAuthMethod(user.id, userAuthMethod.provider)
      .valueOrF(mergeAccountsAction(userAuthMethod, user))
  }

  private def validateAuthMethodAction(user: User, authMethod: AuthMethod): Sealed[M, LoginResponse, Nothing] = {
    val result =
      if (checkAuthMethodAction(authMethod)) LoginResponse.LoggedIn(issueTokenFor(user)) 
      else LoginResponse.InvalidCredentials
    Monad[M].pure(result).seal
  }

  // Low-level service dependencies
  def findUser: String => M[Option[User]]                         = ???
  def findAuthMethod: (Long, Provider) => M[Option[AuthMethod]]   = ???
  def authMethodFromUserIdF: Long => AuthMethod                   = ???
  def mergeAccountsAction: (AuthMethod, User) => M[LoginResponse] = ???
  def checkAuthMethodAction: AuthMethod => Boolean                = ???
  def issueTokenFor: User => String                               = ???
}
```

**Benefits of this structure:**
- **High-level readability**: The main flow consists of just 3 descriptive steps anyone can understand
- **Progressive disclosure**: Implementation details are encapsulated in well-named methods
- **Separation of concerns**: Each step handles a specific part of the business logic
- **Documentation through naming**: Method names serve as documentation
- **Maintainability**: Changes can be isolated to specific steps without affecting the overall flow
- **Testability**: Each step can be tested independently

This approach creates a "self-documenting" service that clearly communicates its purpose at each level of abstraction. New team members can quickly understand the high-level flow, then dive into specific implementations as needed.

## Use Case 3: API Request Processing

Sealed Monad is particularly valuable for API endpoints with multiple validation steps and potential failure modes:

```scala mdoc
import cats.syntax.applicative._

sealed trait CreateOrderResponse
object CreateOrderResponse {
  case class Created(orderId: String) extends CreateOrderResponse
  case object UserNotFound extends CreateOrderResponse
  case object ProductOutOfStock extends CreateOrderResponse
  case object InsufficientFunds extends CreateOrderResponse
  case object AddressInvalid extends CreateOrderResponse
}

case class Order(id: String, userId: String, productId: String, quantity: Int)

def createOrder[M[_]: Monad](
  userId: String, 
  productId: String,
  quantity: Int,
  findUser: String => M[Option[User]],
  checkInventory: (String, Int) => M[Boolean],
  checkUserFunds: User => M[Boolean],
  validateAddress: User => Boolean,
  createOrderRecord: (String, String, Int) => M[Order]
): M[CreateOrderResponse] = {
  (for {
    // Validate user exists
    user <- findUser(userId).valueOr[CreateOrderResponse](CreateOrderResponse.UserNotFound)
    
    // Validate user has valid address
    _ <- user.pure[M].ensure(validateAddress, CreateOrderResponse.AddressInvalid)
    
    // Check if product is in stock
    _ <- checkInventory(productId, quantity)
           .ensure(identity, CreateOrderResponse.ProductOutOfStock)
    
    // Check if user has sufficient funds
    _ <- checkUserFunds(user)
           .ensure(identity, CreateOrderResponse.InsufficientFunds)
    
    // Create the order
    order <- createOrderRecord(userId, productId, quantity).seal
  } yield CreateOrderResponse.Created(order.id)).run
}
```

This pattern works exceptionally well for API endpoints, where different validation steps need to be performed in sequence, with clear responses for each potential failure mode.

## Further Examples and Resources

For more examples of using Sealed Monad in real-world scenarios, check out the [examples in the repository](https://github.com/theiterators/sealed-monad/blob/master/examples/src/main/scala/pl/iterators/sealedmonad/examples/Options.scala).

If you're curious about the design process behind Sealed Monad, watch [this video by Marcin Rzeźnicki](https://www.youtube.com/watch?v=uZ7IFQTYPic) explaining the evolution of the library.
