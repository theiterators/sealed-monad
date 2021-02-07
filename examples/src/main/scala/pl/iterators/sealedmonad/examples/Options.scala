package pl.iterators.sealedmonad.examples

import cats.Monad
import cats.data.{EitherT, OptionT}
import cats.syntax.flatMap._
import cats.syntax.functor._
import pl.iterators.sealedmonad.Sealed

import scala.language.higherKinds

object Options {

  object Example1 {
    import pl.iterators.sealedmonad.syntax._

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
        authMethod <- findAuthMethod(user.id, userAuthMethod.provider).valueOrF(mergeAccountsAction(userAuthMethod, user))
      } yield if (checkAuthMethodAction(authMethod)) LoginResponse.LoggedIn(issueTokenFor(user)) else LoginResponse.InvalidCredentials

      s.run
    }
  }

  object Example2 {
    import pl.iterators.sealedmonad.syntax._

    def confirmEmail[M[_]: Monad](
        token: String,
        findAuthMethod: String => M[Option[AuthMethod]],
        findUser: Long => M[Option[User]],
        upsertAuthMethod: AuthMethod => M[Int],
        issueTokenFor: User => String,
        confirmMethod: AuthMethod => AuthMethod
    ): M[ConfirmResponse] = {
      val userT = for {
        method <- EitherT.fromOptionF(findAuthMethod(token), ifNone = ConfirmResponse.MethodNotFound)
        user   <- EitherT.fromOptionF(findUser(method.userId), ifNone = ConfirmResponse.UserNotFound: ConfirmResponse)
      } yield (method, user)

      userT.semiflatMap { case (method, user) =>
        upsertAuthMethod(confirmMethod(method)).map(_ => ConfirmResponse.Confirmed(issueTokenFor(user)))
      }.merge
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
        user   <- findUser(method.userId).valueOr[ConfirmResponse](ConfirmResponse.UserNotFound) ! upsertAuthMethod(confirmMethod(method))
      } yield ConfirmResponse.Confirmed(issueTokenFor(user))

      s.run
    }

    class Example3[M[_]: Monad] {
      import pl.iterators.sealedmonad.syntax._

      def sealedLogin(email: String): M[LoginResponse] =
        (for {
          user        <- findAndValidateUser(email)
          authMethod  <- findOrMergeAuthMethod(user)
          loginResult <- validateAuthMethodAction(user, authMethod)
        } yield loginResult).run

      // three below private methods should have understandable, descriptive names
      private def findAndValidateUser(email: String): Sealed[M, User, LoginResponse] =
        findUser(email)
          .valueOr(LoginResponse.InvalidCredentials)
          .ensure(!_.archived, LoginResponse.Deleted)

      private def findOrMergeAuthMethod(user: User): Sealed[M, AuthMethod, LoginResponse] = {
        val userAuthMethod = authMethodFromUserIdF(user.id)
        findAuthMethod(user.id, userAuthMethod.provider).valueOrF(mergeAccountsAction(userAuthMethod, user))
      }

      private def validateAuthMethodAction(user: User, authMethod: AuthMethod): Sealed[M, LoginResponse, Nothing] = {
        val result =
          if (checkAuthMethodAction(authMethod)) LoginResponse.LoggedIn(issueTokenFor(user)) else LoginResponse.InvalidCredentials
        Monad[M].pure(result).seal
      }

      // below methods could be coming from different services
      def findUser: String => M[Option[User]]                         = ???
      def findAuthMethod: (Long, Provider) => M[Option[AuthMethod]]   = ???
      def authMethodFromUserIdF: Long => AuthMethod                   = ???
      def mergeAccountsAction: (AuthMethod, User) => M[LoginResponse] = ???
      def checkAuthMethodAction: AuthMethod => Boolean                = ???
      def issueTokenFor: User => String                               = ???
    }

  }

  trait AuthMethod {
    def provider: Provider
    def userId: Long
  }

  trait User {
    def id: Long
    def archived: Boolean
  }

  sealed trait LoginResponse

  object LoginResponse {
    final case class LoggedIn(token: String)       extends LoginResponse
    case object AccountsMergeRequested             extends LoginResponse
    final case class AccountsMerged(token: String) extends LoginResponse
    case object InvalidCredentials                 extends LoginResponse
    case object Deleted                            extends LoginResponse
    case object ProviderAuthFailed                 extends LoginResponse
  }

  sealed trait ConfirmResponse

  object ConfirmResponse {
    case object MethodNotFound              extends ConfirmResponse
    case object UserNotFound                extends ConfirmResponse
    final case class Confirmed(jwt: String) extends ConfirmResponse
  }

  sealed trait Provider

  object Provider {
    case object EmailPass extends Provider
    case object LinkedIn  extends Provider
    case object Facebook  extends Provider
  }

}
