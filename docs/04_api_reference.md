# Sealed Monad Documentation

The Sealed Monad is a Scala library designed to simplify business‐logic error handling using for–comprehensions. It combines the benefits of monadic error handling with the clarity of using ADTs (algebraic data types) to signal error conditions. In this documentation the core operations are explained with code examples extracted from the source.

---

## Core Concepts

A Sealed instance represents a computation that may either yield an intermediate successful value or short-circuit with a final “error” value (an ADT). You can create Sealed instances from pure values or from effectful computations. For example:

- **Lifting a Pure Value**

  ```scala
  import cats.Id
  import pl.iterators.sealedmonad._
  import pl.iterators.sealedmonad.syntax._

  // Lifts an integer into the Sealed context.
  val sealedValue: Sealed[Id, Int, Nothing] = 42.liftSealed[Id, Nothing]
  println(sealedValue.run) // prints 42
  ```

- **Lifting an Effectful Computation**

  ```scala
  import cats.effect.IO
  import pl.iterators.sealedmonad._
  import pl.iterators.sealedmonad.syntax._

  // Creates a Sealed from an IO computation.
  val ioSealed: Sealed[IO, String, Nothing] = IO("Hello, World!").seal
  ioSealed.run.map(println) // prints "Hello, World!"
  ```

---

## Core Operations

### Mapping (map)

Transforms the intermediate value using a pure function. If the computation is successful, the function is applied; otherwise the final value is preserved.

```scala
import cats.Id
import pl.iterators.sealedmonad._
import pl.iterators.sealedmonad.syntax._

val sealedInt: Sealed[Id, Int, String] = 10.liftSealed[Id, String]
val sealedMapped: Sealed[Id, Int, String] = sealedInt.map(_ + 5)
// Since the value is present, run returns the new intermediate value.
println(sealedMapped.run) // prints 15
```

### FlatMapping (flatMap)

Chains computations where the next step may also return a Sealed instance. This operation maintains the short-circuiting behavior in case any step produces a final ADT value.

```scala
import cats.Id
import pl.iterators.sealedmonad._
import pl.iterators.sealedmonad.syntax._

def safeDivide(a: Int, b: Int): Sealed[Id, Int, String] =
  if (b != 0) (a / b).liftSealed[Id, String]
  else "DivisionByZero".seal

val result = 20.liftSealed[Id, String].flatMap(x => safeDivide(x, 4))
println(result.run) // prints 5

val errorResult = 20.liftSealed[Id, String].flatMap(x => safeDivide(x, 0))
println(errorResult.run) // prints DivisionByZero
```

### Effectful Transformation (semiflatMap)

Transforms the intermediate value using an effectful function; that is, a function returning a value in an effect context F.

```scala
import cats.effect.IO
import pl.iterators.sealedmonad._
import pl.iterators.sealedmonad.syntax._

val ioSealed: Sealed[IO, Int, String] = IO(50).seal
val transformed: Sealed[IO, Int, String] = ioSealed.semiflatMap(i => IO(i * 2))
transformed.run.map(println) // prints 100
```

### Converting Options to Sealed (valueOr / valueOrF)

Transforms an effectful Option into a Sealed instance. If the Option is defined, its value becomes the intermediate result; otherwise, the supplied ADT (or effectful ADT) is used to short-circuit.

```scala
import cats.effect.IO
import pl.iterators.sealedmonad._
import pl.iterators.sealedmonad.syntax._

val someUser: IO[Option[String]] = IO.pure(Some("JohnDoe"))
val missingUser: IO[Option[String]] = IO.pure(None)

// Use a plain ADT to signal error
val sealedUser1: Sealed[IO, String, String] = someUser.valueOr("UserNotFound")
val sealedUser2: Sealed[IO, String, String] = missingUser.valueOr("UserNotFound")

sealedUser1.run.map(println) // prints "JohnDoe"
sealedUser2.run.map(println) // prints "UserNotFound"
```

### Ensuring a Condition (ensure / ensureF)

Checks that the intermediate value satisfies a predicate. If the predicate fails, the computation is ended with a provided ADT (or an effectful ADT).

```scala
import cats.effect.IO
import pl.iterators.sealedmonad._
import pl.iterators.sealedmonad.syntax._

case class User(name: String, isActive: Boolean)
sealed trait LoginError
object LoginError {
  case object InactiveAccount extends LoginError
}

// Lift a User into a Sealed instance and check if active.
val userIO: IO[Option[User]] = IO.pure(Some(User("Alice", isActive = false)))

val activeUser: Sealed[IO, User, LoginError] =
  userIO.valueOr(LoginError.InactiveAccount)
    .ensure(user => user.isActive, LoginError.InactiveAccount)

activeUser.run.map {
  case user: User       => println(s"User active: ${user.name}")
  case error: LoginError => println(s"Error: $error")
}
// prints "Error: InactiveAccount" because the condition failed.
```

