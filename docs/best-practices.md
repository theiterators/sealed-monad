---
id: best-practices
title: Best Practices
slug: /best-practices
---

This document gathers best practices for leveraging the Sealed Monad pattern in your Scala applications. By following these guidelines, you'll create domain‐rich, type‐safe code that communicates intent clearly and minimizes runtime surprises.

## 1. Designing Your ADTs

The foundation of effective Sealed Monad usage is a well-designed ADT (Algebraic Data Type) for operation results.

### ✅ Create Specific, Descriptive Response Types

Design ADTs that capture all possible outcomes of an operation clearly:

```scala
sealed trait UserRegistrationResult
object UserRegistrationResult {
  final case class Success(userId: String) extends UserRegistrationResult
  case object EmailAlreadyExists extends UserRegistrationResult
  case object InvalidEmail extends UserRegistrationResult
  case object PasswordTooWeak extends UserRegistrationResult
  case object UsernameInvalid extends UserRegistrationResult
}
```

### ❌ Avoid Generic Error Types

Avoid overly general result types that hide the specifics of different outcomes:

```scala
// Too generic - avoid this
sealed trait Result
object Result {
  case class Success(data: Any) extends Result
  case class Error(message: String) extends Result
}
```

### ✅ Model Domain-Specific Edge Cases

Include edge cases that are meaningful in your domain:

```scala
sealed trait PaymentResult
object PaymentResult {
  final case class Success(transactionId: String) extends PaymentResult
  case object InsufficientFunds extends PaymentResult
  case object CardDeclined extends PaymentResult
  case object PaymentMethodExpired extends PaymentResult
  case object FraudDetected extends PaymentResult  // Domain-specific case
  case object ProcessorUnavailable extends PaymentResult
}
```

## 2. Structuring Your Code

The way you organize your code impacts its readability and maintainability.

### ✅ Separate High-Level Flow from Implementation Details

Create a clear hierarchical structure with high-level flows and focused helper methods:

```scala
import pl.iterators.sealedmonad.syntax._
import pl.iterators.sealedmonad.Sealed
import cats.effect.IO

class OrderService(
  userRepository: UserRepository,
  productRepository: ProductRepository,
  paymentService: PaymentService,
  orderRepository: OrderRepository
) {
  
  // High-level flow - clear and concise
  def processOrder(userId: String, items: List[OrderItem]): IO[OrderResult] = {
    (for {
      user     <- findAndValidateUser(userId)
      products <- validateProductsAvailability(items)
      payment  <- processPayment(user, products)
      order    <- createOrder(user, products, payment)
    } yield OrderResult.Success(order.id)).run
  }
  
  // Mid-level methods with focused responsibilities
  private def findAndValidateUser(userId: String): Sealed[IO, User, OrderResult] = {
    // Implementation details here
    userRepository.findById(userId)
      .valueOr[OrderResult](OrderResult.UserNotFound)
      .ensure(user => user.isActive, OrderResult.UserInactive)
  }
  
  private def validateProductsAvailability(items: List[OrderItem]): Sealed[IO, List[Product], OrderResult] = {
    // Implementation details here
    items.traverse { item =>
      productRepository.findById(item.productId)
        .valueOr[OrderResult](OrderResult.ProductNotFound(item.productId))
        .ensure(p => p.stock >= item.quantity, OrderResult.InsufficientStock(item.productId))
    }.seal
  }
  
  private def processPayment(user: User, products: List[Product]): Sealed[IO, String, OrderResult] = {
    // Implementation details here
    val amount = calculateTotal(products)
    paymentService.processPayment(user.id, amount)
      .attempt {
        case Right(transactionId) => Right(transactionId)
        case Left(PaymentError.InsufficientFunds) => Left(OrderResult.PaymentFailed("Insufficient funds"))
        case Left(PaymentError.CardDeclined) => Left(OrderResult.PaymentFailed("Card declined"))
        case Left(_) => Left(OrderResult.PaymentFailed("Unknown payment error"))
      }
  }
  
  private def createOrder(user: User, products: List[Product], paymentId: String): Sealed[IO, Order, OrderResult] = {
    // Implementation details here
    orderRepository.create(user.id, products.map(p => OrderItem(p.id, 1)), paymentId).seal
  }
  
  private def calculateTotal(products: List[Product]): BigDecimal = 
    products.map(_.price).sum
}
```


### ❌ Don't: Expose Sealed Monad in Public Interfaces

Sealed Monad should never be exposed in the public interface of your module or service. It's designed for internal processing and error handling only.

