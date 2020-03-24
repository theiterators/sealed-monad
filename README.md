## sealed-monad
##### Scala library for nice for-comprehension-style error handling
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/theiterators/sealed-monad/master/COPYING)

![logo](https://raw.githubusercontent.com/theiterators/sealed-monad/master/logo.png)


### Observations

Below are some practices we observed in our codebase that we find useful. By the way, by error we mean business-type of problem. We assume exceptions are handled by some kind of wrapper, like Future.

1. Operation (method) results are represented as ADTs. Ex.:
 ```scala
sealed trait LoginResponse

object LoginResponse {
    final case class LoggedIn(token: String)       extends LoginResponse
    case object AccountsMergeRequested             extends LoginResponse
    final case class AccountsMerged(token: String) extends LoginResponse
    case object InvalidCredentials                 extends LoginResponse
    case object Deleted                            extends LoginResponse
    case object ProviderAuthFailed                 extends LoginResponse
}
 ```

2. Methods (especially in services) are closed units of code, each returning one value out of result ADT for this particular method:
 ```scala
def login(email: String,
          findUser: String => Future[Option[User]],
          findAuthMethod: (Long, Provider) => Future[Option[AuthMethod]],
          issueTokenFor: User => String,
          checkAuthMethodAction: AuthMethod => Boolean,
          authMethodFromUserIdF: Long => AuthMethod,
          mergeAccountsAction: (AuthMethod, User) => Future[LoginResponse]): Future[LoginResponse]
```

3. There's no distinguished error type

We didn't find it useful too often. Also when logging in, if a user is deleted is it "error" or maybe "legit" return value? There's no reason to think about it.

4. Error handling should be method-local

Enforcing global or even module-based error handling could be harmful to application architecture - errors are not born equal.

5. For-comprehensions are nice, programmers like them

6. Computations create tree-like structures

If-else = branching.

### Sealed Monad

Our observations led us to creating Sealed Monad. Check out this ugly code:

```scala
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

```scala
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

For more examples go [here](https://github.com/theiterators/sealed-monad/blob/master/examples/src/main/scala/pl/iterators/sealedmonad/examples/Options.scala).

If you're curious about Sealed Monad design process, checkout [this amazing video by Marcin Rzeźnicki](https://www.youtube.com/watch?v=uZ7IFQTYPic).

### Installation
```scala
libraryDependencies += "pl.iterators" %% "sealed" % "1.0.0"
```

### Known problems

Type interference is really bad (but hey, it's not our fault!), you'll need to annotate types a lot.