### Attempting a Transformation (attempt / attemptF)

Attempts to transform the intermediate value into an Either. If the function returns a Right, the intermediate value is updated; if it returns a Left the computation short-circuits.

```scala
import cats.Id
import pl.iterators.sealedmonad._
import pl.iterators.sealedmonad.syntax._

def businessLogic(num: Int): Either[String, Int] =
  if (num > 0) Right(num * 10)
  else Left("NonPositiveInput")

val sealedAttempt: Sealed[Id, Int, String] = 5.liftSealed[Id, String].attempt(businessLogic)
println(sealedAttempt.run) // prints 50

val sealedFail: Sealed[Id, Int, String] = (-3).liftSealed[Id, String].attempt(businessLogic)
println(sealedFail.run) // prints "NonPositiveInput"
```

---

## Usage Example – A Business Flow

The following example demonstrates how to combine several operations together to define a business logic flow. In this example a Todo is created if all validations pass.

```scala
import java.util.UUID
import cats.effect.IO
import pl.iterators.sealedmonad._
import pl.iterators.sealedmonad.syntax._

case class CreateTodoRequest(title: String)
case class Todo(id: UUID, title: String)
sealed trait CreateTodoResponse
object CreateTodoResponse {
  case class Created(todo: Todo) extends CreateTodoResponse
  case object UserNotFound extends CreateTodoResponse
  case object UserNotInOrganization extends CreateTodoResponse
  case object UserNotAllowedToCreateTodos extends CreateTodoResponse
  case object TodoAlreadyExists extends CreateTodoResponse
  case object TodoTitleEmpty extends CreateTodoResponse
}

def userRepositoryFind(id: UUID): IO[Option[String]] = IO.pure(Some("Alice"))
def organizationRepositoryFindFor(userId: UUID): IO[Option[String]] = IO.pure(Some("Org1"))
def todoRepositoryFind(title: String): IO[Option[Todo]] = IO.pure(None)
def todoRepositoryInsert(todo: Todo): IO[Todo] = IO.pure(todo)

def createTodo(userId: UUID, organizationId: UUID, request: CreateTodoRequest): IO[CreateTodoResponse] = {
  // The main flow is defined in a for–comprehension using Sealed Monad methods.
  val flow = for {
    user <- userRepositoryFind(userId)
      .valueOr(CreateTodoResponse.UserNotFound)
    _ <- organizationRepositoryFindFor(userId)
      .valueOr(CreateTodoResponse.UserNotInOrganization)
      .ensure(org => org.nonEmpty, CreateTodoResponse.UserNotAllowedToCreateTodos)
    _ <- todoRepositoryFind(request.title)
      .ensure(_.isEmpty, CreateTodoResponse.TodoAlreadyExists)
    // Validate the request
    _ <- request.title.liftSealed[IO, CreateTodoResponse]
      .ensure(_.nonEmpty, CreateTodoResponse.TodoTitleEmpty)
    // Insert the Todo item
    todo <- todoRepositoryInsert(Todo(UUID.randomUUID(), request.title)).seal
  } yield CreateTodoResponse.Created(todo)

  flow.run
}

// Example invocation:
val response: IO[CreateTodoResponse] = createTodo(UUID.randomUUID(), UUID.randomUUID(), CreateTodoRequest("Learn Sealed Monad"))
response.map(println)
```

---

## Additional Extensions

The library also provides a number of syntax extensions to make working with effectful types easier:

- **faSyntax** lets you convert values of type `F[A]` into a Rich Sealed interface.
- **faOptSyntax** helps in converting `F[Option[A]]` into Sealed instances with default error values.
- **faEitherSyntax** allows converting `F[Either[A, B]]` into a Sealed instance, so that errors can be handled uniformly.
- **eitherSyntax** converts plain Either values into Sealed instances.

These extensions allow you to write concise and readable business logic flows while maintaining type safety and clear error signalling.

---

For further information and more examples, please refer to the [project repository](https://github.com/theiterators/sealed-monad) and the [website documentation](https://theiterators.github.io/sealed-monad/docs/introduction).

Enjoy using Sealed Monad for clean, readable error handling!
