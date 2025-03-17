---
id: faq
title: FAQ
slug: /faq
---

This document provides a comprehensive FAQ on Sealed Monad, a Scala library that enables human‐readable, for–comprehension–style error handling. The FAQ is enriched with concrete Scala code examples drawn from our repository to illustrate common usage patterns and best practices.

---

## What Problem Does Sealed Monad Solve?

Traditional error handling in Scala—whether through nested conditionals, monad transformers like `EitherT` or `OptionT`, or even plain exceptions—often leads to convoluted and hard–to–read code. Sealed Monad simplifies business logic by:

• Representing all possible outcomes with a sealed trait (ADT).  
• Allowing error conditions to be handled locally in a single, top–down for–comprehension.  
• Eliminating the need for deeply nested pattern–matching or explicit monad transformer stacking.

For example, consider the following “ugly” login implementation that uses nested conditionals and monad transformers:

```scala
import cats.Monad
import cats.data.{EitherT, OptionT}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
  
sealed trait LoginResponse
object LoginResponse {
  final case class LoggedIn(token: String) extends LoginResponse
  case object AccountsMergeRequested extends LoginResponse
  final case class AccountsMerged(token: String) extends LoginResponse
  case object InvalidCredentials extends LoginResponse
  case object Deleted extends LoginResponse
  case object ProviderAuthFailed extends LoginResponse
}
  
trait User {
  def id: Long
  def archived: Boolean
}
  
trait AuthMethod {
  def provider: Provider
  def userId: Long
}
  
sealed trait Provider
object Provider {
  case object EmailPass extends Provider
  case object LinkedIn extends Provider
  case object Facebook extends Provider
}
  
def login[M[_]](
    email: String,
    findUser: String => M[Option[User]],
    findAuthMethod: (Long, Provider) => M[Option[AuthMethod]],
    issueTokenFor: User => String,
    checkAuthMethodAction: AuthMethod => Boolean,
    authMethodFromUserIdF: Long => AuthMethod,
    mergeAccountsAction: (AuthMethod, User) => M[LoginResponse]
)(implicit M: Monad[M]): M[LoginResponse] =
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

By contrast, Sealed Monad enables a much more concise and self–documenting implementation:

```scala
import cats.Monad
import pl.iterators.sealedmonad.syntax.*

def sealedLogin[M[_]](
    email: String,
    findUser: String => M[Option[User]],
    findAuthMethod: (Long, Provider) => M[Option[AuthMethod]],
    issueTokenFor: User => String,
    checkAuthMethodAction: AuthMethod => Boolean,
    authMethodFromUserIdF: Long => AuthMethod,
    mergeAccountsAction: (AuthMethod, User) => M[LoginResponse]
)(implicit M: Monad[M]): M[LoginResponse] = {
  val s = for {
    user <- findUser(email)
      .valueOr(LoginResponse.InvalidCredentials)
      .ensure(!_.archived, LoginResponse.Deleted)
    userAuthMethod = authMethodFromUserIdF(user.id)
    authMethod <- findAuthMethod(user.id, userAuthMethod.provider)
      .valueOrF(mergeAccountsAction(userAuthMethod, user))
  } yield {
    if (checkAuthMethodAction(authMethod))
      LoginResponse.LoggedIn(issueTokenFor(user))
    else
      LoginResponse.InvalidCredentials
  }
  s.run
}
```

Here the flow is linear and self–explanatory. The use of helper methods such as `valueOr`, `ensure`, and `valueOrF` makes it clear how each possible error condition is handled, thereby separating the business logic from the error handling details.

---

## How Does Sealed Monad Improve Service Flow Clarity?

Sealed Monad encourages a design where the main service logic is expressed in a clean, descriptive for–comprehension. The detailed error handling is encapsulated in small, well–named helper methods. Consider the following example drawn from an authentication service:

```scala
import cats.Monad
import pl.iterators.sealedmonad.syntax.*

sealed trait LoginResponse
object LoginResponse {
  final case class LoggedIn(token: String) extends LoginResponse
  case object InvalidCredentials extends LoginResponse
  case object Deleted extends LoginResponse
}

trait User {
  def id: Long
  def archived: Boolean
}

trait AuthMethod {
  def provider: Provider
  def userId: Long
}

sealed trait Provider
object Provider {
  case object EmailPass extends Provider
  case object LinkedIn extends Provider
  case object Facebook extends Provider
}

class Example3[M[_]: Monad] {
  import pl.iterators.sealedmonad.Sealed

  // The main service flow is expressed as a sequence of descriptive steps
  def sealedLogin(email: String): M[LoginResponse] =
    (for {
      user        <- findAndValidateUser(email)
      authMethod  <- findOrMergeAuthMethod(user)
      loginResult <- validateAuthMethodAction(user, authMethod)
    } yield loginResult).run

