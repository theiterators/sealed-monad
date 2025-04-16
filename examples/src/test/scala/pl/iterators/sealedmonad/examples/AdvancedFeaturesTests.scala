package pl.iterators.sealedmonad.examples

import cats.Monad
import cats.instances.future._
import cats.instances.option._
import cats.syntax.applicative._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pl.iterators.sealedmonad.syntax._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.concurrent.Await

class AdvancedFeaturesTests extends AnyFunSuite with Matchers {

  implicit val ec: ExecutionContext = ExecutionContext.global

  // Domain models
  case class User(id: Long, email: String, archived: Boolean)

  sealed trait LoginResponse

  object LoginResponse {
    final case class LoggedIn(token: String) extends LoginResponse
    case object InvalidCredentials           extends LoginResponse
    case object Deleted                      extends LoginResponse
    case object ProviderAuthFailed           extends LoginResponse
  }

  // Test for the Multi-Step Authentication Flow example from advanced-features.md
  test("Multi-Step Authentication Flow example should work as expected") {
    // Additional model for this example
    case class AuthMethod(userId: Long, providerName: String)

    // Implementation with Sealed Monad
    def sealedLogin[M[_]: Monad](
        email: String,
        // Repository functions
        findUser: String => M[Option[User]],
        findAuthMethod: (Long, String) => M[Option[AuthMethod]],
        issueTokenFor: User => String,
        checkAuthMethod: AuthMethod => Boolean,
        authMethodFromUserId: Long => AuthMethod,
        mergeAccountsAction: (AuthMethod, User) => M[LoginResponse]
    ): M[LoginResponse] = {
      val s = for {
        // Find and validate user
        user <- findUser(email)
          .valueOr(LoginResponse.InvalidCredentials)  // If user not found
          .ensure(!_.archived, LoginResponse.Deleted) // If user is archived

        // Get auth method
        userAuthMethod = authMethodFromUserId(user.id)
        authMethod <- findAuthMethod(user.id, userAuthMethod.providerName)
          .valueOrF(mergeAccountsAction(userAuthMethod, user)) // If auth method missing, try to merge accounts
      } yield
      // Generate token or return invalid credentials
      if (checkAuthMethod(authMethod))
        LoginResponse.LoggedIn(issueTokenFor(user))
      else
        LoginResponse.InvalidCredentials

      s.run // Execute the computation and return the result
    }

    // Test with Future as the effect type
    {
      // Test data
      val activeUser        = User(1, "user@example.com", archived = false)
      val archivedUser      = User(2, "archived@example.com", archived = true)
      val validAuthMethod   = AuthMethod(1, "email")
      val invalidAuthMethod = AuthMethod(1, "oauth")

      // Test cases
      val testCases = List(
        ("user@example.com", LoginResponse.LoggedIn("token-1")),
        ("archived@example.com", LoginResponse.Deleted),
        ("nonexistent@example.com", LoginResponse.InvalidCredentials),
        ("merge@example.com", LoginResponse.ProviderAuthFailed)
      )

      // Run tests for each test case
      for ((testEmail, expectedResponse) <- testCases) {
        // Mock functions specific to this test case
        def findUser(email: String): Future[Option[User]] = email match {
          case "user@example.com"     => Future.successful(Some(activeUser))
          case "archived@example.com" => Future.successful(Some(archivedUser))
          case "merge@example.com"    => Future.successful(Some(activeUser))
          case _                      => Future.successful(None)
        }

        def findAuthMethod(userId: Long, providerName: String): Future[Option[AuthMethod]] =
          if (testEmail == "merge@example.com") {
            Future.successful(None)
          } else {
            (userId, providerName) match {
              case (1, "email") => Future.successful(Some(validAuthMethod))
              case _            => Future.successful(None)
            }
          }

        def issueTokenFor(user: User): String = s"token-${user.id}"

        def checkAuthMethod(authMethod: AuthMethod): Boolean = authMethod.providerName == "email"

        def authMethodFromUserId(userId: Long): AuthMethod = AuthMethod(userId, "email")

        def mergeAccountsAction(authMethod: AuthMethod, user: User): Future[LoginResponse] =
          Future.successful(LoginResponse.ProviderAuthFailed)

        val result = Await.result(
          sealedLogin(testEmail, findUser, findAuthMethod, issueTokenFor, checkAuthMethod, authMethodFromUserId, mergeAccountsAction),
          5.seconds
        )

        result shouldBe expectedResponse
      }
    }

    // Test with Option as the effect type
    {
      // Test data
      val activeUser      = User(1, "user@example.com", archived = false)
      val archivedUser    = User(2, "archived@example.com", archived = true)
      val validAuthMethod = AuthMethod(1, "email")

      // Test cases
      val testCases = List(
        ("user@example.com", LoginResponse.LoggedIn("token-1")),
        ("archived@example.com", LoginResponse.Deleted),
        ("nonexistent@example.com", LoginResponse.InvalidCredentials)
      )

      // Run tests for each test case
      for ((testEmail, expectedResponse) <- testCases) {
        // Mock functions specific to this test case
        def findUser(email: String): Option[Option[User]] = email match {
          case "user@example.com"     => Some(Some(activeUser))
          case "archived@example.com" => Some(Some(archivedUser))
          case _                      => Some(None)
        }

        def findAuthMethod(userId: Long, providerName: String): Option[Option[AuthMethod]] =
          (userId, providerName) match {
            case (1, "email") => Some(Some(validAuthMethod))
            case _            => Some(None)
          }

        def issueTokenFor(user: User): String = s"token-${user.id}"

        def checkAuthMethod(authMethod: AuthMethod): Boolean = authMethod.providerName == "email"

        def authMethodFromUserId(userId: Long): AuthMethod = AuthMethod(userId, "email")

        def mergeAccountsAction(authMethod: AuthMethod, user: User): Option[LoginResponse] =
          Some(LoginResponse.ProviderAuthFailed)

        val result =
          sealedLogin(testEmail, findUser, findAuthMethod, issueTokenFor, checkAuthMethod, authMethodFromUserId, mergeAccountsAction)

        result shouldBe Some(expectedResponse)
      }
    }
  }
}
