# Use Cases


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

Our observations led us to creating Sealed Monad. Check out this ugly code:

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

Matches, ifs, monad-transformers everywhere! Yuck! Applying Sealed Monad:

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

It's short, nice-looking and reads top-down.

Below approach focuses on making main service flow clear, understandable in few seconds and wholely contained in for-comprehension. Compiling example in [here](https://github.com/theiterators/sealed-monad/blob/master/examples/src/main/scala/pl/iterators/sealedmonad/examples/Options.scala#L103).

```scala mdoc
class Example3[M[_]: Monad] {
  import pl.iterators.sealedmonad.Sealed
  import pl.iterators.sealedmonad.syntax._

  // whole main service flow contained in 3 descriptive words in for comprehension
  def sealedLogin(email: String): M[LoginResponse] =
    (for {
      user        <- findAndValidateUser(email)
      authMethod  <- findOrMergeAuthMethod(user)
      loginResult <- validateAuthMethodAction(user, authMethod)
    } yield loginResult).run

  // three below private methods should have understandable, descriptive names. They hide boiler plate and contain error validation
  private def findAndValidateUser(email: String): Sealed[M, User, LoginResponse] = ???
  private def findOrMergeAuthMethod(user: User): Sealed[M, AuthMethod, LoginResponse] = ???
  private def validateAuthMethodAction(user: User, authMethod: AuthMethod): Sealed[M, LoginResponse, Nothing] = ???

  // below methods implementation could be coming from different services
  def findUser: String => M[Option[User]]                         = ???
  def findAuthMethod: (Long, Provider) => M[Option[AuthMethod]]   = ???
  def authMethodFromUserIdF: Long => AuthMethod                   = ???
  def mergeAccountsAction: (AuthMethod, User) => M[LoginResponse] = ???
  def checkAuthMethodAction: AuthMethod => Boolean                = ???
  def issueTokenFor: User => String                               = ???
}
```

The main flow consists just of 3 descriptive sentences that anyone can read and comprehend in few seconds. Everything is part of for-comprehension, which comes with the price of a little more boilerplate. Just a short look is required to
understand what is happening in the service. Descriptive private method names serve as a "documentation". If one "needs to go deeper" into the validation details, then it will go to the body of the private method. Otherwise the validation/error handling code doesn't bloat understanding of the main method flow.

For more examples go [here](https://github.com/theiterators/sealed-monad/blob/master/examples/src/main/scala/pl/iterators/sealedmonad/examples/Options.scala).

If you're curious about Sealed Monad design process, checkout [this amazing video by Marcin Rzeźnicki](https://www.youtube.com/watch?v=uZ7IFQTYPic).
