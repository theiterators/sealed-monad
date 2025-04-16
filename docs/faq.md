---
id: faq
title: Frequently Asked Questions
slug: /faq
---

# Frequently Asked Questions

This document addresses common questions about Sealed Monad, its use cases, and best practices.

## What Problem Does Sealed Monad Solve?

Traditional error handling in Scala—whether through nested conditionals, monad transformers like `EitherT` or `OptionT`, or even plain exceptions—often leads to convoluted and hard-to-read code. Sealed Monad simplifies business logic by:

• Representing all possible outcomes with a sealed trait (ADT)  
• Allowing error conditions to be handled locally in a single, top-down for-comprehension  
• Eliminating the need for deeply nested pattern-matching or explicit monad transformer stacking

## How Does Sealed Monad Differ from EitherT?

Sealed Monad can be thought of as an enhanced version of EitherT with a more business-oriented API. The key differences are:

1. **More expressive API**: Method names like `valueOr`, `ensure`, and `attempt` clearly communicate intent compared to EitherT's more abstract operations.

2. **Uniform treatment of results**: Sealed Monad doesn't distinguish between "errors" and "successes" conceptually—everything is just a result of the operation.

3. **Simplified composition**: When working with domain-specific error types, Sealed Monad requires less boilerplate than EitherT to combine operations.

4. **Focus on ADTs**: Sealed Monad encourages modeling domain results as explicit ADTs rather than using generic error types.

## When Should I Use Sealed Monad vs. Plain Either?

Use Sealed Monad when:

- You need to compose operations that can fail in different ways
- You want to express business logic in a linear, step-by-step fashion
- Your operations work with effectful types like Future or IO
- You have complex validation workflows with multiple potential outcomes

Plain Either is simpler and sufficient when:

- You're working with synchronous code without effects
- You have simple success/failure semantics
- You don't need to combine multiple operations with different error types

## How Do I Handle Multiple Validation Steps?

Sealed Monad excels at expressing multiple validation steps clearly. Here's an example:

```scala
import pl.iterators.sealedmonad.syntax._
import cats.effect.IO

def validateOrder(order: Order): IO[OrderValidationResult] = {
  (for {
    // Validate order has items
    _ <- IO.pure(order.items.nonEmpty)
           .ensure(identity, OrderValidationResult.EmptyOrder)
    
    // Validate all items are in stock
    stockCheck <- inventoryService.checkStock(order.items).seal
    _ <- IO.pure(stockCheck.allInStock)
           .ensure(identity, OrderValidationResult.OutOfStock(stockCheck.outOfStockItems))
    
    // Validate payment information
    _ <- validatePaymentInfo(order.payment)
           .valueOr(OrderValidationResult.InvalidPayment)
    
    // Validate shipping address
    _ <- validateShippingAddress(order.shippingAddress)
           .valueOr(OrderValidationResult.InvalidShippingAddress)
  } yield OrderValidationResult.Valid).run
}
```

Each validation step is clearly expressed, and the computation short-circuits as soon as any validation fails.

## How Do I Work with Optional Values?

Use the `valueOr` operator to handle optional values:

```scala
// Find a user by ID, with NotFound as the fallback
userRepository.findById(userId)  // IO[Option[User]]
  .valueOr[UserResponse](UserResponse.NotFound)  // Sealed[IO, User, UserResponse]
```

If you need to provide an effectful fallback, use `valueOrF`:

```scala
// Find a user by ID, with logging and NotFound as the fallback
userRepository.findById(userId)
  .valueOrF(
    logger.warn(s"User not found: $userId") *> 
    IO.pure(UserResponse.NotFound)
  )
```

## How Do I Integrate Sealed Monad with Existing Code?

If you're integrating with existing code, you can:

1. **Wrap Option-returning functions**:
   ```scala
   legacyService.findUserById(id)  // Future[Option[User]]
     .valueOr(UserError.NotFound)  // Sealed[Future, User, UserError]
   ```

2. **Wrap Either-returning functions**:
   ```scala
   legacyService.validateInput(data)  // Future[Either[String, ValidatedData]]
     .fromEither  // Sealed[Future, ValidatedData, String]
   ```

3. **Use attempt for exception-handling code**:
   ```scala
   IO.delay(legacyService.riskyOperation())  // IO[Result]
     .attempt {
       case Right(result) => Right(result)
       case Left(ex: NotFoundException) => Left(Error.NotFound)
       case Left(ex) => Left(Error.Unknown(ex.getMessage))
     }
   ```

## How Do I Test Code that Uses Sealed Monad?

Testing Sealed Monad code is straightforward because it works with standard effect types:

