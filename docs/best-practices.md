---
sidebar_position: 5
---

# Best Practices

This guide offers patterns and best practices for using Sealed Monad effectively in your applications.

## Designing Your ADTs

The foundation of a good Sealed Monad implementation is a well-designed ADT (Algebraic Data Type).

### ✅ Do: Create Specific, Descriptive Response Types

```scala
sealed trait PaymentResponse
object PaymentResponse {
  case class Success(transactionId: String) extends PaymentResponse
  case object InsufficientFunds extends PaymentResponse
  case object CardExpired extends PaymentResponse
  case object CardDeclined extends PaymentResponse
  case object PaymentMethodNotFound extends PaymentResponse
  case object ServiceUnavailable extends PaymentResponse
}
```

### ❌ Don't: Use Generic Error Types

```scala
// Too generic
sealed trait Response
object Response {
  case class Success(data: Any) extends Response
  case class Error(message: String) extends Response
}
```

### ✅ Do: Group Related Responses in Modules

For complex systems with many operations, organize related responses together:

```scala
object Payments {
  sealed trait Response

  object Authentication {
    case object InvalidCredentials extends Response
    case object SessionExpired extends Response
  }

  object Processing {
    case class Success(id: String) extends Response
    case object InsufficientFunds extends Response
    case object CardDeclined extends Response
  }
}
```

## Structuring Your Code

### ✅ Do: Separate High-Level Flow from Implementation Details

Create a clear hierarchical structure:

```scala
def processOrder(orderId: String): F[OrderResponse] = {
  (for {
    order    <- findAndValidateOrder(orderId)
    payment  <- processPayment(order)
    shipping <- arrangeShipping(order, payment)
  } yield OrderResponse.Completed(shipping.trackingId)).run
}

private def findAndValidateOrder(id: String): Sealed[F, Order, OrderResponse] = {
  // Implementation details here
}

private def processPayment(order: Order): Sealed[F, Payment, OrderResponse] = {
  // Implementation details here
}

private def arrangeShipping(order: Order, payment: Payment): Sealed[F, Shipping, OrderResponse] = {
  // Implementation details here
}
```

### ❌ Don't: Mix Business Logic with Technical Details

Avoid mixing different levels of abstraction:

```scala
// Too much detail in the main flow
def processOrder(orderId: String): F[OrderResponse] = {
  (for {
    orderOpt <- orderRepository.findById(orderId).seal
    order <- orderOpt.valueOr(OrderResponse.NotFound)
    _ <- order.pure[F].ensure(!_.isExpired, OrderResponse.Expired)
    _ <- paymentService.processPayment(order.total).ensure(_.isSuccessful, OrderResponse.PaymentFailed)
    tracking <- shippingService.ship(order).attemptF(_.fold(
      error => OrderResponse.ShippingFailed.pure[F],
      tracking => OrderResponse.Completed(tracking).pure[F]
    ))
  } yield tracking).run
}
```

### ✅ Do: Use Method Names as Documentation

Choose method names that describe business operations clearly:

```scala
def registerUser(request: RegisterRequest): F[RegisterResponse] = {
  (for {
    email     <- validateEmail(request.email)
    password  <- validatePassword(request.password)
    user      <- createUserAccount(email, password)
    _         <- sendWelcomeEmail(user)
  } yield RegisterResponse.Success(user.id)).run
}
```

## Working with Options and Either

### ✅ Do: Use valueOr for Option Extraction

```scala
userRepository.findById(userId)  // F[Option[User]]
  .valueOr[UserResponse](UserResponse.NotFound)  // Sealed[F, User, UserResponse]
```

### ✅ Do: Chain Validations Fluently

```scala
userRepository.findById(userId)
  .valueOr[UserResponse](UserResponse.NotFound)  // If user doesn't exist
  .ensure(!_.isDeleted, UserResponse.AccountDeleted)  // If account is deleted
  .ensure(_.isActive, UserResponse.AccountInactive)  // If account isn't active
```

### ✅ Do: Use ensure for Conditional Validation

```scala
product.pure[F]
  .ensure(_.inStock, ProductResponse.OutOfStock)
  .ensure(_.price <= maxPrice, ProductResponse.PriceExceedsLimit)
```

### ✅ Do: Use attempt for More Complex Validations

```scala
validateAddress(address).attempt {
  case Right(validatedAddress) => Right(validatedAddress)
  case Left(AddressError.InvalidZipCode) => Left(UserResponse.InvalidZipCode)
  case Left(AddressError.UnknownCity) => Left(UserResponse.UnknownCity)
  case Left(_) => Left(UserResponse.InvalidAddress)
}
```

### ✅ Do: Consider using valueOrF for effectful fallbacks

```scala
userRepo.findById(id).valueOrF(
  fallbackUserService.findById(id)
)
```

## Side Effects and Debugging

### ✅ Do: Use tap for Debugging

```scala
def processOrder(orderId: String): F[OrderResponse] = {
  (for {
    order <- findOrder(orderId)
      .tap(order => logger.debug(s"Found order: $order"))
    payment <- processPayment(order)
      .tap(payment => logger.info(s"Payment processed: ${payment.id}"))
  } yield OrderResponse.Success).run
}
```

### ✅ Do: Use inspect for Comprehensive Logging

```scala
sealed.inspect {
  case Right(value) => logger.info(s"Success: $value")
  case Left(error) => logger.warn(s"Failed: $error")
}
```

