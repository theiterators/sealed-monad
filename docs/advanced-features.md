---
id: advanced-features
title: Advanced Features
slug: /advanced-features
---

This document provides a comprehensive guide to the advanced features of the Sealed Monad library. We'll cover techniques for composing nested operations, integrating with other monadic structures, debugging, and advanced use cases. Each feature is illustrated with concrete Scala code examples to demonstrate practical application.

## Nested Operations with For-Comprehensions

Sealed Monad simplifies the expression of complex business logic that involves multiple sequential operations by leveraging Scala's for-comprehension. Instead of nesting flatMaps and pattern matching calls, you can use a linear, top-down style that cleanly propagates errors.

### Example: Multi-Step Authentication Flow

Below is an example that demonstrates how to retrieve a user from a database, validate the user state, and then fetch additional authentication data:

```scala
import cats.Monad
import pl.iterators.sealedmonad.syntax._
import cats.effect.IO

// ADT representing all possible login outcomes
sealed trait LoginResponse
object LoginResponse {
  final case class LoggedIn(token: String) extends LoginResponse
  case object InvalidCredentials extends LoginResponse
  case object Deleted extends LoginResponse
}

def sealedLogin[M[_]: Monad](
    email: String,
    // Repository functions
    findUser: String => M[Option[User]],
    findAuthMethod: (Long, String) => M[Option[AuthMethod]],
    issueTokenFor: User => String,
    checkAuthMethod: AuthMethod => Boolean,
    authMethodFromUserId: Long => AuthMethod,
    mergeAccountsAction: (AuthMethod, User) => M[LoginResponse]
): M[LoginResponse] = {
  val s = for {
    // Find and validate user
    user <- findUser(email)
      .valueOr(LoginResponse.InvalidCredentials) // If user not found
      .ensure(!_.archived, LoginResponse.Deleted) // If user is archived
    
    // Get auth method
    userAuthMethod = authMethodFromUserId(user.id)
    authMethod <- findAuthMethod(user.id, userAuthMethod.providerName)
      .valueOrF(mergeAccountsAction(userAuthMethod, user)) // If auth method missing, try to merge accounts
  } yield {
    // Generate token or return invalid credentials
    if (checkAuthMethod(authMethod))
      LoginResponse.LoggedIn(issueTokenFor(user))
    else
      LoginResponse.InvalidCredentials
  }
  
  s.run // Execute the computation and return the result
}
```

In this example:
- The helper methods **valueOr** and **ensure** capture common error handling patterns.
- The entire operation is expressed within a for-comprehension, making the logic easy to follow.
- When a value is missing (or the condition fails) the computation "short-circuits" and returns the appropriate error response.

## Composing with Other Monadic Structures

Sealed Monad is designed to work effortlessly with other monadic abstractions such as IO, OptionT, and EitherT from libraries like Cats. Below are examples of such integrations.

### Integrating with Cats Effect IO

When working with Cats Effect's IO, you can lift IO values into the Sealed Monad context using the **seal** and **liftF** methods.

```scala
import cats.effect.IO
import pl.iterators.sealedmonad.syntax._

// Data repository with IO-based operations
trait UserRepository {
  def findById(id: String): IO[Option[User]]
  def save(user: User): IO[User]
}

// Service using Sealed Monad with IO
class UserService(repository: UserRepository) {
  
  sealed trait UserOperationResult
  object UserOperationResult {
    case class Success(user: User) extends UserOperationResult
    case object NotFound extends UserOperationResult
    case object InvalidOperation extends UserOperationResult
  }
  
  def activateUser(id: String): IO[UserOperationResult] = {
    (for {
      // Find the user or return NotFound
      user <- repository.findById(id)
        .valueOr[UserOperationResult](UserOperationResult.NotFound)
      
      // Ensure user isn't already active
      _ <- IO.pure(!user.active)
        .ensure(identity, UserOperationResult.InvalidOperation)
      
      // Activate and save the user
      activatedUser = user.copy(active = true)
      savedUser <- repository.save(activatedUser).seal
    } yield UserOperationResult.Success(savedUser)).run
  }
}
```

### Working with Concurrent Operations

Sealed Monad can be used with concurrent operations, allowing you to compose complex workflows that involve parallel processing:

```scala
import cats.effect.{IO, Resource}
import cats.syntax.parallel._
import pl.iterators.sealedmonad.syntax._

sealed trait OrderProcessingResult
object OrderProcessingResult {
  case class Success(orderId: String) extends OrderProcessingResult
  case object InventoryUnavailable extends OrderProcessingResult
  case object PaymentFailed extends OrderProcessingResult
  case object ShippingUnavailable extends OrderProcessingResult
}

class OrderService(
  inventoryService: InventoryService,
  paymentService: PaymentService,
  shippingService: ShippingService
) {
  
  def processOrder(order: Order): IO[OrderProcessingResult] = {
    val s = for {
      // Check inventory, process payment, and check shipping availability in parallel
      (inventoryOk, paymentOk, shippingOk) <- (
        checkInventory(order.items),
        processPayment(order.payment),
        checkShippingAvailability(order.address)
      ).parMapN((i, p, s) => (i, p, s))
      
      // Validate each result
      _ <- IO.pure(inventoryOk)
        .ensure(identity, OrderProcessingResult.InventoryUnavailable)
      
      _ <- IO.pure(paymentOk)
        .ensure(identity, OrderProcessingResult.PaymentFailed)
      
      _ <- IO.pure(shippingOk)
        .ensure(identity, OrderProcessingResult.ShippingUnavailable)
      
      // Create the final order record
      orderId <- createOrderRecord(order).seal
    } yield OrderProcessingResult.Success(orderId)
    
    s.run
  }
  
  // Helper methods
  private def checkInventory(items: List[OrderItem]): IO[Boolean] = ???
  private def processPayment(payment: Payment): IO[Boolean] = ???
  private def checkShippingAvailability(address: Address): IO[Boolean] = ???
  private def createOrderRecord(order: Order): IO[String] = ???
}
```

## Debugging and Performance Optimization

Sealed Monad includes several operators that help you inspect and trace the flow of computations without altering their results. These operators are especially useful for debugging complex monad chains and for logging.

### Using `inspect` and `tap` for Debugging

You can use the **inspect** method to log intermediate outputs, as well as **tap** and **flatTap** to execute side-effects without disrupting the main computation.

```scala
import cats.effect.IO
import pl.iterators.sealedmonad.syntax._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class AuditingUserService(repository: UserRepository) {
  // Get the logger
  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  
  def findUserWithAuditing(id: String): IO[UserOperationResult] = {
    (for {
      // Find user and audit the lookup attempt
      _ <- logger.info(s"User lookup attempted: $id").seal
      
      user <- repository.findById(id)
        .valueOr[UserOperationResult](UserOperationResult.NotFound)
        // Log the result using inspect
        .inspect {
          case Right(u) => logger.info(s"User found: ${u.email}").unsafeRunSync()
          case Left(UserOperationResult.NotFound) => 
            logger.warn(s"User not found: $id").unsafeRunSync()
        }
      
      // Add audit info without affecting the computation
      _ <- IO.pure(()).flatTap(_ => 
        logger.info(s"User access: $id by ${user.email} at ${java.time.Instant.now}")
      )
    } yield UserOperationResult.Success(user)).run
  }
}
```

### Performance Considerations

While Sealed Monad adds minimal overhead, there are cases where you might want to optimize performance:

```scala
import cats.effect.IO
import pl.iterators.sealedmonad.syntax._

class OptimizedOrderService(repository: OrderRepository) {
  // Example of optimizing for the common case
  def getOrderDetails(orderId: String): IO[OrderResponse] = {
    // For frequently accessed, almost-always-present data, you might
    // check first and only enter the Sealed Monad context if needed
    repository.findById(orderId).flatMap {
      case Some(order) if order.isActive => 
        // Happy path - direct return without Sealed Monad overhead
        IO.pure(OrderResponse.Found(order))
        
      case optOrder => 
        // Use Sealed Monad for the complex error handling path
        (for {
          order <- IO.pure(optOrder)
            .valueOr[OrderResponse](OrderResponse.NotFound)
            .ensure(!_.isDeleted, OrderResponse.Deleted)
            .ensure(_.isActive, OrderResponse.Inactive)
        } yield OrderResponse.Found(order)).run
    }
  }
}
```

## Advanced Use Cases and Common Patterns

In real-world applications, complex business logic often necessitates multiple validation steps, fallback strategies, and integration with external services. Sealed Monad shines in such contexts.