```scala
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.unsafe.implicits.global

class UserServiceSpec extends AnyFlatSpec with Matchers {
  
  "UserService.findUser" should "return Success when user exists" in {
    val service = new UserService(
      repository = mockRepository(existingUserId = Some("user-123"))
    )
    
    val result = service.findUser("user-123").unsafeRunSync()
    result shouldBe UserResponse.Success(User("user-123", "test@example.com"))
  }
  
  it should "return NotFound when user doesn't exist" in {
    val service = new UserService(
      repository = mockRepository(existingUserId = None)
    )
    
    val result = service.findUser("invalid").unsafeRunSync()
    result shouldBe UserResponse.NotFound
  }
  
  // Helper to create mock repository
  private def mockRepository(existingUserId: Option[String]): UserRepository = 
    new UserRepository {
      def findById(id: String): IO[Option[User]] = 
        IO.pure(
          if (existingUserId.contains(id)) Some(User(id, "test@example.com"))
          else None
        )
    }
}
```

## Can I Use Sealed Monad with ZIO/Monix/Other Effect Libraries?

Yes! Sealed Monad is built on cats-core and works with any effect type that has a Monad instance. This includes:

- Cats Effect IO
- ZIO
- Monix Task
- Standard library Future
- Any other effect type with a cats Monad instance

## How Do I Debug Sealed Monad Code?

Sealed Monad provides several operators for debugging:

```scala
import pl.iterators.sealedmonad.syntax._
import cats.effect.IO

def processOrder(orderId: String): IO[OrderResponse] = {
  (for {
    // Find order with debug logging
    order <- orderRepository.findById(orderId)
               .valueOr(OrderResponse.NotFound)
               .inspect {
                 case Right(o) => println(s"Found order: $orderId")
                 case Left(OrderResponse.NotFound) => println(s"Order not found: $orderId")
               }
    
    // Process with side-effect logging
    result <- processOrderItems(order.items)
                .tap(r => println(s"Processed ${r.size} items"))
  } yield OrderResponse.Success(order.id)).run
}
```

The `inspect` operator lets you observe the current state, while `tap` and `flatTap` allow you to perform side effects without affecting the computation.

## How Does Sealed Monad Handle Performance?

Sealed Monad adds minimal overhead compared to direct monadic operations. For most business logic, the clarity and maintainability benefits far outweigh any performance considerations.

If you have performance-critical code, consider:

1. Only use Sealed Monad for the business logic portions that benefit from clear error handling
2. For hot paths with simple success/failure semantics, use more direct approaches
3. Profile your application to identify actual bottlenecks before optimizing

## Is Sealed Monad Compatible with Scala 3?

Yes, Sealed Monad is compatible with both Scala 2.13.x and Scala 3.x.

## Why Do I Get a Compilation Error with the `run` Method?

The `run` method in Sealed Monad has a specific type constraint: the success type `A` must be a subtype of the error type `ADT`. This is enforced by the type parameter bound:

```scala
def run[ADT1 >: ADT](implicit ev: A <:< ADT1, F: Monad[F]): F[ADT1]
```

This constraint exists because the `run` method needs to return a single type that can represent both successful and error outcomes.

### Common Solutions

1. **Make your success type extend your error type**:
   ```scala
   sealed trait Response
   case class Success(value: Int) extends Response
   case object NotFound extends Response
   
   // Now Success <:< Response, so this works:
   val result: Future[Response] = sealedValue.run
   ```

2. **Map your intermediate value to a result type before calling `run`**:
   ```scala
   sealed trait Response
   case class Success(value: Int) extends Response
   case object NotFound extends Response
   
   // Map Int to Success before calling run
   val result: Future[Response] = sealedInt.map(Success).run
   ```

3. **Use pattern matching after the computation**:
   ```scala
   // Instead of calling run directly
   val either: Future[Either[Error, Int]] = sealedInt.map(Right(_)).getOrElse(Left(Error))
   
   // Then pattern match on the result
   either.map {
     case Right(value) => handleSuccess(value)
     case Left(error) => handleError(error)
   }
   ```

### Required Imports

When working with Sealed Monad, make sure you have all the necessary imports:

```scala
import pl.iterators.sealedmonad.syntax._  // For extension methods
import cats.instances.future._  // For Future instances
import cats.syntax.applicative._  // For pure method
```

The `cats.syntax.applicative._` import is particularly important when using methods like `pure` on primitive values.

## Where Can I Learn More?

- [GitHub Repository](https://github.com/theiterators/sealed-monad)
- [API Documentation](https://javadoc.io/doc/pl.iterators/sealed-monad_2.13/latest/index.html)
- [Marcin Rzeźnicki's Talk: Reach ADT or Die](https://www.youtube.com/watch?v=uZ7IFQTYPic) - Learn about the design philosophy behind Sealed Monad