  // Each helper method encapsulates specific error checks,
  // making the main flow easy to read and understand.
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
      if (checkAuthMethodAction(authMethod))
        LoginResponse.LoggedIn(issueTokenFor(user))
      else
        LoginResponse.InvalidCredentials
    Monad[M].pure(result).seal
  }

  // These methods represent dependencies that would be provided by other services:
  def findUser: String => M[Option[User]]                         = ???
  def findAuthMethod: (Long, Provider) => M[Option[AuthMethod]]   = ???
  def authMethodFromUserIdF: Long => AuthMethod                   = ???
  def mergeAccountsAction: (AuthMethod, User) => M[LoginResponse] = ???
  def checkAuthMethodAction: AuthMethod => Boolean                = ???
  def issueTokenFor: User => String                               = ???
}
```

In this design, a developer can understand the overall flow by simply reading the three main steps—“find and validate user”, “find or merge auth method”, and “validate auth method action”—without having to sift through low–level error handling boilerplate.

---

## How Do I Handle Alternate Outcomes Like Email Confirmation?

Sealed Monad is equally valuable for other operations. Consider the email confirmation scenario. Here is a concise version using Sealed Monad:

```scala
import cats.Monad
import pl.iterators.sealedmonad.syntax.*

sealed trait ConfirmResponse
object ConfirmResponse {
  case object MethodNotFound extends ConfirmResponse
  case object UserNotFound extends ConfirmResponse
  final case class Confirmed(jwt: String) extends ConfirmResponse
}

def sealedConfirmEmail[M[_]: Monad](
    token: String,
    findAuthMethod: String => M[Option[AuthMethod]],
    findUser: Long => M[Option[User]],
    upsertAuthMethod: AuthMethod => M[Int],
    issueTokenFor: User => String,
    confirmMethod: AuthMethod => AuthMethod
): M[ConfirmResponse] = {
  val s = for {
    method <- findAuthMethod(token).valueOr[ConfirmResponse](ConfirmResponse.MethodNotFound)
    user <- findUser(method.userId)
      .valueOr[ConfirmResponse](ConfirmResponse.UserNotFound)
      .flatTap(_ => upsertAuthMethod(confirmMethod(method)))
  } yield ConfirmResponse.Confirmed(issueTokenFor(user))
  s.run
}
```

This example shows how Sealed Monad transforms potentially missing values into well–defined outcomes with a simple linear flow.

---

## How Can Sealed Monad Be Applied in a Business–Critical Workflow?

Let’s consider a service method for creating a new "todo" item. This example illustrates how you can combine validations, error handling, and repository calls into a single clear for–comprehension:

```scala
import java.util.UUID
import cats.effect.IO
import pl.iterators.sealedmonad.syntax._

sealed trait CreateTodoResponse
object CreateTodoResponse {
  case object UserNotFound extends CreateTodoResponse
  case object UserNotInOrganization extends CreateTodoResponse
  case object UserNotAllowedToCreateTodos extends CreateTodoResponse
  case object TodoAlreadyExists extends CreateTodoResponse
  case object TodoTitleEmpty extends CreateTodoResponse
  final case class Created(todo: Todo) extends CreateTodoResponse
}

case class CreateTodoRequest(title: String)
case class Todo(id: UUID, title: String)
  
// These traits represent our repository interfaces.
trait UserRepository {
  def find(userId: UUID): IO[Option[User]]
}

trait OrganizationRepository {
  def findFor(userId: UUID): IO[Option[Organization]]
}

trait TodoRepository {
  def find(title: String): IO[Option[Todo]]
  def insert(todo: Todo): IO[Todo]
}

trait Todo {
  def title: String
}

def createTodo(userId: UUID, organizationId: UUID, request: CreateTodoRequest): IO[CreateTodoResponse] = {
  (for {
    user <- userRepository.find(userId)
      .valueOr(CreateTodoResponse.UserNotFound)
      .ensure(!_.archived, CreateTodoResponse.UserNotInOrganization)
    _ <- organizationRepository.findFor(userId)
      .valueOr(CreateTodoResponse.UserNotInOrganization)
      .ensure(_.canCreateTodos(user), CreateTodoResponse.UserNotAllowedToCreateTodos)
    _ <- todoRepository.find(request.title)
      .ensure(_.isEmpty, CreateTodoResponse.TodoAlreadyExists)
    _ <- Todo
      .from(request)  // Assume this converts request to a Todo instance.
      .pure[IO]
      .ensure(_.title.nonEmpty, CreateTodoResponse.TodoTitleEmpty)
    todo <- todoRepository.insert(Todo(UUID.randomUUID(), request.title)).seal
  } yield CreateTodoResponse.Created(todo)).run
}
```

In the above snippet, each step checks the necessary condition and, in case of a failure, short–circuits with an appropriate response. This pattern makes the service behavior explicit and easy to trace.

---

## Where Can I Learn More About Sealed Monad’s Design?

If you wish to understand the design process and rationale behind Sealed Monad, please watch the following video by Marcin Rzeźnicki:  
[Marcin Rzeźnicki – Reach ADT or Die](https://www.youtube.com/watch?v=uZ7IFQTYPic)

For further details, refer to our comprehensive documentation on the website.

---

## Conclusion

Sealed Monad offers a way to write clear, maintainable, and business–oriented Scala code by unifying error handling and business logic in a single, linear flow. By encapsulating validations and short–circuiting errors inside descriptive helper methods, the library allows you to focus on the primary logic while handling errors gracefully.

For more code examples and further documentation, please visit our [website](https://theiterators.github.io/sealed-monad/docs/introduction).

Happy coding!