### Access Control with Multiple Levels

Below is an example of an access control service that checks whether a user has access to a resource based on multiple criteria:

```scala
import cats.effect.IO
import pl.iterators.sealedmonad.syntax._

sealed trait AccessControlResult
object AccessControlResult {
  case object Allowed extends AccessControlResult
  final case class Restricted(details: String) extends AccessControlResult
  case object NotAllowed extends AccessControlResult
}

class AccessControlService(
  membershipRepository: MembershipRepository,
  permissionRepository: PermissionRepository
) {
  def resolveAccessControl(
      userId: Long,
      orgId: Long,
      action: String
  ): IO[AccessControlResult] = {
    (for {
      // Check if user is admin (admins bypass normal permission checks)
      isAdmin <- membershipRepository.isAdmin(userId, orgId).seal
      
      // If admin then immediately allow access
      _ <- IO.pure(())
        .ensure(_ => !isAdmin, AccessControlResult.Allowed)
        
      // Check if user is a member
      isMember <- membershipRepository.isMember(userId, orgId).seal
        .ensure(identity, AccessControlResult.NotAllowed)
        
      // Get user's permissions
      permissions <- permissionRepository.getPermissions(userId, orgId)
        .valueOr(AccessControlResult.Restricted("No permissions"))
        
      // Check if user has permission for requested action
      hasPermission = permissions.contains(action)
      _ <- IO.pure(hasPermission)
        .ensure(identity, AccessControlResult.Restricted(s"Missing permission: $action"))
    } yield AccessControlResult.Allowed).run
  }
}
```

### Staged Processing with Recovery Options

For complex workflows that might have multiple fallback options, Sealed Monad allows you to express the logic clearly:

```scala
import cats.effect.IO
import pl.iterators.sealedmonad.syntax._

sealed trait PaymentResult
object PaymentResult {
  case class Success(transactionId: String) extends PaymentResult
  case object PrimaryMethodFailed extends PaymentResult
  case object AllMethodsFailed extends PaymentResult
  case object InsufficientFunds extends PaymentResult
}

class PaymentService(
  creditCardProcessor: CreditCardProcessor,
  paypalProcessor: PayPalProcessor,
  bankTransferProcessor: BankTransferProcessor
) {
  def processPayment(order: Order): IO[PaymentResult] = {
    // Try primary payment method first
    tryPrimaryMethod(order).flatMap {
      case success: PaymentResult.Success => IO.pure(success)
      
      // If primary method fails, try fallback methods
      case _ => tryFallbackMethods(order)
    }
  }
  
  private def tryPrimaryMethod(order: Order): IO[PaymentResult] = {
    (for {
      method <- getPrimaryPaymentMethod(order.userId).seal
      result <- processByMethod(order, method).seal
    } yield result).run
  }
  
  private def tryFallbackMethods(order: Order): IO[PaymentResult] = {
    (for {
      methods <- getFallbackMethods(order.userId).seal
      
      // Try each fallback method until one succeeds
      result <- IO.pure(methods)
        .ensure(_.nonEmpty, PaymentResult.AllMethodsFailed)
        .flatMap { methods =>
          // Try each method sequentially
          methods.foldLeft[Sealed[IO, PaymentResult, PaymentResult]](
            PaymentResult.AllMethodsFailed.seal[IO].rethrow
          ) { (acc, method) =>
            acc.flatMap {
              case success: PaymentResult.Success => success.seal[IO].rethrow
              case _ => processByMethod(order, method).seal
            }
          }
        }
    } yield result).run
  }
  
  // Helper methods
  private def getPrimaryPaymentMethod(userId: Long): IO[PaymentMethod] = ???
  private def getFallbackMethods(userId: Long): IO[List[PaymentMethod]] = ???
  private def processByMethod(order: Order, method: PaymentMethod): IO[PaymentResult] = ???
}
```

## Conclusion

Sealed Monad provides a robust and expressive framework for handling business logic in Scala applications. By embracing for‑comprehension, clear ADT-based error handling, and seamless integration with other functional libraries, developers can craft code that is concise, type‑safe, and easy to maintain.

With these advanced techniques, you can leverage Sealed Monad to simplify complex control flows, avoid common pitfalls, and enhance both debugging and performance in your applications.