# Observations

Below are some practices we observed in our codebase that we find useful. By the way, by error we mean business-type of problem. We assume exceptions are handled by some kind of wrapper, like Future.

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
```

1. Operation (method) results are represented as ADTs. Ex.:

```scala mdoc
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

2. Methods (especially in services) are closed units of code, each returning one value out of result ADT for this particular method:

```scala mdoc
  def login(email: String,
            findUser: String => Future[Option[User]],
            findAuthMethod: (Long, Provider) => Future[Option[AuthMethod]],
            issueTokenFor: User => String,
            checkAuthMethodAction: AuthMethod => Boolean,
            authMethodFromUserIdF: Long => AuthMethod,
            mergeAccountsAction: (AuthMethod, User) => Future[LoginResponse]): Future[LoginResponse] = ???
```

3. There's no distinguished error type

We didn't find it useful too often. Also when logging in, if a user is deleted is it "error" or maybe "legit" return value? There's no reason to think about it.

4. Error handling should be method-local

Enforcing global or even module-based error handling could be harmful to application architecture - errors are not born equal.

5. For-comprehensions are nice, programmers like them

6. Computations create tree-like structures

If-else = branching.
