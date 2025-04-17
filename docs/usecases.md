---
id: usecases
title: Practical Use Cases
slug: /usecases
---

# Practical Use Cases

Sealed Monad shines in real-world business logic scenarios. This section demonstrates practical examples showing how Sealed Monad improves readability and maintainability in different contexts.

> **Note:** You can find complete, runnable versions of these examples in the [examples directory](https://github.com/theiterators/sealed-monad/tree/master/examples/src).


## Use Case 1: Form Validation & Processing

When processing form input, multiple validations often need to be performed before proceeding. Sealed Monad provides a clean way to express this workflow.

### The Response ADT

```scala
sealed trait RegistrationResponse
object RegistrationResponse {
  case class Success(userId: String) extends RegistrationResponse
  case object EmailInvalid extends RegistrationResponse
  case object EmailTaken extends RegistrationResponse
  case object PasswordTooWeak extends RegistrationResponse
  case object AddressInvalid extends RegistrationResponse
}
```

### Implementation with Sealed Monad

```scala
import pl.iterators.sealedmonad.syntax._

def registerUser(
  email: String,
  password: String,
  address: Address,
  validateEmail: String => Future[Boolean],
  checkEmailExists: String => Future[Boolean],
  validatePassword: String => Future[Boolean],
  validateAddress: Address => Future[Boolean],
  createUser: (String, String, Address) => Future[User]
): Future[RegistrationResponse] = {
  (for {
    // Validate email format
    _ <- validateEmail(email)
          .ensure(isValid => isValid, RegistrationResponse.EmailInvalid)
    
    // Check if email is already taken
    emailExistsCheck <- checkEmailExists(email).seal
    _ <- (!emailExistsCheck).pure[Future]
          .ensure(notTaken => notTaken, RegistrationResponse.EmailTaken)
    
    // Validate password strength
    _ <- validatePassword(password)
          .ensure(isStrong => isStrong, RegistrationResponse.PasswordTooWeak)
    
    // Validate address
    _ <- validateAddress(address)
          .ensure(isValid => isValid, RegistrationResponse.AddressInvalid)
    
    // Create user
    user <- createUser(email, password, address).seal
  } yield RegistrationResponse.Success(user.id)).run
}
```

What makes this approach powerful:
1. **Clear sequence** - Each validation step is explicit and in logical order
2. **Early return** - Any validation failure short-circuits the entire computation
3. **Flat structure** - No nested indentation, improving readability
4. **Self-documenting** - The code clearly represents the business process

## Use Case 2: Structuring Complex Services

For complex business logic, Sealed Monad enables a tiered approach with clear abstraction layers.

### The Response ADT

```scala
sealed trait OrderProcessingResponse
object OrderProcessingResponse {
  case class Success(orderId: String) extends OrderProcessingResponse
  case object UserNotFound extends OrderProcessingResponse
  case object ProductNotFound extends OrderProcessingResponse
  case object InsufficientStock extends OrderProcessingResponse
  case object PaymentFailed extends OrderProcessingResponse
  case object ShippingUnavailable extends OrderProcessingResponse
}
```

### Tiered Service Implementation

```scala
import pl.iterators.sealedmonad.syntax._
import pl.iterators.sealedmonad.Sealed

class OrderService[F[_]: Monad](
  userRepository: UserRepository[F],
  productRepository: ProductRepository[F],
  paymentService: PaymentService[F],
  shippingService: ShippingService[F],
  orderRepository: OrderRepository[F]
) {
  // High-level business flow
  def processOrder(userId: String, items: List[OrderItem]): F[OrderProcessingResponse] = {
    (for {
      user      <- findUser(userId)
      products  <- validateProducts(items)
      payment   <- processPayment(user, products)
      shipping  <- arrangeShipping(user, products)
      order     <- createOrder(user, items, payment, shipping)
    } yield OrderProcessingResponse.Success(order.id)).run
  }

  // Mid-level methods with focused responsibilities
  private def findUser(userId: String): Sealed[F, User, OrderProcessingResponse] = 
    userRepository.findById(userId)
      .valueOr(OrderProcessingResponse.UserNotFound)

  private def validateProducts(items: List[OrderItem]): Sealed[F, List[Product], OrderProcessingResponse] = {
    // Implementation to look up and validate each product
    Monad[F].pure(List.empty[Product]).seal  // Simplified
  }

  private def processPayment(
    user: User, 
    products: List[Product]
  ): Sealed[F, String, OrderProcessingResponse] = {
    // Implementation to process payment
    Monad[F].pure("payment-123").seal  // Simplified
  }

  private def arrangeShipping(
    user: User, 
    products: List[Product]
  ): Sealed[F, String, OrderProcessingResponse] = {
    // Implementation to arrange shipping
    Monad[F].pure("shipping-456").seal  // Simplified
  }

  private def createOrder(
    user: User, 
    items: List[OrderItem],
    paymentId: String,
    shippingId: String
  ): Sealed[F, Order, OrderProcessingResponse] = {
    // Implementation to create and store the order
    Monad[F].pure(Order("order-789", user.id, items)).seal  // Simplified
  }
}
```

Benefits of this structure:
1. **High-level readability** - The main flow consists of descriptive steps
2. **Progressive disclosure** - Implementation details are encapsulated in well-named methods
3. **Separation of concerns** - Each step handles a specific part of the business logic
4. **Testability** - Each step can be tested independently

## Key Takeaways

These examples demonstrate how Sealed Monad can improve your code by:

1. **Flattening nested logic** - No more deeply indented conditional blocks
2. **Providing clear failure paths** - Each validation step maps to a specific response
3. **Enabling abstraction layers** - From high-level flows to implementation details
4. **Standardizing error handling** - Consistent approach across different operations

For more examples and detailed explanations, see the [Best Practices](best-practices) sections.
