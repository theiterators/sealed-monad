---
id: migration-guide
title: Migration Guide
slug: /migration-guide
---

# Migration Guide

This guide provides step-by-step instructions for migrating existing code to use Sealed Monad. Whether you're currently using nested pattern matching, EitherT, or another approach, this guide will help you transition smoothly.

## Migrating from Pattern Matching

If you're currently using nested pattern matching with Options or Either, here's how to migrate to Sealed Monad.

### Before:

```scala
def confirmEmail(
  token: String,
  findAuthMethod: String => Future[Option[AuthMethod]],
  findUser: Long => Future[Option[User]],
  upsertAuthMethod: AuthMethod => Future[Int],
  issueTokenFor: User => String,
  confirmMethod: AuthMethod => AuthMethod
): Future[ConfirmResponse] = {
  import scala.concurrent.ExecutionContext.Implicits.global
  
  findAuthMethod(token).flatMap {
    case None =>
      Future.successful(ConfirmResponse.MethodNotFound)
    case Some(method) =>
      findUser(method.userId).flatMap {
        case None =>
          Future.successful(ConfirmResponse.UserNotFound)
        case Some(user) =>
          upsertAuthMethod(confirmMethod(method)).map { _ =>
            ConfirmResponse.Confirmed(issueTokenFor(user))
          }
      }
  }
}
```

### Migration Steps:

1. **Identify the result ADT**: Make sure you have a sealed trait hierarchy for the responses.

```scala
sealed trait ConfirmResponse
object ConfirmResponse {
  case object MethodNotFound extends ConfirmResponse
  case object UserNotFound extends ConfirmResponse
  final case class Confirmed(token: String) extends ConfirmResponse
}
```

2. **Import the Sealed Monad syntax**: Add the import at the top of your file.

```scala
import pl.iterators.sealedmonad.syntax._
```

3. **Identify the pattern matching branches**: Look for places where you're pattern matching on Option/Either results.

4. **Rewrite using Sealed Monad operators**:
   - Use `valueOr` for Option extraction
   - Use `ensure` for conditional checks
   - Use `attempt` for Either conversion

5. **Structure as a for-comprehension**: Put the steps in a for-comprehension.

6. **Add the .run call**: Complete the computation with `.run`.

### After:

```scala
def confirmEmail(
  token: String,
  findAuthMethod: String => Future[Option[AuthMethod]],
  findUser: Long => Future[Option[User]],
  upsertAuthMethod: AuthMethod => Future[Int],
  issueTokenFor: User => String,
  confirmMethod: AuthMethod => AuthMethod
): Future[ConfirmResponse] = {
  import scala.concurrent.ExecutionContext.Implicits.global
  
  (for {
    // Find auth method or return MethodNotFound
    method <- findAuthMethod(token)
              .valueOr[ConfirmResponse](ConfirmResponse.MethodNotFound)

    // Find user or return UserNotFound
    user <- findUser(method.userId)
            .valueOr[ConfirmResponse](ConfirmResponse.UserNotFound)

    // Update auth method
    _ <- upsertAuthMethod(confirmMethod(method)).seal
  } yield ConfirmResponse.Confirmed(issueTokenFor(user))).run
}
```

## Migrating from Try/Either/Exception Handling

If you're using traditional Try/Either or exception handling, follow these steps:

### Before:

```scala
def processOrder(
  orderId: String,
  orderRepository: OrderRepository,
  paymentService: PaymentService,
  shippingService: ShippingService
): Future[OrderProcessingResult] = {
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.util.{Success, Failure}
  
  orderRepository.findById(orderId).flatMap {
    case None => 
      Future.successful(OrderProcessingResult.NotFound)
    case Some(order) =>
      if (order.isExpired) {
        Future.successful(OrderProcessingResult.Expired)
      } else {
        paymentService.processPayment(order.id, order.amount).flatMap {
          case Success(paymentId) =>
            shippingService.arrangeShipping(order).transform {
              case Success(trackingId) => 
                Success(OrderProcessingResult.Completed(trackingId))
              case Failure(_) => 
                Success(OrderProcessingResult.ShippingFailed)
            }
          case Failure(e: InsufficientFundsException) =>
            Future.successful(OrderProcessingResult.InsufficientFunds)
          case Failure(_) =>
            Future.successful(OrderProcessingResult.PaymentFailed)
        }
      }
  }
}
```

### Migration Steps:

1. **Define your ADT**: Ensure your result type is a sealed trait with all possible outcomes.

2. **Restructure as a linear flow**: Identify the steps and convert to a for-comprehension.

3. **Use appropriate operators**: Replace conditionals with `ensure`, exceptions with `attempt`.

### After:

```scala
def processOrder(
  orderId: String,
  orderRepository: OrderRepository,
  paymentService: PaymentService,
  shippingService: ShippingService
): Future[OrderProcessingResult] = {
  import scala.concurrent.ExecutionContext.Implicits.global
  import pl.iterators.sealedmonad.syntax._
  
  (for {
    // Find order or return NotFound
    order <- orderRepository.findById(orderId)
              .valueOr[OrderProcessingResult](OrderProcessingResult.NotFound)
              
    // Check if order is expired
    _ <- order.pure[Future]
           .ensure(!_.isExpired, OrderProcessingResult.Expired)
           
    // Process payment
    paymentId <- paymentService.processPayment(order.id, order.amount)
                  .attempt {
                    case Success(id) => Right(id)
                    case Failure(e: InsufficientFundsException) => 
                      Left(OrderProcessingResult.InsufficientFunds)
                    case Failure(_) => 
                      Left(OrderProcessingResult.PaymentFailed)
                  }
                  
    // Arrange shipping
    trackingId <- shippingService.arrangeShipping(order)
                   .attempt {
                     case Success(id) => Right(id)
                     case Failure(_) => Left(OrderProcessingResult.ShippingFailed)
                   }
  } yield OrderProcessingResult.Completed(trackingId)).run
}
```

## Gradual Migration Strategy

When migrating a large codebase, consider this gradual approach:

1. **Start with leaf methods**: Begin with methods that don't depend on other methods returning ADTs.

2. **Create ADT wrappers**: For methods you're not ready to migrate, create wrappers that return the appropriate ADT.

```scala
// Original method
def findUser(id: String): Future[Option[User]] = ???

// Wrapper for use with Sealed Monad
def findUserOrError(id: String): Future[UserResponse] =
  findUser(id).map(_.fold[UserResponse](UserResponse.NotFound)(UserResponse.Found))
```

3. **Migrate core business logic first**: Focus on complex business logic with multiple error cases first, as these will benefit most from Sealed Monad.

4. **Update tests**: Make sure to update tests to verify both success and failure paths.

5. **Refactor in small, focused PRs**: Don't try to migrate everything at once. Focus on small, manageable pull requests.

## Mixing Sealed Monad with Other Approaches

During migration, you might need to mix Sealed Monad with existing approaches:

### Integrating with EitherT

```scala
import cats.data.EitherT
import cats.implicits._
import pl.iterators.sealedmonad.Sealed
import pl.iterators.sealedmonad.syntax._

// Convert from EitherT to Sealed
def fromEitherT[F[_], A, B](eitherT: EitherT[F, B, A])(implicit M: Monad[F]): Sealed[F, A, B] =
  Sealed(eitherT.value).rethrow

// Convert from Sealed to EitherT
def toEitherT[F[_]: Monad, A, B](sealed: Sealed[F, A, B]): EitherT[F, B, A] =
  EitherT(sealed.either.run)
```

### Integrating with Option-returning functions

```scala
// When calling external code that returns Option
def callLegacyCode(id: String): Sealed[F, User, UserError] =
  legacyService.findUserById(id).valueOr(UserError.NotFound)
```

## Example: Migrating a Service Class

Here's a complete example of migrating a service class:

### Before:

```scala
class OrderService(
  repository: OrderRepository,
  paymentService: PaymentService,
  notificationService: NotificationService
) {
  import scala.concurrent.ExecutionContext.Implicits.global
  
  def placeOrder(userId: String, items: List[OrderItem]): Future[Either[String, Order]] = {
    if (items.isEmpty) {
      return Future.successful(Left("Order must contain at least one item"))
    }
    
    repository.findActiveCartByUser(userId).flatMap {
      case None => 
        Future.successful(Left("No active cart found"))
      case Some(cart) =>
        if (cart.items.isEmpty) {
          Future.successful(Left("Cart is empty"))
        } else {
          val order = Order(
            id = generateOrderId(), 
            userId = userId,
            items = cart.items,
            status = "pending",
            createdAt = Instant.now()
          )
          
          repository.createOrder(order).flatMap { createdOrder =>
            paymentService.processPayment(userId, calculateTotal(cart.items)).flatMap {
              case Right(paymentId) =>
                val finalOrder = createdOrder.copy(
                  paymentId = Some(paymentId),
                  status = "paid"
                )
                repository.updateOrder(finalOrder).flatMap { updatedOrder =>
                  notificationService.sendOrderConfirmation(updatedOrder)
                    .map(_ => Right(updatedOrder))
                }
              case Left(error) =>
                Future.successful(Left(s"Payment failed: $error"))
            }
          }
        }
    }
  }
  
  private def calculateTotal(items: List[OrderItem]): BigDecimal = 
    items.map(item => item.price * item.quantity).sum
    
  private def generateOrderId(): String = 
    s"ORD-${System.currentTimeMillis()}"
}
```

### After:

```scala
sealed trait OrderResult
object OrderResult {
  case class Success(order: Order) extends OrderResult
  case object EmptyItems extends OrderResult
  case object NoActiveCart extends OrderResult
  case object EmptyCart extends OrderResult
  case class PaymentFailed(reason: String) extends OrderResult
}

class OrderService(
  repository: OrderRepository,
  paymentService: PaymentService,
  notificationService: NotificationService
) {
  import scala.concurrent.ExecutionContext.Implicits.global
  import pl.iterators.sealedmonad.syntax._
  
  def placeOrder(userId: String, items: List[OrderItem]): Future[OrderResult] = {
    (for {
      // Validate items
      _ <- items.pure[Future]
          .ensure(_.nonEmpty, OrderResult.EmptyItems)
      
      // Find active cart
      cart <- repository.findActiveCartByUser(userId)
               .valueOr[OrderResult](OrderResult.NoActiveCart)
      
      // Validate cart items
      _ <- cart.pure[Future]
          .ensure(_.items.nonEmpty, OrderResult.EmptyCart)
      
      // Create order
      order = Order(
        id = generateOrderId(),
        userId = userId,
        items = cart.items,
        status = "pending",
        createdAt = Instant.now()
      )
      
      // Save order
      createdOrder <- repository.createOrder(order).seal
      
      // Process payment
      paymentId <- paymentService.processPayment(userId, calculateTotal(cart.items))
                    .attempt {
                      case Right(id) => Right(id)
                      case Left(error) => Left(OrderResult.PaymentFailed(error))
                    }
      
      // Update order with payment info
      finalOrder = createdOrder.copy(
        paymentId = Some(paymentId),
        status = "paid"
      )
      
      // Save updated order
      updatedOrder <- repository.updateOrder(finalOrder).seal
      
      // Send confirmation notification
      _ <- notificationService.sendOrderConfirmation(updatedOrder).seal
    } yield OrderResult.Success(updatedOrder)).run
  }
  
  private def calculateTotal(items: List[OrderItem]): BigDecimal = 
    items.map(item => item.price * item.quantity).sum
    
  private def generateOrderId(): String = 
    s"ORD-${System.currentTimeMillis()}"
}
```

## Final Checklist

Before considering a migration complete, ensure you've:

- [ ] Imported `pl.iterators.sealedmonad.syntax._` wherever needed
- [ ] Replaced all pattern matching with Sealed Monad operators
- [ ] Added `.run` to execute computations
- [ ] Updated tests to verify both success and error paths
- [ ] Updated documentation to reflect the new approach
- [ ] Reviewed for readability and consistency

With these steps, you should be able to successfully migrate to Sealed Monad and enjoy cleaner, more maintainable error handling in your code.

## Migrating from EitherT

If you're currently using Cats' EitherT, here's how to migrate to Sealed Monad.

### Before:

```scala
def confirmEmail(
  token: String,
  findAuthMethod: String => Future[Option[AuthMethod]],
  findUser: Long => Future[Option[User]],
  upsertAuthMethod: AuthMethod => Future[Int],
  issueTokenFor: User => String,
  confirmMethod: AuthMethod => AuthMethod
): Future[ConfirmResponse] = {
  import scala.concurrent.ExecutionContext.Implicits.global
  import cats.implicits._
  
  val userT = for {
    method <- EitherT.fromOptionF(
                findAuthMethod(token),
                ifNone = ConfirmResponse.MethodNotFound
              )
    user <- EitherT.fromOptionF(
              findUser(method.userId),
              ifNone = ConfirmResponse.UserNotFound
            )
  } yield (method, user)

  userT.semiflatMap { case (method, user) =>
    upsertAuthMethod(confirmMethod(method))
      .map(_ => ConfirmResponse.Confirmed(issueTokenFor(user)))
  }.merge
}
```

### Migration Steps:

1. **Import the Sealed Monad syntax**: Add the import at the top of your file.

```scala
import pl.iterators.sealedmonad.syntax._
```

2. **Replace EitherT operations with Sealed Monad equivalents**:

   - `EitherT.fromOptionF(opt, ifNone)` → `opt.valueOr(ifNone)`
   - `EitherT.cond(test, right, left)` → `right.pure[F].ensure(_ => test, left)`
   - `eitherT.semiflatMap` → `sealed.flatMap` or `sealed.flatTap`
   - `.merge` → `.run`

3. **Structure as a for-comprehension**: Put the steps in a for-comprehension.

4. **Add the .run call**: Complete the computation with `.run`.

### After:

```scala
def confirmEmail(
  token: String,
  findAuthMethod: String => Future[Option[AuthMethod]],
  findUser: Long => Future[Option[User]],
  upsertAuthMethod: AuthMethod => Future[Int],
  issueTokenFor: User => String,
  confirmMethod: AuthMethod => AuthMethod
): Future[ConfirmResponse] = {
  import scala.concurrent.ExecutionContext.Implicits.global
  import pl.iterators.sealedmonad.syntax._
  
  (for {
    method <- findAuthMethod(token)
              .valueOr[ConfirmResponse](ConfirmResponse.MethodNotFound)

    user <- findUser(method.userId)
            .valueOr[ConfirmResponse](ConfirmResponse.UserNotFound)

    _ <- upsertAuthMethod(confirmMethod(method)).seal
  } yield ConfirmResponse.Confirmed(issueTokenFor(user))).run
}
```