```scala
// ❌ DON'T expose Sealed Monad in your public API
def create(auth: AuthContext, orgId: OrganizationId): Sealed[IO, Roadmap, RoadmapCreateResult]

// ✅ DO return the effect type with your ADT directly
def create(auth: AuthContext, orgId: OrganizationId): IO[RoadmapCreateResult]
```
In a real service implementation, keep Sealed Monad internal:

```scala
class RoadmapService {
  // Define your Step type alias for better readability
  private type Step[A] = Sealed[IO, A, RoadmapCreateResult]
  
  // Public interface returns IO[Result], not Sealed[IO, _, _]
  def create(
    auth: AuthContext, 
    orgId: OrganizationId,
    request: RoadmapCreateRequest
  ): IO[RoadmapCreateResult] =
    (for {
      _            <- checkAccessToOrganization(auth, orgId)
      organization <- findOrganization(orgId)
      roadmap      <- createRoadmap(organization, auth.id, request)
    } yield RoadmapCreateResult.Created(roadmap)).run
    
  // Private methods use the Step type alias
  private def checkAccessToOrganization(auth: AuthContext, id: OrganizationId): Sealed[IO, Boolean, RoadmapCreateResult] = ...
  private def findOrganization(id: OrganizationId): Sealed[IO, Organization, RoadmapCreateResult] = ...
  private def createRoadmap(org: Organization, userId: UserId, request: RoadmapCreateRequest): Sealed[IO, Roadmap, RoadmapCreateResult] = ...
}
```
By keeping Sealed Monad as an implementation detail, you maintain cleaner module boundaries and avoid leaking implementation details to your API consumers.

### ❌ Don't: Mix Business Logic with Technical Details

Avoid mixing different levels of abstraction:

```scala
// Too much detail in the main flow - avoid this
def processOrder(orderId: String): IO[OrderResult] = {
  (for {
    orderOpt <- orderRepository.findById(orderId).seal
    order <- orderOpt.valueOr(OrderResult.NotFound)
    _ <- order.pure[IO].ensure(!_.isExpired, OrderResult.Expired)
    _ <- paymentService.processPayment(order.total).ensure(_.isSuccessful, OrderResult.PaymentFailed)
    _ <- emailService.sendConfirmation(order.userId, order.id).attempt {
      case Right(_) => Right(())
      case Left(_) => Left(OrderResult.EmailFailed)
    }
    tracking <- shippingService.ship(order).seal
  } yield OrderResult.Success(tracking)).run
}
```

### ✅ Use Method Names as Documentation

Choose method names that describe business operations clearly:

```scala
def registerUser(request: RegisterRequest): IO[RegisterResponse] = {
  (for {
    email     <- validateEmail(request.email)
    password  <- validatePassword(request.password)
    user      <- createUserAccount(email, password)
    _         <- sendWelcomeEmail(user)
  } yield RegisterResponse.Success(user.id)).run
}
```

## 3. Working with Options and Either

Sealed Monad provides elegant ways to work with Option and Either types.

### ✅ Use valueOr for Option Extraction

```scala
// Find a user by ID or return NotFound
userRepository.findById(userId)  // IO[Option[User]]
  .valueOr[UserResponse](UserResponse.NotFound)  // Sealed[IO, User, UserResponse]
```

### ✅ Chain Validations Fluently

```scala
// Multiple validations in sequence
userRepository.findById(userId)
  .valueOr[UserResponse](UserResponse.NotFound)  // If user doesn't exist
  .ensure(!_.archived, UserResponse.AccountDeleted)  // If account is deleted
  .ensure(_.isActive, UserResponse.AccountInactive)  // If account isn't active
```

### ✅ Use ensure for Conditional Validation

```scala
// Validate a product's availability
product.pure[IO]
  .ensure(_.inStock, ProductResponse.OutOfStock)
  .ensure(_.price <= maxPrice, ProductResponse.PriceExceedsLimit)
```

### ✅ Use attempt for Complex Transformations

```scala
// Handle different validation errors differently
validateAddress(address).attempt {
  case Right(validatedAddress) => Right(validatedAddress)
  case Left(AddressError.InvalidZipCode) => Left(UserResponse.InvalidZipCode)
  case Left(AddressError.UnknownCity) => Left(UserResponse.UnknownCity)
  case Left(_) => Left(UserResponse.InvalidAddress)
}
```

## 4. Side Effects and Debugging

Sealed Monad provides several operators for handling side effects without disrupting your main computation.

### ✅ Use tap for Debugging and Logging