## Testing

### ✅ Do: Test Each Step Independently

```scala
"findAndValidateUser" should "return user when valid" in {
  val result = service.findAndValidateUser("valid@example.com").run
  result shouldBe Right(expectedUser)
}

"findAndValidateUser" should "return UserNotFound when user doesn't exist" in {
  val result = service.findAndValidateUser("missing@example.com").run
  result shouldBe Left(UserResponse.NotFound)
}
```

## Migration Strategies

If you're migrating an existing codebase to Sealed Monad, consider these approaches:

### Gradual Adoption

1. **Start with new code**: Apply Sealed Monad to new features first
2. **Identify pain points**: Target existing code with complex error handling or nested pattern matching
3. **Refactor incrementally**: Convert one method or service at a time
4. **Add comprehensive tests**: Ensure behavior remains the same after refactoring

### Example Refactoring Path

1. Define your ADT for the operation result
2. Identify points where control flow branches (pattern matching, if/else)
3. Convert those to Sealed Monad operations (valueOr, ensure, etc.)
4. Wrap the body in a for-comprehension
5. Add a .run call at the end
6. Extract complex validation logic to helper methods

## Common Pitfalls

### ❌ Don't: Forget to call .run

```scala
// This doesn't actually execute the computation!
def processOrder(id: String): F[OrderResponse] = {
  for {
    order <- orderRepository.findById(id).valueOr(OrderResponse.NotFound)
    // ...more processing
  } yield OrderResponse.Success(order.id)
}

// You must call .run to execute
def processOrder(id: String): F[OrderResponse] = {
  (for {
    order <- orderRepository.findById(id).valueOr(OrderResponse.NotFound)
    // ...more processing
  } yield OrderResponse.Success(order.id)).run
}
```

### ❌ Don't: Use overly complex transformations

If you find yourself writing complex transformations, consider breaking them down into smaller, focused methods:

```scala
// Too complex
def processOrder(id: String): F[OrderResponse] = {
  (for {
    order <- orderRepository.findById(id).valueOr(OrderResponse.NotFound)
    _ <- paymentService.process(order.payment).attempt {
      case Right(_) => Right(())
      case Left(PaymentError.InsufficientFunds) => Left(OrderResponse.InsufficientFunds)
      case Left(PaymentError.PaymentDeclined) => Left(OrderResponse.PaymentDeclined)
      case Left(_) => Left(OrderResponse.PaymentFailed)
    }
  } yield OrderResponse.Success(order.id)).run
}

// Better: Extract complex logic
def processOrder(id: String): F[OrderResponse] = {
  (for {
    order <- findOrder(id)
    _ <- processPayment(order.payment)
  } yield OrderResponse.Success(order.id)).run
}

private def findOrder(id: String): Sealed[F, Order, OrderResponse] =
  orderRepository.findById(id).valueOr(OrderResponse.NotFound)

private def processPayment(payment: Payment): Sealed[F, Unit, OrderResponse] =
  paymentService.process(payment).attempt {
    case Right(_) => Right(())
    case Left(PaymentError.InsufficientFunds) => Left(OrderResponse.InsufficientFunds)
    case Left(PaymentError.PaymentDeclined) => Left(OrderResponse.PaymentDeclined)
    case Left(_) => Left(OrderResponse.PaymentFailed)
  }
```

## Real-World Example: API Service

Here's a complete example of an API service using Sealed Monad:

```scala
class UserService[F[_]: Monad](
  repository: UserRepository[F],
  emailService: EmailService[F],
  securityService: SecurityService[F]
) {
  import pl.iterators.sealedmonad.syntax._

  def register(request: RegisterRequest): F[RegisterResponse] = {
    (for {
      // Validate email format and availability
      email <- validateEmail(request.email)

      // Validate password strength
      _ <- securityService.validatePassword(request.password)
             .ensure(identity, RegisterResponse.PasswordTooWeak)

      // Create user account
      user <- createUser(email, request.password, request.name)

      // Send welcome email
      _ <- emailService.sendWelcome(user.email)
             .attemptF(handleEmailError)
    } yield RegisterResponse.Success(user.id)).run
  }

  private def validateEmail(email: String): Sealed[F, String, RegisterResponse] = {
    val isValidFormat = EmailValidator.isValid(email)

    for {
      // Check email format
      _ <- isValidFormat.pure[F]
             .ensure(identity, RegisterResponse.InvalidEmailFormat)

      // Check if email is already taken
      exists <- repository.emailExists(email)
      _ <- exists.pure[F]
             .ensure(!identity, RegisterResponse.EmailAlreadyExists)
    } yield email
  }

  private def createUser(
    email: String,
    password: String,
    name: String
  ): Sealed[F, User, RegisterResponse] = {
    repository
      .create(User(email, password, name))
      .attempt {
        case Right(user) => Right(user)
        case Left(_) => Left(RegisterResponse.RegistrationFailed)
      }
  }

  private def handleEmailError(
    error: EmailError
  ): F[Either[RegisterResponse, Unit]] = {
    // Log the error but don't fail registration
    Monad[F].pure(Right(()))
  }
}
```

This example demonstrates many Sealed Monad best practices:

- Clear separation of high-level flow and implementation details
- Well-designed ADT for response types
- Breaking validation into smaller, focused methods
- Using a variety of operators for different situations
- Proper error handling and conversion
