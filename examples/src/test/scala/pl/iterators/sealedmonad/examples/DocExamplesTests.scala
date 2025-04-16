package pl.iterators.sealedmonad.examples

import cats.Monad
import cats.instances.future._
import cats.syntax.applicative._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pl.iterators.sealedmonad.syntax._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.concurrent.Await

class DocExamplesTests extends AnyFunSuite with Matchers {

  implicit val ec: ExecutionContext = ExecutionContext.global

  // ===== Models and tests for usecases.md examples =====
  case class Address(street: String, city: String, zipCode: String)

  // Form validation example
  sealed trait RegistrationResponse

  object RegistrationResponse {
    case class Success(userId: String) extends RegistrationResponse
    case object EmailInvalid           extends RegistrationResponse
    case object EmailTaken             extends RegistrationResponse
    case object PasswordTooWeak        extends RegistrationResponse
    case object AddressInvalid         extends RegistrationResponse
  }

  // Domain models from motivations.md
  sealed trait Provider

  object Provider {
    case object EmailPass extends Provider
    case object OAuth     extends Provider
  }

  case class User(id: Long, email: String, archived: Boolean)
  case class AuthMethod(userId: Long, provider: Provider)

  sealed trait LoginResponse

  object LoginResponse {
    final case class LoggedIn(token: String) extends LoginResponse
    case object InvalidCredentials           extends LoginResponse
    case object Deleted                      extends LoginResponse
    case object ProviderAuthFailed           extends LoginResponse
  }

  // Test for the login example from motivations.md
  test("Login example from motivations.md should work as expected") {
    // Implementation without Sealed Monad
    def loginWithoutSealed(
        email: String,
        findUser: String => Future[Option[User]],
        findAuthMethod: (Long, Provider) => Future[Option[AuthMethod]],
        issueTokenFor: User => String,
        checkAuthMethod: AuthMethod => Boolean
    ): Future[LoginResponse] =
      findUser(email).flatMap {
        case None =>
          Future.successful(LoginResponse.InvalidCredentials)
        case Some(user) if user.archived =>
          Future.successful(LoginResponse.Deleted)
        case Some(user) =>
          findAuthMethod(user.id, Provider.EmailPass).map {
            case None =>
              LoginResponse.ProviderAuthFailed
            case Some(authMethod) if !checkAuthMethod(authMethod) =>
              LoginResponse.InvalidCredentials
            case Some(_) =>
              LoginResponse.LoggedIn(issueTokenFor(user))
          }
      }

    // Implementation with Sealed Monad
    def loginWithSealed(
        email: String,
        findUser: String => Future[Option[User]],
        findAuthMethod: (Long, Provider) => Future[Option[AuthMethod]],
        issueTokenFor: User => String,
        checkAuthMethod: AuthMethod => Boolean
    ): Future[LoginResponse] =
      (for {
        user <- findUser(email)
          .valueOr(LoginResponse.InvalidCredentials)
          .ensure(!_.archived, LoginResponse.Deleted)

        authMethod <- findAuthMethod(user.id, Provider.EmailPass)
          .valueOr(LoginResponse.ProviderAuthFailed)
          .ensure(checkAuthMethod, LoginResponse.InvalidCredentials)
      } yield LoginResponse.LoggedIn(issueTokenFor(user))).run

    // Test data
    val activeUser        = User(1, "user@example.com", archived = false)
    val archivedUser      = User(2, "archived@example.com", archived = true)
    val validAuthMethod   = AuthMethod(1, Provider.EmailPass)
    val invalidAuthMethod = AuthMethod(1, Provider.OAuth)

    // Mock functions
    def findUser(email: String): Future[Option[User]] = email match {
      case "user@example.com"     => Future.successful(Some(activeUser))
      case "archived@example.com" => Future.successful(Some(archivedUser))
      case _                      => Future.successful(None)
    }

    def findAuthMethod(userId: Long, provider: Provider): Future[Option[AuthMethod]] = (userId, provider) match {
      case (1, Provider.EmailPass) => Future.successful(Some(validAuthMethod))
      case _                       => Future.successful(None)
    }

    def issueTokenFor(user: User): String = s"token-${user.id}"

    def checkAuthMethod(authMethod: AuthMethod): Boolean = authMethod.provider == Provider.EmailPass

    // Test cases
    val testCases = List(
      ("user@example.com", LoginResponse.LoggedIn("token-1")),
      ("archived@example.com", LoginResponse.Deleted),
      ("nonexistent@example.com", LoginResponse.InvalidCredentials)
    )

    // Run tests for both implementations
    for ((email, expectedResponse) <- testCases) {
      val withoutSealedResult = Await.result(
        loginWithoutSealed(email, findUser, findAuthMethod, issueTokenFor, checkAuthMethod),
        5.seconds
      )

      val withSealedResult = Await.result(
        loginWithSealed(email, findUser, findAuthMethod, issueTokenFor, checkAuthMethod),
        5.seconds
      )

      withoutSealedResult shouldBe expectedResponse
      withSealedResult shouldBe expectedResponse
      withoutSealedResult shouldBe withSealedResult
    }
  }