```scala
def processOrder(orderId: String): IO[OrderResponse] = {
  (for {
    order <- findOrder(orderId)
      .tap(order => logger.debug(s"Found order: $order"))
    payment <- processPayment(order)
      .tap(payment => logger.info(s"Payment processed: ${payment.id}"))
  } yield OrderResponse.Success(order.id)).run
}
```


### ✅ Use valueOrF, ensureF, attemptF for Logging with Cats Effect

The **valueOrF** operator extends the valueOr pattern with effectful error handling. Here, when a job can't be found, we log the error before returning the failure case. This allows for better observability while preserving your typed error channel.


```scala
private def findJob(jobName: String): Step[Job] =
  jobService.findJob(jobName)
    .valueOrF(Logger[IO].error(s"Unable to find job $jobName").as(JobResult.JobNotFound))
```

The **ensureF** operator combines validation with effectful error handling. In this example, we ensure the query results are non-empty, and if they aren't, we log an error message before returning the appropriate domain error. This pattern is perfect when you need to validate results and provide context about the failure.

```scala
private def listJobs(filters: Seq[JobFilter]): JobStep[Seq[Job]] =
  jobRepository
    .list(filters)
    .ensureF(
      _.nonEmpty,
      Logger[IO]
        .error("No matching job found for request")
        .as(JobResult.NotFound)
    )
```

The **attemptF** operator is useful when you need to perform effects (like logging) during error handling. In this example, when a payment operation fails, we log the specific error before returning a typed error result. This provides rich context about failures while keeping your error handling clean and maintaining your typed error channel.


```scala
private def processJobPayment(job: Job): StepIO[PaymentConfirmation] =
  paymentService.process(job.cost).seal.attemptF {
    case PaymentResult.Success(confirmation) => IO.pure(Right(confirmation))
    case PaymentResult.Declined => 
      Logger[IO]
        .error(s"Payment declined for job ${job.id}")
        .as(Left(JobResult.PaymentFailed("Payment declined")))
    case PaymentResult.InsufficientFunds => 
      Logger[IO]
        .error(s"Insufficient funds for job ${job.id}")
        .as(Left(JobResult.InsufficientFunds))
    case PaymentResult.ProcessingError(error) => 
      Logger[IO]
        .error(s"Payment processing error for job ${job.id}: $error")
        .as(Left(JobResult.PaymentFailed(error)))
  }
```

### ✅ Use inspect for Comprehensive Logging

```scala
// Log different outcomes differently
userValidation.inspect {
  case Right(user) => logger.info(s"User validated: ${user.email}")
  case Left(error: ValidationError.InvalidEmail) => 
    logger.warn(s"Invalid email format: ${error.email}")
  case Left(error) => 
    logger.error(s"Validation failed: $error")
}
```

### ✅ Use flatTap for Effectful Side Operations

```scala
// Perform side effects without affecting the main computation
user.flatTap(u => 
  auditService.recordAccess(u.id, AccessType.Login)
)
```

## 5. Testing Strategies

Proper testing ensures your Sealed Monad code works as expected.

### ✅ Test All Possible Outcomes

For each ADT case, write tests that ensure the correct outcome is produced:

```scala
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.unsafe.implicits.global

class UserServiceSpec extends AnyFlatSpec with Matchers {
  
  "UserService.registerUser" should "return Success when registration succeeds" in {
    val service = new UserService(
      emailValidator = _ => IO.pure(true),
      emailRepository = _ => IO.pure(false), // email doesn't exist
      passwordValidator = _ => IO.pure(true),
      userRepository = (_, _) => IO.pure(User("user-123", "test@example.com"))
    )
    
    val result = service.registerUser(RegisterRequest("test@example.com", "password123"))
      .unsafeRunSync()
      
    result shouldBe RegisterResponse.Success("user-123")
  }
  
  it should "return EmailInvalid when email is invalid" in {
    val service = new UserService(
      emailValidator = _ => IO.pure(false), // invalid email
      emailRepository = _ => IO.pure(false),
      passwordValidator = _ => IO.pure(true),
      userRepository = (_, _) => IO.pure(User("user-123", "test@example.com"))
    )
    
    val result = service.registerUser(RegisterRequest("invalid", "password123"))
      .unsafeRunSync()
      
    result shouldBe RegisterResponse.EmailInvalid
  }
  
  // Additional tests for other outcomes
}
```

### ✅ Test Helper Methods Independently

Test your helper methods separately to ensure they work correctly in isolation:

```scala
"UserService.validateEmail" should "return valid email when format is correct and email doesn't exist" in {
  val service = new UserService(/* ... */)
  
  val result = service.validateEmail("valid@example.com")
    .run.unsafeRunSync()
    
  result shouldBe Right("valid@example.com")
}
```