  // Test for the form validation example from usecases.md
  test("Form validation example from usecases.md should work as expected") {
    // Implementation with Sealed Monad
    def registerUser(
        email: String,
        password: String,
        address: Address,
        validateEmail: String => Future[Boolean],
        checkEmailExists: String => Future[Boolean],
        validatePassword: String => Future[Boolean],
        validateAddress: Address => Future[Boolean],
        createUser: (String, String, Address) => Future[User]
    ): Future[RegistrationResponse] =
      (for {
        // Validate email format
        _ <- validateEmail(email)
          .ensure(isValid => isValid, RegistrationResponse.EmailInvalid)

        // Check if email is already taken
        emailExistsCheck <- checkEmailExists(email).seal
        _ <- (!emailExistsCheck)
          .pure[Future]
          .ensure(notTaken => notTaken, RegistrationResponse.EmailTaken)

        // Validate password strength
        _ <- validatePassword(password)
          .ensure(isStrong => isStrong, RegistrationResponse.PasswordTooWeak)

        // Validate address
        _ <- validateAddress(address)
          .ensure(isValid => isValid, RegistrationResponse.AddressInvalid)

        // Create user
        user <- createUser(email, password, address).seal
      } yield RegistrationResponse.Success(user.id.toString)).run

    // Mock functions
    def validateEmail(email: String): Future[Boolean] =
      Future.successful(email.matches(".+@.+\\..+"))

    def checkEmailExists(email: String): Future[Boolean] =
      Future.successful(email == "existing@example.com")

    def validatePassword(password: String): Future[Boolean] =
      Future.successful(password.length >= 8)

    def validateAddress(address: Address): Future[Boolean] =
      Future.successful(address.street.nonEmpty && address.city.nonEmpty && address.zipCode.nonEmpty)

    def createUser(email: String, password: String, address: Address): Future[User] =
      Future.successful(User(123, email, archived = false))

    // Test cases
    val validAddress   = Address("123 Main St", "Anytown", "12345")
    val invalidAddress = Address("", "", "")

    val testCases = List(
      // Valid registration
      ("new@example.com", "password123", validAddress, RegistrationResponse.Success("123")),
      // Invalid email format
      ("invalid-email", "password123", validAddress, RegistrationResponse.EmailInvalid),
      // Email already taken
      ("existing@example.com", "password123", validAddress, RegistrationResponse.EmailTaken),
      // Password too weak
      ("new@example.com", "weak", validAddress, RegistrationResponse.PasswordTooWeak),
      // Invalid address
      ("new@example.com", "password123", invalidAddress, RegistrationResponse.AddressInvalid)
    )

    // Run tests
    for ((email, password, address, expectedResponse) <- testCases) {
      val result = Await.result(
        registerUser(email, password, address, validateEmail, checkEmailExists, validatePassword, validateAddress, createUser),
        5.seconds
      )

      result shouldBe expectedResponse
    }
  }
}