## 6. Common Pitfalls and How to Avoid Them

Being aware of common mistakes will help you use Sealed Monad more effectively.

### ❌ Don't: Forget to call .run

```scala
// This doesn't actually execute the computation!
def processOrder(id: String): IO[OrderResponse] = {
  for {
    order <- orderRepository.findById(id).valueOr(OrderResponse.NotFound)
    // ...more processing
  } yield OrderResponse.Success(order.id)
}

// Correct way - don't forget .run
def processOrder(id: String): IO[OrderResponse] = {
  (for {
    order <- orderRepository.findById(id).valueOr(OrderResponse.NotFound)
    // ...more processing
  } yield OrderResponse.Success(order.id)).run
}
```

### ❌ Don't: Mix Monad Transformers with Sealed Monad

```scala
// Don't mix EitherT with Sealed Monad
def processUser(id: String): IO[UserResponse] = {
  val eitherT = EitherT(userRepository.findById(id).map {
    case Some(user) => Right(user)
    case None => Left(UserResponse.NotFound)
  })
  
  // This mixing makes code harder to follow
  val sealed = for {
    user <- eitherT.value.fromEither
    // ...more operations with Sealed Monad
  } yield UserResponse.Success(user)
  
  sealed.run
}

// Better: stick with one approach
def processUser(id: String): IO[UserResponse] = {
  (for {
    user <- userRepository.findById(id).valueOr(UserResponse.NotFound)
    // ...more operations with Sealed Monad
  } yield UserResponse.Success(user)).run
}
```

### ❌ Don't: Use overly complex transformations

If you find yourself writing complex transformations, consider breaking them down into smaller, focused methods:

```scala
// Too complex
def processOrder(id: String): IO[OrderResponse] = {
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
def processOrder(id: String): IO[OrderResponse] = {
  (for {
    order <- findOrder(id)
    _ <- processPayment(order.payment)
  } yield OrderResponse.Success(order.id)).run
}

private def findOrder(id: String): Sealed[IO, Order, OrderResponse] =
  orderRepository.findById(id).valueOr(OrderResponse.NotFound)

private def processPayment(payment: Payment): Sealed[IO, Unit, OrderResponse] =
  paymentService.process(payment).attempt {
    case Right(_) => Right(())
    case Left(PaymentError.InsufficientFunds) => Left(OrderResponse.InsufficientFunds)
    case Left(PaymentError.PaymentDeclined) => Left(OrderResponse.PaymentDeclined)
    case Left(_) => Left(OrderResponse.PaymentFailed)
  }
```

## 7. Migration Strategies

If you're migrating an existing codebase to use Sealed Monad, here are some recommended approaches:

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

## 8. Real-World Example: API Service

Here's a complete example of an API service using Sealed Monad:

```scala
import cats.effect.IO
import pl.iterators.sealedmonad.syntax._
import io.circe.generic.auto._

// Domain models
case class User(id: String, email: String, name: String, active: Boolean)
case class RegisterRequest(email: String, password: String, name: String)

// Result ADT
sealed trait RegisterResponse
object RegisterResponse {
  case class Success(userId: String) extends RegisterResponse
  case object EmailAlreadyExists extends RegisterResponse
  case object InvalidEmailFormat extends RegisterResponse
  case object PasswordTooWeak extends RegisterResponse
}

class UserService(
  emailValidator: EmailValidator,
  passwordValidator: PasswordValidator,
  userRepository: UserRepository
) {
  import pl.iterators.sealedmonad.syntax._

  def register(request: RegisterRequest): IO[RegisterResponse] = {
    (for {
      // Validate email format
      _ <- emailValidator.isValid(request.email)
           .ensure(identity, RegisterResponse.InvalidEmailFormat)

      // Check if email already exists
      emailExists <- userRepository.emailExists(request.email).seal
      _ <- (!emailExists).pure[IO]
           .ensure(identity, RegisterResponse.EmailAlreadyExists)

      // Validate password strength
      _ <- passwordValidator.isStrong(request.password)
           .ensure(identity, RegisterResponse.PasswordTooWeak)

      // Create user account
      user <- userRepository.create(
                User(generateId(), request.email, request.name, true)
              ).seal
    } yield RegisterResponse.Success(user.id)).run
  }

  private def generateId(): String = java.util.UUID.randomUUID().toString
}
```

This example demonstrates many Sealed Monad best practices:
- Clear separation of high-level flow and implementation details
- Well-designed ADT for response types
- Validation performed at appropriate steps
- Proper error handling and conversion