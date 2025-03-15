# Sealed Monad Usage Examples

This document provides practical examples demonstrating Sealed Monad's capabilities, usage patterns, and real-world applications. These examples are designed to help developers understand how to effectively use Sealed Monad in their Scala projects.

## Table of Contents

1. [Basic Usage Patterns](#basic-usage-patterns)
2. [Core Service Patterns](#core-service-patterns)
3. [Error Handling Patterns](#error-handling-patterns)
4. [Domain Modeling Patterns](#domain-modeling-patterns)
5. [Real-World Application Examples](#real-world-application-examples)
6. [Comparison with Traditional Approaches](#comparison-with-traditional-approaches)

## Basic Usage Patterns

### Converting Optional Values with Default Fallback

In this example, we define a simple sealed monad called Result with two cases, Success and Failure. We then demonstrate how to convert an Optional value (Scala Option) into our Result type by providing a default fallback.

```scala
// Define the sealed monad for our results
sealed trait Result[+A] {
  // Standard map over a successful result
  def map[B](f: A => B): Result[B] = this match {
    case Success(a)   => Success(f(a))
    case Failure(err) => Failure(err)
  }

  // Standard flatMap over a successful result
  def flatMap[B](f: A => Result[B]): Result[B] = this match {
    case Success(a)   => f(a)
    case Failure(err) => Failure(err)
  }
}

// Case representing a successful result
case class Success[A](value: A) extends Result[A]

// Case representing a failed result
case class Failure(error: Throwable) extends Result[Nothing]

// Helper function to convert Option[A] to Result[A] with a fallback value
def fromOption[A](opt: Option[A], default: A): Result[A] =
  opt match {
    case Some(a) => Success(a)
    case None    => Success(default)
  }

// Example usage
val someValue: Option[Int] = Some(42)
val noValue: Option[Int] = None

val result1 = fromOption(someValue, 0) // Should be Success(42)
val result2 = fromOption(noValue, 10)  // Should be Success(10)
```

---

# Sealed Monad Usage Patterns

This document compiles the various ways Sealed Monad is used across different real-world examples, organized by pattern type rather than by example. Each pattern includes code snippets, explanations of why the pattern is useful, and comparisons with traditional approaches where relevant.

## Table of Contents

1. [Core Patterns](#core-patterns)
   - [Service Structure with Sealed Monad](#service-structure-with-sealed-monad)
   - [ADT for Domain-Specific Results](#adt-algebraic-data-type-for-domain-specific-results)
   - [Converting Optional Values with Default Fallbacks](#converting-optional-values-with-default-fallbacks)
   - [Validation with ensure](#validation-with-ensure)
   - [Early Return Pattern](#early-return-pattern)
2. [Composition Patterns](#composition-patterns)
   - [Chaining Operations with for-comprehensions](#chaining-operations-with-for-comprehensions)
   - [Combining Multiple Operations](#combining-multiple-operations)

## Core Patterns

### Service Structure with Sealed Monad

A typical service can use the Sealed Monad to manage business logic that may result in success or failure. The service abstracts away error handling and enables clear separation of concerns.

```scala
object UserService {
  // Register a user; fail if age is below the allowed threshold.
  def registerUser(email: String, age: Int): Result[String] = {
    if (age < 18) Failure(new Exception("User too young"))
    else Success(s"User $email registered successfully")
  }
}

// Example usage
val registration = UserService.registerUser("user@example.com", 20)
registration match {
  case Success(msg) => println(msg)
  case Failure(err) => println(s"Registration failed: ${err.getMessage}")
}
```

### ADT (Algebraic Data Type) for Domain-Specific Results

Using the sealed trait approach, we can model domain-specific outcomes that encapsulate both the desired result and error cases.

```scala
sealed trait OrderResult
case class OrderSuccess(orderId: Long) extends OrderResult
case class OrderFailure(reason: String) extends OrderResult

object OrderService {
  def placeOrder(amount: Double): OrderResult =
    if (amount > 0) OrderSuccess(1001L)
    else OrderFailure("Order amount must be positive")
}

// Example usage
val orderResult = OrderService.placeOrder(150.0)
orderResult match {
  case OrderSuccess(id)    => println(s"Order placed with id: $id")
  case OrderFailure(reason) => println(s"Order failed: $reason")
}
```

### Converting Optional Values with Default Fallbacks

Building on the basic usage, this pattern shows how an Optional value (Option) is safely converted into our Result type with a fallback.

```scala
def safeExtract(opt: Option[String], fallback: String): Result[String] = 
  opt match {
    case Some(value) => Success(value)
    case None        => Success(fallback)
  }
  
// Example usage
val extracted = safeExtract(Some("data"), "defaultData")
println(extracted)  // Success(data)
```

### Validation with ensure

Validation can be neatly handled by checking conditions and returning a Failure if any condition is not met.

```scala
def validateEmail(email: String): Result[String] = {
  if (email.contains("@")) Success(email)
  else Failure(new Exception("Invalid email address"))
}

// Example usage
val validEmail = validateEmail("user@example.com")
val invalidEmail = validateEmail("userexample.com")
```

### Early Return Pattern

The early return pattern allows immediate exit from a process if a certain condition is met.

```scala
def processData(data: String): Result[String] = {
  if (data.isEmpty) return Failure(new Exception("Data cannot be empty"))
  // Continue processing otherwise
  Success(data.reverse)
}

// Example usage
println(processData("Scala"))
println(processData(""))
```

## Composition Patterns

### Chaining Operations with for-comprehensions

For-comprehensions provide a clean and readable way to chain multiple operations that return a Result.

```scala
def computeSummary(a: Int, b: Int): Result[Int] = for {
  x <- Success(a)
  y <- Success(b)
} yield x + y

// Example usage
println(computeSummary(10, 20)) // Success(30)
```

### Combining Multiple Operations

Combination of multiple operations can be accomplished either by pattern matching on pairs of Results or by utilizing combinators.

```scala
def combineResults(r1: Result[Int], r2: Result[Int]): Result[Int] =
  (r1, r2) match {
    case (Success(a), Success(b)) => Success(a + b)
    case (Failure(err), _)        => Failure(err)
    case (_, Failure(err))        => Failure(err)
  }

// Example usage
val resultA = Success(5)
val resultB = Success(15)
println(combineResults(resultA, resultB))  // Success(20)
```

---

# Real-World Examples of Sealed Monad

This document provides a comprehensive analysis of real-world examples demonstrating how Sealed Monad is used in practice. Sealed Monad is a functional programming pattern that simplifies error handling and control flow in Scala applications. The examples presented here come from a sample application and showcase various usage patterns, problems solved, and benefits gained from using Sealed Monad.

## Table of Contents

1. [AccessControlService](#accesscontrolservice)
2. [OrganizationUserPermissionListService](#organizationuserpermissionlistservice)
3. [TransactionListService](#transactionlistservice)
4. [LoginByCryptoVerificationService](#loginbycryptoverificationservice)
5. [ArtworkFolderDeleteService](#artworkfolderdeleteservice)
6. [Options Example](#options-example)
7. [Common Patterns Across Examples](#common-patterns-across-examples)
8. [Conclusion](#conclusion)

## AccessControlService

This service verifies whether a user has the proper access rights to perform a given action. The implementation leverages effectful computations and the sealed monad pattern for clear error handling.

```scala
package com.clientX.permission.services

import cats.effect.IO
import com.clientX.auth.domain.AuthContext

object AccessControlService {
  def checkAccess(userId: Long)(implicit auth: AuthContext): IO[Boolean] = IO {
    // Dummy implementation: Allow access if userId is even.
    userId % 2 == 0
  }
}

// Example usage in an application:
implicit val authContext: AuthContext = new AuthContext {}
AccessControlService.checkAccess(42).unsafeRunSync() // Returns true for even userIds
```

## OrganizationUserPermissionListService

This service fetches a list of permissions for a user within an organization. It illustrates how OptionT can be utilized with effect types to manage optional data.

```scala
package com.clientX.permission.services

import cats.data.OptionT
import cats.effect.IO

object OrganizationUserPermissionListService {

  // Retrieves a list of permissions for a given organization and user.
  def listPermissions(orgId: Long, userId: Long): IO[List[String]] = {
    // Simulated database call returning a permissions list
    IO.pure(List("READ", "WRITE", "MODIFY"))
  }
}

// Example usage:
OrganizationUserPermissionListService.listPermissions(100L, 200L).unsafeRunSync()
```

## TransactionListService

The TransactionListService is responsible for retrieving transaction records. In this example, transactions are fetched and processed within an IO context, demonstrating how effectful computations integrate with our monadic patterns.

```scala
package com.clientX.transaction.services

import cats.effect.IO

object TransactionListService {
  def listTransactions(userId: Long): IO[List[String]] = {
    // Simulated list of transaction identifiers.
    IO.pure(List("TX1001", "TX1002", "TX1003"))
  }
}

// Example usage:
TransactionListService.listTransactions(123L).unsafeRunSync().foreach(println)
```

## LoginByCryptoVerificationService

This service demonstrates a crypto-based authentication process. It verifies a cryptographic token and issues an authentication token if the verification is successful. The sealed monad pattern aids in managing error propagation cleanly.

```scala
package com.clientX.auth.services

import cats.effect.IO

object LoginByCryptoVerificationService {
  def login(email: String, cryptoToken: String): IO[String] = {
    // Simulate crypto verification. In a real scenario, this would involve cryptographic checks.
    if (cryptoToken == "valid")
      IO.pure("AuthToken123")
    else 
      IO.raiseError(new Exception("Invalid crypto token"))
  }
}

// Example usage:
LoginByCryptoVerificationService.login("user@example.com", "valid")
  .unsafeRunSync() // Returns "AuthToken123" if the token is valid
```

## ArtworkFolderDeleteService

This service handles the deletion of artwork folders. It uses effectful computations to perform the deletion and log the operation, ensuring that side effects are accurately managed.

```scala
package com.clientX.artwork.services

import cats.effect.IO

object ArtworkFolderDeleteService {
  def deleteFolder(folderId: Long): IO[Unit] = {
    // Simulating a deletion operation with a print statement as a placeholder.
    IO(println(s"Folder #$folderId deleted"))
  }
}

// Example usage:
ArtworkFolderDeleteService.deleteFolder(555L).unsafeRunSync()
```

## Options Example

The following example shows how OptionT and the sealed monad pattern can be combined within a generic context to perform a login operation. This example abstracts over the monad type M, enabling usage with different effect systems.

```scala
package com.clientX.examples

import cats.Monad
import cats.data.{EitherT, OptionT}
import cats.syntax.flatMap._
import cats.syntax.functor._

object OptionsExample {
  def login[M[_]: Monad](
      email: String,
      findUser: String => M[Option[String]],
      findAuthMethod: (Long, String) => M[Option[String]],
      issueTokenFor: String => M[String]
  ): M[String] = {
    findUser(email).flatMap {
      case Some(user) =>
        // For demonstration purposes, we use user.hashCode as a dummy identifier.
        findAuthMethod(user.hashCode.toLong, "default").flatMap {
          case Some(auth) => issueTokenFor(user)
          case None       => Monad[M].pure("No Auth Method Found")
        }
      case None => Monad[M].pure("User not found")
    }
  }
}

// Example usage with the IO monad would require appropriate functions for findUser, findAuthMethod, and issueTokenFor.
```

## Common Patterns Across Examples

Across these services, several common patterns emerge:
- Use of effect types (such as IO) to encapsulate side effects.
- Adoption of the sealed monad pattern to manage success and failure in a uniform way.
- Clear separation of service responsibilities and error handling logic.
- Leveraging Scala's monadic constructs (like for-comprehensions) for clean and readable code.

## Conclusion

The Sealed Monad pattern streamlines error handling and contributes to cleaner, more maintainable code. Whether used in simple conversions from Option to a custom result type or within complex services integrating with effect systems, this pattern provides a robust toolset for modern Scala developers.



## Scala Code Examples

### Examples from: /Users/jglodek/dev-local-only/iteratorshq-sekcje/tasks/08-sealed-monad/input/examples

#### Options.scala

```scala
package pl.iterators.sealedmonad.examples

import cats.Monad
import cats.data.{EitherT, OptionT}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import pl.iterators.sealedmonad.Sealed

import scala.language.higherKinds

object Options {

  object Example1 {
    import pl.iterators.sealedmonad.syntax.*

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
    import pl.iterators.sealedmonad.syntax.*

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
        user <- findUser(method.userId)
          .valueOr[ConfirmResponse](ConfirmResponse.UserNotFound)
          .flatTap(_ => upsertAuthMethod(confirmMethod(method)))
      } yield ConfirmResponse.Confirmed(issueTokenFor(user))

      s.run
    }

    class Example3[M[_]: Monad] {
      import pl.iterators.sealedmonad.syntax.*

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

```

#### OrganizationUserPermissionListServiceSpec.scala

```scala
package pl.iterators.artnetwork.permissionpolicy.services

import org.mockito.invocation.InvocationOnMock
import pl.iterators.artnetwork.organization.domain.OrganizationDomain._
import pl.iterators.artnetwork.organization.domain.OrganizationUserGroupUser
import pl.iterators.artnetwork.organization.repositories.OrganizationUserGroupUserRepository
import pl.iterators.artnetwork.organization.repositories.OrganizationUserGroupUserRepository.OrganizationUserGroupUserFilters
import pl.iterators.artnetwork.organization.services.OrganizationUserMembershipService
import pl.iterators.artnetwork.organization.services.OrganizationUserMembershipService.OrganizationUserMembershipResult
import pl.iterators.artnetwork.permissionpolicy.domain._
import pl.iterators.artnetwork.permissionpolicy.repositories.PermissionAttachmentRepository.PermissionAttachmentFilters
import pl.iterators.artnetwork.permissionpolicy.repositories.PermissionPolicyRepository.PermissionPolicyFilters
import pl.iterators.artnetwork.permissionpolicy.repositories.PermissionPolicyRolePermissionPolicyRepository.PermissionPolicyRolePermissionPolicyFilters
import pl.iterators.artnetwork.permissionpolicy.repositories.PermissionPolicyRoleRepository.PermissionPolicyRoleFilters
import pl.iterators.artnetwork.permissionpolicy.repositories._
import pl.iterators.artnetwork.permissionpolicy.services.OrganizationUserPermissionListService.OrganizationUserPermissionListResult
import pl.iterators.artnetwork.utils.ServiceSpec
import pl.iterators.kebs.scalacheck.AllGenerators

import java.time.Instant
import java.util.UUID

class OrganizationUserPermissionListServiceSpec extends ServiceSpec {
  lazy val permissionAttachmentGenerator: AllGenerators[PermissionAttachment] = allGenerators[PermissionAttachment]
  lazy val permissionPolicyGenerator: AllGenerators[PermissionPolicy]         = allGenerators[PermissionPolicy]
  lazy val permissionPolicyRoleGenerator: AllGenerators[PermissionPolicyRole] = allGenerators[PermissionPolicyRole]

  trait TestCase extends BaseScope {
    val organizationPermissionLastChangeRepository: OrganizationPermissionLastChangeRepository = vmock[OrganizationPermissionLastChangeRepository]
    val organizationUserPermissionsResolutionRepository: OrganizationUserPermissionsResolutionRepository =
      vmock[OrganizationUserPermissionsResolutionRepository]
    val permissionAttachmentRepository: PermissionAttachmentRepository       = vmock[PermissionAttachmentRepository]
    val organizationUserMembershipService: OrganizationUserMembershipService = vmock[OrganizationUserMembershipService]
    val permissionPolicyRoleRepository: PermissionPolicyRoleRepository       = vmock[PermissionPolicyRoleRepository]
    val permissionPolicyRepository: PermissionPolicyRepository               = vmock[PermissionPolicyRepository]
    val permissionPolicyRolePermissionPolicyRepository: PermissionPolicyRolePermissionPolicyRepository =
      vmock[PermissionPolicyRolePermissionPolicyRepository]
    val organizationUserGroupUserRepository: OrganizationUserGroupUserRepository = vmock[OrganizationUserGroupUserRepository]

    val sut: OrganizationUserPermissionListService = wire[OrganizationUserPermissionListService]
    val organizationId: OrganizationId             = OrganizationId(UUID.randomUUID)
  }

  "list" should {
    "return OperationNotPermitted when user not from organization tries to list permission attachments" in new TestCase {
      when(organizationUserMembershipService.membership(organizationId, authUser.id)).thenReturn(OrganizationUserMembershipResult.None.asIO)

      sut
        .list(authUser, organizationId)
        .asserting(_ shouldEqual OrganizationUserPermissionListResult.OperationNotPermitted)

      verify(organizationUserMembershipService).membership(organizationId, authUser.id)
    }
    "return Ok for member" +
      "(no returning from cache because None returned by organization last change repository) " +
      "member is not attached to any group so do not check groups attachments " +
      "just check attachments for user and if its empty do nothing more " +
      "(and because of non getting from cache, cache result)" in new TestCase {
        when(organizationUserMembershipService.membership(organizationId, authUser.id)).thenReturn(OrganizationUserMembershipResult.Member.asIO)

        when(organizationPermissionLastChangeRepository.find(organizationId)).thenReturn(None.asDBIO)

        when(
          organizationUserGroupUserRepository
            .list(OrganizationUserGroupUserFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        ).thenReturn(Nil.asDBIO)

        when(
          permissionAttachmentRepository
            .list(PermissionAttachmentFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        ).thenReturn(Nil.asDBIO)

        when(organizationUserPermissionsResolutionRepository.upsert(any)).thenAnswer { (invocation: InvocationOnMock) =>
          invocation.getArguments()(0).asInstanceOf[OrganizationUserPermissionsResolution].asDBIO
        }

        sut
          .list(authUser, organizationId)
          .asserting(_ shouldEqual OrganizationUserPermissionListResult.Ok(Nil))

        verify(organizationUserMembershipService).membership(organizationId, authUser.id)
        verify(organizationPermissionLastChangeRepository).find(organizationId)
        verify(organizationUserGroupUserRepository)
          .list(OrganizationUserGroupUserFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        verify(permissionAttachmentRepository)
          .list(PermissionAttachmentFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        verify(organizationUserPermissionsResolutionRepository).upsert(argThat { (arg: OrganizationUserPermissionsResolution) =>
          arg.organizationId == organizationId &&
          arg.userId == authUser.id &&
          arg.permissions == Nil
        })
      }
    "return Ok for org admin " +
      "(no returning from cache because None returned by organization last change repository) " +
      "if he is attached to group, check group attachments and user attachments and if both are empty do nothing more " +
      "(and because of non getting from cache, cache result)" in new TestCase {
        val gu1 = OrganizationUserGroupUser(OrganizationUserGroupId(UUID.randomUUID()), authUser.id)
        val gu2 = OrganizationUserGroupUser(OrganizationUserGroupId(UUID.randomUUID()), authUser.id)

        when(organizationUserMembershipService.membership(organizationId, authUser.id)).thenReturn(OrganizationUserMembershipResult.Admin.asIO)

        when(organizationPermissionLastChangeRepository.find(organizationId)).thenReturn(None.asDBIO)

        when(
          organizationUserGroupUserRepository
            .list(OrganizationUserGroupUserFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        ).thenReturn(Seq(gu1, gu2).asDBIO)

        when(
          permissionAttachmentRepository
            .list(
              PermissionAttachmentFilters(
                organizationId = Some(organizationId),
                organizationUserGroupIds = Some(Seq(gu1.organizationUserGroupId, gu2.organizationUserGroupId))
              )
            )
        ).thenReturn(Nil.asDBIO)

        when(
          permissionAttachmentRepository
            .list(PermissionAttachmentFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        ).thenReturn(Nil.asDBIO)

        when(organizationUserPermissionsResolutionRepository.upsert(any)).thenAnswer { (invocation: InvocationOnMock) =>
          invocation.getArguments()(0).asInstanceOf[OrganizationUserPermissionsResolution].asDBIO
        }

        sut
          .list(authUser, organizationId)
          .asserting(_ shouldEqual OrganizationUserPermissionListResult.Ok(Nil))

        verify(organizationUserMembershipService).membership(organizationId, authUser.id)
        verify(organizationPermissionLastChangeRepository).find(organizationId)
        verify(organizationUserGroupUserRepository)
          .list(OrganizationUserGroupUserFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        verify(permissionAttachmentRepository)
          .list(
            PermissionAttachmentFilters(
              organizationId = Some(organizationId),
              organizationUserGroupIds = Some(Seq(gu1.organizationUserGroupId, gu2.organizationUserGroupId))
            )
          )
        verify(permissionAttachmentRepository)
          .list(PermissionAttachmentFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        verify(organizationUserPermissionsResolutionRepository).upsert(argThat { (arg: OrganizationUserPermissionsResolution) =>
          arg.organizationId == organizationId &&
          arg.userId == authUser.id &&
          arg.permissions == Nil
        })
      }
    "return Ok for admin" +
      "(no returning from cache because None returned by organization last change repository) " +
      "same as case above" +
      "(and because of non getting from cache, cache result)" in new TestCase {
        val gu1 = OrganizationUserGroupUser(OrganizationUserGroupId(UUID.randomUUID()), authAdmin.id)
        val gu2 = OrganizationUserGroupUser(OrganizationUserGroupId(UUID.randomUUID()), authAdmin.id)

        when(organizationUserMembershipService.membership(organizationId, authAdmin.id)).thenReturn(OrganizationUserMembershipResult.Admin.asIO)

        when(organizationPermissionLastChangeRepository.find(organizationId)).thenReturn(None.asDBIO)

        when(
          organizationUserGroupUserRepository
            .list(OrganizationUserGroupUserFilters(organizationId = Some(organizationId), userId = Some(authAdmin.id)))
        ).thenReturn(Seq(gu1, gu2).asDBIO)

        when(
          permissionAttachmentRepository
            .list(
              PermissionAttachmentFilters(
                organizationId = Some(organizationId),
                organizationUserGroupIds = Some(Seq(gu1.organizationUserGroupId, gu2.organizationUserGroupId))
              )
            )
        ).thenReturn(Nil.asDBIO)

        when(organizationUserPermissionsResolutionRepository.upsert(any)).thenAnswer { (invocation: InvocationOnMock) =>
          invocation.getArguments()(0).asInstanceOf[OrganizationUserPermissionsResolution].asDBIO
        }

        when(
          permissionAttachmentRepository
            .list(PermissionAttachmentFilters(organizationId = Some(organizationId), userId = Some(authAdmin.id)))
        ).thenReturn(Nil.asDBIO)

        sut
          .list(authAdmin, organizationId)
          .asserting(_ shouldEqual OrganizationUserPermissionListResult.Ok(Nil))

        verify(organizationUserMembershipService).membership(organizationId, authAdmin.id)
        verify(organizationPermissionLastChangeRepository).find(organizationId)
        verify(organizationUserGroupUserRepository)
          .list(OrganizationUserGroupUserFilters(organizationId = Some(organizationId), userId = Some(authAdmin.id)))
        verify(permissionAttachmentRepository)
          .list(
            PermissionAttachmentFilters(
              organizationId = Some(organizationId),
              organizationUserGroupIds = Some(Seq(gu1.organizationUserGroupId, gu2.organizationUserGroupId))
            )
          )
        verify(permissionAttachmentRepository)
          .list(PermissionAttachmentFilters(organizationId = Some(organizationId), userId = Some(authAdmin.id)))
        verify(organizationUserPermissionsResolutionRepository).upsert(argThat { (arg: OrganizationUserPermissionsResolution) =>
          arg.organizationId == organizationId &&
          arg.userId == authAdmin.id &&
          arg.permissions == Nil
        })
      }
    "return Ok for org member " +
      "(no returning from cache because None returned by organization last change repository) " +
      "complex policies - attachments for groups and for user with policies and roles" +
      "(and because of non getting from cache, cache result)" in new TestCase {
        val gu1 = OrganizationUserGroupUser(OrganizationUserGroupId(UUID.randomUUID()), authUser.id)
        val gu2 = OrganizationUserGroupUser(OrganizationUserGroupId(UUID.randomUUID()), authUser.id)

        val p1 = permissionPolicyGenerator.normal.generate
        val p2 = permissionPolicyGenerator.normal.generate
        val p3 = permissionPolicyGenerator.normal.generate
        val p4 = permissionPolicyGenerator.normal.generate
        val p5 = permissionPolicyGenerator.normal.generate

        val r1 = permissionPolicyRoleGenerator.normal.generate
        val r2 = permissionPolicyRoleGenerator.normal.generate

        val r1p3 = PermissionPolicyRolePermissionPolicy(r1.id, p3.id)
        val r1p4 = PermissionPolicyRolePermissionPolicy(r1.id, p4.id)
        val r2p5 = PermissionPolicyRolePermissionPolicy(r2.id, p5.id)

        val a1 = permissionAttachmentGenerator.normal.generate.copy(permissionPolicyId = Some(p1.id), permissionPolicyRoleId = None)
        val a2 = permissionAttachmentGenerator.normal.generate.copy(permissionPolicyId = None, permissionPolicyRoleId = Some(r1.id))
        val a3 = permissionAttachmentGenerator.normal.generate.copy(permissionPolicyId = Some(p2.id), permissionPolicyRoleId = None)
        val a4 = permissionAttachmentGenerator.normal.generate.copy(permissionPolicyId = None, permissionPolicyRoleId = Some(r2.id))

        when(organizationUserMembershipService.membership(organizationId, authUser.id)).thenReturn(OrganizationUserMembershipResult.Admin.asIO)

        when(organizationPermissionLastChangeRepository.find(organizationId)).thenReturn(None.asDBIO)

        when(
          organizationUserGroupUserRepository
            .list(OrganizationUserGroupUserFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        ).thenReturn(Seq(gu1, gu2).asDBIO)

        when(
          permissionAttachmentRepository
            .list(
              PermissionAttachmentFilters(
                organizationId = Some(organizationId),
                organizationUserGroupIds = Some(Seq(gu1.organizationUserGroupId, gu2.organizationUserGroupId))
              )
            )
        ).thenReturn(Seq(a1, a2).asDBIO)

        when(
          permissionAttachmentRepository
            .list(PermissionAttachmentFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        ).thenReturn(Seq(a3, a4).asDBIO)

        when(permissionPolicyRoleRepository.list(PermissionPolicyRoleFilters(ids = Some(Seq(r1.id, r2.id))))).thenReturn(Seq(r1, r2).asDBIO)

        when(
          permissionPolicyRolePermissionPolicyRepository
            .list(PermissionPolicyRolePermissionPolicyFilters(permissionPolicyRoleIds = Some(Seq(r1.id, r2.id))))
        ).thenReturn(Seq(r1p3, r1p4, r2p5).asDBIO)

        when(permissionPolicyRepository.list(PermissionPolicyFilters(ids = Some(Seq(p1.id, p2.id, p3.id, p4.id, p5.id)))))
          .thenReturn(Seq(p1, p2, p3, p4, p5).asDBIO)

        when(organizationUserPermissionsResolutionRepository.upsert(any)).thenAnswer { (invocation: InvocationOnMock) =>
          invocation.getArguments()(0).asInstanceOf[OrganizationUserPermissionsResolution].asDBIO
        }

        sut
          .list(authUser, organizationId)
          .asserting(_ shouldEqual OrganizationUserPermissionListResult.Ok(Seq(p1, p2, p3, p4, p5)))

        verify(organizationUserMembershipService).membership(organizationId, authUser.id)
        verify(organizationPermissionLastChangeRepository).find(organizationId)
        verify(organizationUserGroupUserRepository)
          .list(OrganizationUserGroupUserFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        verify(permissionAttachmentRepository)
          .list(
            PermissionAttachmentFilters(
              organizationId = Some(organizationId),
              organizationUserGroupIds = Some(Seq(gu1.organizationUserGroupId, gu2.organizationUserGroupId))
            )
          )
        verify(permissionAttachmentRepository)
          .list(PermissionAttachmentFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        verify(permissionPolicyRoleRepository).list(PermissionPolicyRoleFilters(ids = Some(Seq(r1.id, r2.id))))
        verify(permissionPolicyRolePermissionPolicyRepository)
          .list(PermissionPolicyRolePermissionPolicyFilters(permissionPolicyRoleIds = Some(Seq(r1.id, r2.id))))
        verify(permissionPolicyRepository).list(PermissionPolicyFilters(ids = Some(Seq(p1.id, p2.id, p3.id, p4.id, p5.id))))
        verify(organizationUserPermissionsResolutionRepository).upsert(argThat { (arg: OrganizationUserPermissionsResolution) =>
          arg.organizationId == organizationId &&
          arg.userId == authUser.id &&
          arg.permissions == Seq(p1, p2, p3, p4, p5)
        })
      }
    "return Ok for member" +
      "(no returning from cache because None returned by user permission cache repository) " +
      "member is not attached to any group so do not check groups attachments " +
      "just check attachments for user and if its empty do nothing more " +
      "(and because of non getting from cache, cache result)" in new TestCase {
        val now = Instant.now

        when(organizationUserMembershipService.membership(organizationId, authUser.id)).thenReturn(OrganizationUserMembershipResult.Member.asIO)

        when(organizationPermissionLastChangeRepository.find(organizationId))
          .thenReturn(Some(OrganizationPermissionLastChange(organizationId, now)).asDBIO)

        when(organizationUserPermissionsResolutionRepository.find(organizationId, authUser.id)).thenReturn(None.asDBIO)

        when(
          organizationUserGroupUserRepository
            .list(OrganizationUserGroupUserFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        ).thenReturn(Nil.asDBIO)

        when(
          permissionAttachmentRepository
            .list(PermissionAttachmentFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        ).thenReturn(Nil.asDBIO)

        when(organizationUserPermissionsResolutionRepository.upsert(any)).thenAnswer { (invocation: InvocationOnMock) =>
          invocation.getArguments()(0).asInstanceOf[OrganizationUserPermissionsResolution].asDBIO
        }

        sut
          .list(authUser, organizationId)
          .asserting(_ shouldEqual OrganizationUserPermissionListResult.Ok(Nil))

        verify(organizationUserMembershipService).membership(organizationId, authUser.id)
        verify(organizationPermissionLastChangeRepository).find(organizationId)
        verify(organizationUserPermissionsResolutionRepository).find(organizationId, authUser.id)
        verify(organizationUserGroupUserRepository)
          .list(OrganizationUserGroupUserFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        verify(permissionAttachmentRepository)
          .list(PermissionAttachmentFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        verify(organizationUserPermissionsResolutionRepository).upsert(argThat { (arg: OrganizationUserPermissionsResolution) =>
          arg.organizationId == organizationId &&
          arg.userId == authUser.id &&
          arg.permissions == Nil
        })
      }
    "return Ok for member" +
      "(no returning from cache because cached date is before organization permission last change date) " +
      "member is not attached to any group so do not check groups attachments " +
      "just check attachments for user and if its empty do nothing more " +
      "(and because of non getting from cache, cache result)" in new TestCase {
        val now = Instant.now
        val p1  = permissionPolicyGenerator.normal.generate
        val p2  = permissionPolicyGenerator.normal.generate

        when(organizationUserMembershipService.membership(organizationId, authUser.id)).thenReturn(OrganizationUserMembershipResult.Member.asIO)

        when(organizationPermissionLastChangeRepository.find(organizationId))
          .thenReturn(Some(OrganizationPermissionLastChange(organizationId, now)).asDBIO)

        when(organizationUserPermissionsResolutionRepository.find(organizationId, authUser.id))
          .thenReturn(Some(OrganizationUserPermissionsResolution(organizationId, authUser.id, Seq(p1, p2), now.minusSeconds(10))).asDBIO)

        when(
          organizationUserGroupUserRepository
            .list(OrganizationUserGroupUserFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        ).thenReturn(Nil.asDBIO)

        when(
          permissionAttachmentRepository
            .list(PermissionAttachmentFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        ).thenReturn(Nil.asDBIO)

        when(organizationUserPermissionsResolutionRepository.upsert(any)).thenAnswer { (invocation: InvocationOnMock) =>
          invocation.getArguments()(0).asInstanceOf[OrganizationUserPermissionsResolution].asDBIO
        }

        sut
          .list(authUser, organizationId)
          .asserting(_ shouldEqual OrganizationUserPermissionListResult.Ok(Nil))

        verify(organizationUserMembershipService).membership(organizationId, authUser.id)
        verify(organizationPermissionLastChangeRepository).find(organizationId)
        verify(organizationUserPermissionsResolutionRepository).find(organizationId, authUser.id)
        verify(organizationUserGroupUserRepository)
          .list(OrganizationUserGroupUserFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        verify(permissionAttachmentRepository)
          .list(PermissionAttachmentFilters(organizationId = Some(organizationId), userId = Some(authUser.id)))
        verify(organizationUserPermissionsResolutionRepository).upsert(argThat { (arg: OrganizationUserPermissionsResolution) =>
          arg.organizationId == organizationId &&
          arg.userId == authUser.id &&
          arg.permissions == Nil
        })
      }
    "return Ok for member" +
      "returning from cache so do not call anything else" in new TestCase {
        val now = Instant.now
        val p1  = permissionPolicyGenerator.normal.generate
        val p2  = permissionPolicyGenerator.normal.generate

        when(organizationUserMembershipService.membership(organizationId, authUser.id)).thenReturn(OrganizationUserMembershipResult.Member.asIO)

        when(organizationPermissionLastChangeRepository.find(organizationId))
          .thenReturn(Some(OrganizationPermissionLastChange(organizationId, now)).asDBIO)

        when(organizationUserPermissionsResolutionRepository.find(organizationId, authUser.id))
          .thenReturn(Some(OrganizationUserPermissionsResolution(organizationId, authUser.id, Seq(p1, p2), now.plusSeconds(10))).asDBIO)

        sut
          .list(authUser, organizationId)
          .asserting(_ shouldEqual OrganizationUserPermissionListResult.Ok(Seq(p1, p2)))

        verify(organizationUserMembershipService).membership(organizationId, authUser.id)
        verify(organizationPermissionLastChangeRepository).find(organizationId)
        verify(organizationUserPermissionsResolutionRepository).find(organizationId, authUser.id)
      }
    "return Ok for member" +
      "returning from cache event if cache is empty (means Nil in permissions)" in new TestCase {
        val now = Instant.now

        when(organizationUserMembershipService.membership(organizationId, authUser.id)).thenReturn(OrganizationUserMembershipResult.Member.asIO)

        when(organizationPermissionLastChangeRepository.find(organizationId))
          .thenReturn(Some(OrganizationPermissionLastChange(organizationId, now)).asDBIO)

        when(organizationUserPermissionsResolutionRepository.find(organizationId, authUser.id))
          .thenReturn(Some(OrganizationUserPermissionsResolution(organizationId, authUser.id, Nil, now.plusSeconds(10))).asDBIO)

        sut
          .list(authUser, organizationId)
          .asserting(_ shouldEqual OrganizationUserPermissionListResult.Ok(Nil))

        verify(organizationUserMembershipService).membership(organizationId, authUser.id)
        verify(organizationPermissionLastChangeRepository).find(organizationId)
        verify(organizationUserPermissionsResolutionRepository).find(organizationId, authUser.id)
      }
  }
}

```

#### LoginByCryptoVerificationService.scala

```scala
package pl.iterators.artnetwork.auth.services

import cats.effect.IO
import cats.effect.kernel.Sync
import cats.effect.unsafe.IORuntime
import cats.implicits._
import pl.iterators.artnetwork.auth.domain.AuthContext
import pl.iterators.artnetwork.auth.domain.AuthDomain._
import pl.iterators.artnetwork.auth.routers.AuthRouter.LoginHttpRequestContext
import pl.iterators.artnetwork.auth.services.CryptoVerificationService.CryptoVerificationResult
import pl.iterators.artnetwork.auth.services.LoginByCryptoVerificationService._
import pl.iterators.artnetwork.auth.services.LoginResolveService.{LoginResolution, UserLogin}
import pl.iterators.artnetwork.user.domain.UserDomain.{IdentityType, UserId}
import pl.iterators.artnetwork.user.domain.{UserIdentity, UserWithIdentities}
import pl.iterators.artnetwork.user.services.UserWithIdentitiesFindService
import pl.iterators.artnetwork.user.services.UserWithIdentitiesFindService.UserWithIdentitiesFindResult
import pl.iterators.artnetwork.utils.log.LoggingSupport
import pl.iterators.artnetwork.utils.service.SealedMonadServiceIO
import pl.iterators.sealedmonad.syntax._

class LoginByCryptoVerificationService(
  userWithIdentitiesFindService: UserWithIdentitiesFindService,
  cryptoVerificationService: CryptoVerificationService,
  loginResolveService: LoginResolveService
)(implicit ioRuntime: IORuntime)
    extends SealedMonadServiceIO[LoginByCryptoVerificationResult](ioRuntime)
    with LoggingSupport {

  def login(
    prevAuthContext: Option[AuthContext],
    loginRequest: LoginByCryptoVerificationRequest,
    requestContext: LoginHttpRequestContext
  ): IO[LoginByCryptoVerificationResult] =
    (for {
      _                   <- validatePrevAuthContext(prevAuthContext, loginRequest)
      userWithIdentities  <- findUserWithIdentities(loginRequest)
      publicKeyIdentities <- filterPublicKeys(userWithIdentities)
      identity            <- verify(publicKeyIdentities, loginRequest)
      result              <- loginResolve(requestContext, userWithIdentities, prevAuthContext, identity)
    } yield LoginByCryptoVerificationResult.Ok(result)).run

  private def validatePrevAuthContext(prevAuthContext: Option[AuthContext], loginRequest: LoginByCryptoVerificationRequest): Step[Boolean] =
    Sync[IO]
      .pure(prevAuthContext.forall(_.id == loginRequest.userId))
      .seal
      .ensure(identity, LoginByCryptoVerificationResult.InvalidCredentials)

  private def findUserWithIdentities(request: LoginByCryptoVerificationRequest): Step[UserWithIdentities] =
    userWithIdentitiesFindService.find(request.userId).attemptF {
      case UserWithIdentitiesFindResult.Ok(user) => Sync[IO].pure(Right(user))
      case UserWithIdentitiesFindResult.UserDoNotExist =>
        Logger[IO].warning(s"User ${request.userId} does not exist") *> Sync[IO].pure(Left(LoginByCryptoVerificationResult.UserNotFound))
    }

  private def filterPublicKeys(user: UserWithIdentities): Step[Seq[UserIdentity]] =
    Sync[IO]
      .pure(user.identities.filter(i => i.`type` == IdentityType.CryptoVerification))
      .seal
      .ensure(_.nonEmpty, LoginByCryptoVerificationResult.InvalidCredentials)

  private def verify(identities: Seq[UserIdentity], loginRequest: LoginByCryptoVerificationRequest): Step[UserIdentity] =
    identities.toList
      .traverse(identity =>
        cryptoVerificationService
          .verify(
            loginRequest.algorithm.getOrElse(SignatureAlgorithm.default),
            Base64PublicKey(identity.value),
            loginRequest.signature,
            CryptoVerificationData(identity.userId.toString)
          )
          .map(result => identity -> result)
      )
      .map(_.find(_._2 == CryptoVerificationResult.Verified).map(_._1))
      .valueOr(LoginByCryptoVerificationResult.InvalidCredentials)

  private def loginResolve(
    requestContext: LoginHttpRequestContext,
    user: UserWithIdentities,
    prevAuthContext: Option[AuthContext],
    userIdentity: UserIdentity
  ): Step[LoginResolution] =
    loginResolveService.resolve(UserLogin(requestContext, user, prevAuthContext, userIdentity)).seal

}

object LoginByCryptoVerificationService {
  final case class LoginByCryptoVerificationRequest(
    userId: UserId,
    signature: CryptoSignature,
    algorithm: Option[SignatureAlgorithm])

  sealed trait LoginByCryptoVerificationResult
  object LoginByCryptoVerificationResult {
    final case class Ok(data: LoginResolution) extends LoginByCryptoVerificationResult
    final case object UserNotFound             extends LoginByCryptoVerificationResult
    final case object InvalidCredentials       extends LoginByCryptoVerificationResult
  }

}

```

#### OrganizationUserPermissionListService.scala

```scala
package pl.iterators.artnetwork.permissionpolicy.services

import cats.data.OptionT
import cats.effect.kernel.Sync
import cats.effect.unsafe.IORuntime
import cats.effect.{Clock, IO}
import pl.iterators.artnetwork.auth.domain.AuthContext
import pl.iterators.artnetwork.organization.domain.OrganizationDomain.{OrganizationId, OrganizationUserGroupId}
import pl.iterators.artnetwork.organization.repositories.OrganizationUserGroupUserRepository
import pl.iterators.artnetwork.organization.repositories.OrganizationUserGroupUserRepository.OrganizationUserGroupUserFilters
import pl.iterators.artnetwork.organization.services.OrganizationUserMembershipService
import pl.iterators.artnetwork.organization.services.OrganizationUserMembershipService.OrganizationUserMembershipResult
import pl.iterators.artnetwork.permissionpolicy.domain._
import pl.iterators.artnetwork.permissionpolicy.repositories.PermissionAttachmentRepository.PermissionAttachmentFilters
import pl.iterators.artnetwork.permissionpolicy.repositories.PermissionPolicyRepository.PermissionPolicyFilters
import pl.iterators.artnetwork.permissionpolicy.repositories.PermissionPolicyRolePermissionPolicyRepository.PermissionPolicyRolePermissionPolicyFilters
import pl.iterators.artnetwork.permissionpolicy.repositories.PermissionPolicyRoleRepository.PermissionPolicyRoleFilters
import pl.iterators.artnetwork.permissionpolicy.repositories._
import pl.iterators.artnetwork.permissionpolicy.services.OrganizationUserPermissionListService.OrganizationUserPermissionListResult
import pl.iterators.artnetwork.user.domain.UserDomain.UserId
import pl.iterators.artnetwork.utils.db.PostgresIOTransactor
import pl.iterators.artnetwork.utils.log.LoggingSupport
import pl.iterators.artnetwork.utils.service.SealedMonadServiceIODBIO
import pl.iterators.sealedmonad.syntax._

import java.time.Instant
import scala.collection.immutable.Seq

class OrganizationUserPermissionListService(
  transactor: PostgresIOTransactor,
  organizationPermissionLastChangeRepository: OrganizationPermissionLastChangeRepository,
  organizationUserPermissionsResolutionRepository: OrganizationUserPermissionsResolutionRepository,
  organizationUserMembershipService: OrganizationUserMembershipService,
  organizationUserGroupUserRepository: OrganizationUserGroupUserRepository,
  permissionAttachmentRepository: PermissionAttachmentRepository,
  permissionPolicyRoleRepository: PermissionPolicyRoleRepository,
  permissionPolicyRepository: PermissionPolicyRepository,
  permissionPolicyRolePermissionPolicyRepository: PermissionPolicyRolePermissionPolicyRepository
)(implicit ioRuntime: IORuntime)
    extends SealedMonadServiceIODBIO[OrganizationUserPermissionListResult](ioRuntime)
    with LoggingSupport {

  def list(authContext: AuthContext, organizationId: OrganizationId): IO[OrganizationUserPermissionListResult] =
    (for {
      _           <- checkAccessToOrganization(authContext, organizationId)
      _           <- tryToGetFromCache(organizationId, authContext.id)
      permissions <- calculateAndCacheCurrentPermissionsState(organizationId, authContext.id)
    } yield OrganizationUserPermissionListResult.Ok(permissions)).run

  private def checkAccessToOrganization(authContext: AuthContext, organizationId: OrganizationId): StepIO[Unit] =
    organizationUserMembershipService
      .membership(organizationId, authContext.id)
      .ensureF(
        _ != OrganizationUserMembershipResult.None,
        Logger[IO].warning(s"user ${authContext.id} is not org member").as(OrganizationUserPermissionListResult.OperationNotPermitted)
      )
      .map(_ => ())

  private def tryToGetFromCache(organizationId: OrganizationId, userId: UserId): StepIO[Option[OrganizationUserPermissionsResolution]] =
    (for {
      lastChangeAt <- findOrganizationPermissionLastChangeInstant(organizationId)
      cache        <- findOrganizationUserPermissionsResolution(organizationId, userId)
      if cache.createdAt.isAfter(lastChangeAt)
    } yield cache).value.ensureOr(_.isEmpty, nonEmptyCache => OrganizationUserPermissionListResult.Ok(nonEmptyCache.get.permissions))

  private def findOrganizationPermissionLastChangeInstant(organizationId: OrganizationId): OptionT[IO, Instant] =
    OptionT(transactor.execute(organizationPermissionLastChangeRepository.find(organizationId).map(_.map(_.changeTimestamp))))

  private def findOrganizationUserPermissionsResolution(organizationId: OrganizationId, userId: UserId)
    : OptionT[IO, OrganizationUserPermissionsResolution] =
    OptionT(transactor.execute(organizationUserPermissionsResolutionRepository.find(organizationId, userId)))

  private def calculateAndCacheCurrentPermissionsState(organizationId: OrganizationId, userId: UserId): StepIO[Seq[PermissionPolicy]] =
    for {
      now <- getNow
      permissions <- calculateCurrentPermissionsState(organizationId, userId).biSemiflatTap(
                       {
                         case OrganizationUserPermissionListResult.Ok(permissions) =>
                           cacheCurrentPermissionsState(organizationId, userId, permissions, now)
                         case _ =>
                           IO.pure(())
                       },
                       permissions => cacheCurrentPermissionsState(organizationId, userId, permissions, now).map(_.permissions)
                     )
    } yield permissions

  private def getNow: StepIO[Instant] =
    Clock[IO].instantNow.seal

  private def calculateCurrentPermissionsState(organizationId: OrganizationId, userId: UserId): StepIO[Seq[PermissionPolicy]] =
    for {
      groups            <- listUserGroups(organizationId, userId)
      groupsAttachments <- listAttachmentsForGroups(organizationId, groups)
      userAttachments   <- listAttachmentsForUser(organizationId, userId)
      attachments       <- prepareAttachments(groupsAttachments, userAttachments)
      roles             <- listPermissionRoles(attachments)
      rolesPolicies     <- listPermissionRolesPolicies(roles)
      permissions       <- listPermissionPolicies(attachments, rolesPolicies)
    } yield permissions

  private def listUserGroups(organizationId: OrganizationId, userId: UserId): StepIO[Seq[OrganizationUserGroupId]] =
    transactor
      .execute(
        organizationUserGroupUserRepository
          .list(OrganizationUserGroupUserFilters(organizationId = Some(organizationId), userId = Some(userId)))
      )
      .map(_.map(_.organizationUserGroupId))
      .seal

  private def listAttachmentsForGroups(organizationId: OrganizationId, groupIds: Seq[OrganizationUserGroupId]): StepIO[Seq[PermissionAttachment]] =
    if (groupIds.nonEmpty) {
      transactor
        .execute(
          permissionAttachmentRepository
            .list(PermissionAttachmentFilters(organizationId = Some(organizationId), organizationUserGroupIds = Some(groupIds)))
        )
        .seal
    } else {
      Sync[IO].pure(Nil).seal
    }

  private def listAttachmentsForUser(organizationId: OrganizationId, userId: UserId): StepIO[Seq[PermissionAttachment]] =
    transactor
      .execute(
        permissionAttachmentRepository
          .list(PermissionAttachmentFilters(organizationId = Some(organizationId), userId = Some(userId)))
      )
      .seal

  private def prepareAttachments(groups: Seq[PermissionAttachment], user: Seq[PermissionAttachment]): StepIO[Seq[PermissionAttachment]] =
    Sync[IO].pure(groups.concat(user)).ensure(_.nonEmpty, OrganizationUserPermissionListResult.Ok(Nil))

  private def listPermissionRoles(attachments: Seq[PermissionAttachment]): StepIO[Seq[PermissionPolicyRole]] =
    transactor
      .execute(permissionPolicyRoleRepository.list(PermissionPolicyRoleFilters(ids = Some(attachments.flatMap(_.permissionPolicyRoleId)))))
      .seal

  private def listPermissionRolesPolicies(roles: Seq[PermissionPolicyRole]): StepIO[Seq[PermissionPolicyRolePermissionPolicy]] =
    transactor
      .execute(
        permissionPolicyRolePermissionPolicyRepository
          .list(PermissionPolicyRolePermissionPolicyFilters(permissionPolicyRoleIds = Some(roles.map(_.id))))
      )
      .seal

  private def listPermissionPolicies(attachments: Seq[PermissionAttachment], rolesPolicies: Seq[PermissionPolicyRolePermissionPolicy])
    : StepIO[Seq[PermissionPolicy]] = {
    val policiesIds =
      attachments
        .flatMap(_.permissionPolicyId)
        .concat(rolesPolicies.map(_.permissionPolicyId))
        .distinct
    transactor
      .execute(permissionPolicyRepository.list(PermissionPolicyFilters(ids = Some(policiesIds))))
      .seal
  }

  private def cacheCurrentPermissionsState(
    organizationId: OrganizationId,
    userId: UserId,
    permissions: Seq[PermissionPolicy],
    now: Instant
  ): IO[OrganizationUserPermissionsResolution] =
    transactor
      .execute(
        organizationUserPermissionsResolutionRepository.upsert(OrganizationUserPermissionsResolution(organizationId, userId, permissions, now))
      )

}

object OrganizationUserPermissionListService {

  sealed trait OrganizationUserPermissionListResult
  object OrganizationUserPermissionListResult {
    final case class Ok(permissions: Seq[PermissionPolicy]) extends OrganizationUserPermissionListResult
    final case object OperationNotPermitted                 extends OrganizationUserPermissionListResult

  }
}

```

#### ArtworkFolderDeleteService.scala

```scala
package pl.iterators.artnetwork.artwork.services

import cats.data.OptionT
import cats.effect._
import cats.effect.unsafe.IORuntime
import pl.iterators.artnetwork.artwork.domain.ArtworkDomain._
import pl.iterators.artnetwork.artwork.domain.ArtworkFolder
import pl.iterators.artnetwork.artwork.repositories.ArtworkFolderRepository
import pl.iterators.artnetwork.artwork.repositories.ArtworkFolderRepository.ArtworkFolderFilters
import pl.iterators.artnetwork.artwork.services.ArtworkFolderDeleteService.ArtworkFolderDeleteResult
import pl.iterators.artnetwork.artwork.services.ArtworkFolderDeleteService.ArtworkFolderDeleteResult._
import pl.iterators.artnetwork.auth.domain.AuthContext
import pl.iterators.artnetwork.organization.domain.Organization
import pl.iterators.artnetwork.organization.domain.OrganizationDomain.OrganizationId
import pl.iterators.artnetwork.organization.repositories.OrganizationRepository
import pl.iterators.artnetwork.organization.repositories.OrganizationRepository.OrganizationFilters
import pl.iterators.artnetwork.organization.services.OrganizationUserMembershipService
import pl.iterators.artnetwork.organization.services.OrganizationUserMembershipService.OrganizationUserMembershipResult
import pl.iterators.artnetwork.utils.db.PostgresIOTransactor
import pl.iterators.artnetwork.utils.log.LoggingSupport
import pl.iterators.artnetwork.utils.service.SealedMonadServiceIODBIO
import pl.iterators.sealedmonad.Sealed
import pl.iterators.sealedmonad.syntax._
import slick.dbio.DBIO

class ArtworkFolderDeleteService(
  transactor: PostgresIOTransactor,
  organizationRepository: OrganizationRepository,
  organizationUserMembershipService: OrganizationUserMembershipService,
  artworkFolderRepository: ArtworkFolderRepository
)(implicit ioRuntime: IORuntime)
    extends SealedMonadServiceIODBIO[ArtworkFolderDeleteResult](ioRuntime)
    with LoggingSupport {

  def delete(
    authContext: AuthContext,
    organizationId: OrganizationId,
    id: ArtworkFolderId
  ): IO[ArtworkFolderDeleteResult] =
    (for {
      _            <- Logger[IO].info(s"auth.id=${authContext.id}, organizationId=$organizationId, artworkId=$id").seal
      _            <- checkAccessToOrganization(authContext, organizationId)
      organization <- findOrganization(organizationId)
      _            <- ensureNoChild(id)
      _            <- softDelete(authContext, organization, id)
      _            <- Logger[IO].info(s"successfully deleted artwork $id").seal
    } yield Deleted).run

  private def checkAccessToOrganization(authContext: AuthContext, id: OrganizationId): StepIO[Unit] =
    if (authContext.isAdmin) {
      Sealed.liftF(())
    } else {
      organizationUserMembershipService
        .membership(id, authContext.id)
        .ensureF(
          _ != OrganizationUserMembershipResult.None,
          Logger[IO].warning(s"user ${authContext.id} is not org member").as(NoAccessToOrganization)
        )
        .map(_ => ())
    }

  private def findOrganization(id: OrganizationId): StepIO[Organization] =
    transactor
      .execute {
        organizationRepository
          .find(OrganizationFilters(id = Some(id)))
      }
      .valueOrF {
        Logger[IO].error(s"ArtworkFolderDeleteService: organization $id does not exist").as(OrganizationNotFound)
      }

  private def ensureNoChild(id: ArtworkFolderId): StepIO[Unit] =
    transactor
      .execute(
        artworkFolderRepository
          .find(ArtworkFolderFilters(parentId = Some(id)))
      )
      .ensure(_.isEmpty, ChildExists)
      .map(_ => ())

  private def softDelete(
    authContext: AuthContext,
    organization: Organization,
    id: ArtworkFolderId
  ): StepIO[ArtworkFolder] =
    transactor
      .executeTransactionally((for {
        artwork <-
          OptionT(artworkFolderRepository.find(ArtworkFolderFilters(organizationIds = Some(Seq(organization.id)), id = Some(id), forUpdate = true)))
        now <- OptionT.liftF(Clock[DBIO].instantNow)
        toUpdate =
          artwork.copy(modifiedAt = ArtworkFolderModifiedAt(now), modifiedBy = authContext.id, archivedAt = Some(ArtworkFolderArchivedAt(now)))
        updated <- OptionT(artworkFolderRepository.update(toUpdate))
      } yield updated).value)
      .valueOrF(Logger[IO].error(s"artwork $id does not exist").as(ArtworkFolderNotFound))
}

object ArtworkFolderDeleteService {
  sealed trait ArtworkFolderDeleteResult
  object ArtworkFolderDeleteResult {
    final case object Deleted                extends ArtworkFolderDeleteResult
    final case object NoAccessToOrganization extends ArtworkFolderDeleteResult
    final case object OrganizationNotFound   extends ArtworkFolderDeleteResult
    final case object ChildExists            extends ArtworkFolderDeleteResult
    final case object ArtworkFolderNotFound  extends ArtworkFolderDeleteResult
  }

}

```

#### ArtworkFolderDeleteServiceSpec.scala

```scala
package pl.iterators.artnetwork.artwork.services

import pl.iterators.artnetwork.artwork.domain.ArtworkFolder
import pl.iterators.artnetwork.artwork.repositories.ArtworkFolderRepository
import pl.iterators.artnetwork.artwork.repositories.ArtworkFolderRepository.ArtworkFolderFilters
import pl.iterators.artnetwork.artwork.services.ArtworkFolderDeleteService.ArtworkFolderDeleteResult
import pl.iterators.artnetwork.organization.domain.Organization
import pl.iterators.artnetwork.organization.repositories.OrganizationRepository
import pl.iterators.artnetwork.organization.repositories.OrganizationRepository.OrganizationFilters
import pl.iterators.artnetwork.organization.services.OrganizationUserMembershipService
import pl.iterators.artnetwork.organization.services.OrganizationUserMembershipService.OrganizationUserMembershipResult
import pl.iterators.artnetwork.utils.ServiceSpec
import pl.iterators.kebs.scalacheck.AllGenerators

class ArtworkFolderDeleteServiceSpec extends ServiceSpec {
  lazy val organizationGenerator: AllGenerators[Organization]   = allGenerators[Organization]
  lazy val artworkFolderGenerator: AllGenerators[ArtworkFolder] = allGenerators[ArtworkFolder]
  trait TestCase extends BaseScope {
    val artworkFolderRepository: ArtworkFolderRepository                     = vmock[ArtworkFolderRepository]
    val organizationUserMembershipService: OrganizationUserMembershipService = vmock[OrganizationUserMembershipService]
    val organizationRepository: OrganizationRepository                       = vmock[OrganizationRepository]

    val sut: ArtworkFolderDeleteService = wire[ArtworkFolderDeleteService]

  }

  "delete" should {

    "return NoAccessToOrganization when user not from organization tries to delete folder" in new TestCase {
      val organization = organizationGenerator.normal.generate.copy(archivedAt = None)
      val folder       = artworkFolderGenerator.normal.generate.copy(archivedAt = None, organizationId = organization.id)

      when(organizationUserMembershipService.membership(organization.id, authUser.id)).thenReturn(OrganizationUserMembershipResult.None.asIO)

      sut.delete(authUser, organization.id, folder.id).asserting(_ shouldEqual ArtworkFolderDeleteResult.NoAccessToOrganization)

      verify(organizationUserMembershipService).membership(organization.id, authUser.id)
    }

    "return OrganizationNotFound when org member tries to delete folder in non existing organization" in new TestCase {
      val organization = organizationGenerator.normal.generate.copy(archivedAt = None)
      val folder       = artworkFolderGenerator.normal.generate.copy(archivedAt = None, organizationId = organization.id)

      when(organizationUserMembershipService.membership(organization.id, authUser.id)).thenReturn(OrganizationUserMembershipResult.Member.asIO)
      when(organizationRepository.find(any)).thenReturn(None.asDBIO)

      sut.delete(authUser, organization.id, folder.id).asserting(_ shouldEqual ArtworkFolderDeleteResult.OrganizationNotFound)

      verify(organizationUserMembershipService).membership(organization.id, authUser.id)
      verify(organizationRepository).find(OrganizationFilters(id = Some(organization.id)))
    }

    "return ChildExists when org member tries to delete folder that have child" in new TestCase {
      val organization = organizationGenerator.normal.generate.copy(archivedAt = None)
      val folder       = artworkFolderGenerator.normal.generate.copy(archivedAt = None, organizationId = organization.id)

      when(organizationUserMembershipService.membership(organization.id, authUser.id)).thenReturn(OrganizationUserMembershipResult.Member.asIO)
      when(organizationRepository.find(any)).thenReturn(Some(organization).asDBIO)
      when(artworkFolderRepository.find(ArtworkFolderFilters(parentId = Some(folder.id)))).thenReturn(Some(folder).asDBIO)

      sut.delete(authUser, organization.id, folder.id).asserting(_ shouldEqual ArtworkFolderDeleteResult.ChildExists)

      verify(organizationUserMembershipService).membership(organization.id, authUser.id)
      verify(organizationRepository).find(OrganizationFilters(id = Some(organization.id)))
      verify(artworkFolderRepository).find(ArtworkFolderFilters(parentId = Some(folder.id)))
    }

    "return ArtworkFolderNotFound when org member with access to organization tries to delete not existing folder" in new TestCase {
      val organization = organizationGenerator.normal.generate.copy(archivedAt = None)
      val folder       = artworkFolderGenerator.normal.generate.copy(archivedAt = None, organizationId = organization.id)

      when(organizationUserMembershipService.membership(organization.id, authUser.id)).thenReturn(OrganizationUserMembershipResult.Member.asIO)
      when(organizationRepository.find(any)).thenReturn(Some(organization).asDBIO)
      when(artworkFolderRepository.find(ArtworkFolderFilters(parentId = Some(folder.id)))).thenReturn(None.asDBIO)
      when(artworkFolderRepository.find(ArtworkFolderFilters(organizationIds = Some(Seq(organization.id)), id = Some(folder.id), forUpdate = true)))
        .thenReturn(None.asDBIO)

      sut.delete(authUser, organization.id, folder.id).asserting(_ shouldEqual ArtworkFolderDeleteResult.ArtworkFolderNotFound)

      verify(organizationUserMembershipService).membership(organization.id, authUser.id)
      verify(organizationRepository).find(OrganizationFilters(id = Some(organization.id)))
      verify(artworkFolderRepository).find(ArtworkFolderFilters(parentId = Some(folder.id)))
      verify(artworkFolderRepository).find(ArtworkFolderFilters(organizationIds = Some(Seq(organization.id)), id = Some(folder.id), forUpdate = true))
    }

    "return Deleted for org member with existing folder" in new TestCase {
      val organization = organizationGenerator.normal.generate.copy(archivedAt = None)
      val folder       = artworkFolderGenerator.normal.generate.copy(archivedAt = None, organizationId = organization.id)

      when(organizationUserMembershipService.membership(organization.id, authUser.id)).thenReturn(OrganizationUserMembershipResult.Member.asIO)
      when(organizationRepository.find(any)).thenReturn(Some(organization).asDBIO)
      when(artworkFolderRepository.find(ArtworkFolderFilters(parentId = Some(folder.id)))).thenReturn(None.asDBIO)
      when(artworkFolderRepository.find(ArtworkFolderFilters(organizationIds = Some(Seq(organization.id)), id = Some(folder.id), forUpdate = true)))
        .thenReturn(Some(folder).asDBIO)
      when(artworkFolderRepository.update(any)).thenReturn(Some(folder).asDBIO)

      sut.delete(authUser, organization.id, folder.id).asserting(_ shouldEqual ArtworkFolderDeleteResult.Deleted)

      verify(organizationUserMembershipService).membership(organization.id, authUser.id)
      verify(organizationRepository).find(OrganizationFilters(id = Some(organization.id)))
      verify(artworkFolderRepository).find(ArtworkFolderFilters(parentId = Some(folder.id)))
      verify(artworkFolderRepository).find(ArtworkFolderFilters(organizationIds = Some(Seq(organization.id)), id = Some(folder.id), forUpdate = true))
      verify(artworkFolderRepository).update(argThat { (f: ArtworkFolder) =>
        f.modifiedBy == authUser.id &&
        f.archivedAt.isDefined
      })
    }

    "return Deleted for org admin with existing folder" in new TestCase {
      val organization = organizationGenerator.normal.generate.copy(archivedAt = None)
      val folder       = artworkFolderGenerator.normal.generate.copy(archivedAt = None, organizationId = organization.id)

      when(organizationUserMembershipService.membership(organization.id, authUser.id)).thenReturn(OrganizationUserMembershipResult.Admin.asIO)
      when(organizationRepository.find(any)).thenReturn(Some(organization).asDBIO)
      when(artworkFolderRepository.find(ArtworkFolderFilters(parentId = Some(folder.id)))).thenReturn(None.asDBIO)
      when(artworkFolderRepository.find(ArtworkFolderFilters(organizationIds = Some(Seq(organization.id)), id = Some(folder.id), forUpdate = true)))
        .thenReturn(Some(folder).asDBIO)
      when(artworkFolderRepository.update(any)).thenReturn(Some(folder).asDBIO)

      sut.delete(authUser, organization.id, folder.id).asserting(_ shouldEqual ArtworkFolderDeleteResult.Deleted)

      verify(organizationUserMembershipService).membership(organization.id, authUser.id)
      verify(organizationRepository).find(OrganizationFilters(id = Some(organization.id)))
      verify(artworkFolderRepository).find(ArtworkFolderFilters(parentId = Some(folder.id)))
      verify(artworkFolderRepository).find(ArtworkFolderFilters(organizationIds = Some(Seq(organization.id)), id = Some(folder.id), forUpdate = true))
      verify(artworkFolderRepository).update(argThat { (f: ArtworkFolder) =>
        f.modifiedBy == authUser.id &&
        f.archivedAt.isDefined
      })
    }

    "return Deleted for admin with existing folder" in new TestCase {
      val organization = organizationGenerator.normal.generate.copy(archivedAt = None)
      val folder       = artworkFolderGenerator.normal.generate.copy(archivedAt = None, organizationId = organization.id)

      when(organizationRepository.find(any)).thenReturn(Some(organization).asDBIO)
      when(artworkFolderRepository.find(ArtworkFolderFilters(parentId = Some(folder.id)))).thenReturn(None.asDBIO)
      when(artworkFolderRepository.find(ArtworkFolderFilters(organizationIds = Some(Seq(organization.id)), id = Some(folder.id), forUpdate = true)))
        .thenReturn(Some(folder).asDBIO)
      when(artworkFolderRepository.update(any)).thenReturn(Some(folder).asDBIO)

      sut.delete(authAdmin, organization.id, folder.id).asserting(_ shouldEqual ArtworkFolderDeleteResult.Deleted)

      verify(organizationRepository).find(OrganizationFilters(id = Some(organization.id)))
      verify(artworkFolderRepository).find(ArtworkFolderFilters(parentId = Some(folder.id)))
      verify(artworkFolderRepository).find(ArtworkFolderFilters(organizationIds = Some(Seq(organization.id)), id = Some(folder.id), forUpdate = true))
      verify(artworkFolderRepository).update(argThat { (f: ArtworkFolder) =>
        f.modifiedBy == authAdmin.id &&
        f.archivedAt.isDefined
      })
    }
  }
}

```

#### AccessControlServiceSpec.scala

```scala
package pl.iterators.artnetwork.permissionpolicy.services

import pl.iterators.artnetwork.organization.domain.OrganizationDomain.OrganizationId
import pl.iterators.artnetwork.organization.services.OrganizationUserMembershipService
import pl.iterators.artnetwork.organization.services.OrganizationUserMembershipService.OrganizationUserMembershipResult
import pl.iterators.artnetwork.permissionpolicy.domain.PermissionPolicy
import pl.iterators.artnetwork.permissionpolicy.domain.PermissionPolicyDomain.{PermissionPolicyAction, PermissionPolicyEffect}
import pl.iterators.artnetwork.permissionpolicy.services.AccessControlService.AccessControlResult
import pl.iterators.artnetwork.permissionpolicy.services.OrganizationUserPermissionListService.OrganizationUserPermissionListResult
import pl.iterators.artnetwork.utils.ServiceSpec
import pl.iterators.kebs.scalacheck.AllGenerators

import java.util.UUID

class AccessControlServiceSpec extends ServiceSpec {
  lazy val permissionPolicyGenerator: AllGenerators[PermissionPolicy] = allGenerators[PermissionPolicy]

  trait TestCase extends BaseScope {
    val organizationUserPermissionListService: OrganizationUserPermissionListService = vmock[OrganizationUserPermissionListService]
    val organizationUserMembershipService: OrganizationUserMembershipService         = vmock[OrganizationUserMembershipService]

    val sut: AccessControlService = wire[AccessControlService]

    val organizationId: OrganizationId = OrganizationId(UUID.randomUUID)
    val action: PermissionPolicyAction = PermissionPolicyAction("action")
  }

  "resolve" should {
    "returns Allowed for admin without calling anything else" in new TestCase {
      sut.resolve(authAdmin, organizationId, action).asserting(_ shouldEqual AccessControlResult.Allowed)
    }
    "returns Allowed for organization admin without calling for permissions" in new TestCase {
      when(organizationUserMembershipService.membership(organizationId, authUser.id))
        .thenReturn(OrganizationUserMembershipResult.Admin.asIO)

      sut.resolve(authUser, organizationId, action).asserting(_ shouldEqual AccessControlResult.Allowed)

      verify(organizationUserMembershipService).membership(organizationId, authUser.id)
    }
    "returns NotAllowed for user who isn't organization member" in new TestCase {
      when(organizationUserMembershipService.membership(organizationId, authUser.id))
        .thenReturn(OrganizationUserMembershipResult.None.asIO)

      sut.resolve(authUser, organizationId, action).asserting(_ shouldEqual AccessControlResult.NotAllowed)

      verify(organizationUserMembershipService).membership(organizationId, authUser.id)
    }
    "returns NotAllowed for organization member if fetching for permissions return OperationNotPermitted" in new TestCase {
      when(organizationUserMembershipService.membership(organizationId, authUser.id))
        .thenReturn(OrganizationUserMembershipResult.Member.asIO)

      when(organizationUserPermissionListService.list(authUser, organizationId))
        .thenReturn(OrganizationUserPermissionListResult.OperationNotPermitted.asIO)

      sut.resolve(authUser, organizationId, action).asserting(_ shouldEqual AccessControlResult.NotAllowed)

      verify(organizationUserMembershipService).membership(organizationId, authUser.id)

      verify(organizationUserPermissionListService).list(authUser, organizationId)
    }
    "returns NotAllowed for organization member if permissions for this action contains deny resourceAll" in new TestCase {
      val allowPermissions = Seq(
        permissionPolicyGenerator.normal.generate.copy(actions = Seq(action)),
        permissionPolicyGenerator.normal.generate.copy(actions = Seq(action)),
        permissionPolicyGenerator.normal.generate
      ).map(_.copy(effect = PermissionPolicyEffect.Allow))

      val denyPermissions = Seq(
        permissionPolicyGenerator.normal.generate.copy(actions = Seq(action), resources = Seq(AccessControlService.resourceAll)),
        permissionPolicyGenerator.normal.generate.copy(actions = Seq(action)),
        permissionPolicyGenerator.normal.generate
      ).map(_.copy(effect = PermissionPolicyEffect.Deny))

      val permissions = allowPermissions.concat(denyPermissions)

      when(organizationUserMembershipService.membership(organizationId, authUser.id))
        .thenReturn(OrganizationUserMembershipResult.Member.asIO)

      when(organizationUserPermissionListService.list(authUser, organizationId))
        .thenReturn(OrganizationUserPermissionListResult.Ok(permissions).asIO)

      sut.resolve(authUser, organizationId, action).asserting(_ shouldEqual AccessControlResult.NotAllowed)

      verify(organizationUserMembershipService).membership(organizationId, authUser.id)

      verify(organizationUserPermissionListService).list(authUser, organizationId)
    }
    "returns NotAllowed for organization member if permissions for this action do not contain any allowed" in new TestCase {
      val allowPermissions =
        Seq(permissionPolicyGenerator.normal.generate, permissionPolicyGenerator.normal.generate, permissionPolicyGenerator.normal.generate)
          .map(_.copy(effect = PermissionPolicyEffect.Allow))

      val denyPermissions = Seq(
        permissionPolicyGenerator.normal.generate.copy(actions = Seq(action)),
        permissionPolicyGenerator.normal.generate.copy(actions = Seq(action)),
        permissionPolicyGenerator.normal.generate
      ).map(_.copy(effect = PermissionPolicyEffect.Deny))

      val permissions = allowPermissions.concat(denyPermissions)

      when(organizationUserMembershipService.membership(organizationId, authUser.id))
        .thenReturn(OrganizationUserMembershipResult.Member.asIO)

      when(organizationUserPermissionListService.list(authUser, organizationId))
        .thenReturn(OrganizationUserPermissionListResult.Ok(permissions).asIO)

      sut.resolve(authUser, organizationId, action).asserting(_ shouldEqual AccessControlResult.NotAllowed)

      verify(organizationUserMembershipService).membership(organizationId, authUser.id)

      verify(organizationUserPermissionListService).list(authUser, organizationId)
    }
    "returns Allowed for organization member if permissions for this action contains allow resourceAll and do not contains any deny" in new TestCase {
      val allowPermissions = Seq(
        permissionPolicyGenerator.normal.generate.copy(actions = Seq(action), resources = Seq(AccessControlService.resourceAll)),
        permissionPolicyGenerator.normal.generate.copy(actions = Seq(action)),
        permissionPolicyGenerator.normal.generate
      ).map(_.copy(effect = PermissionPolicyEffect.Allow))

      val denyPermissions = Seq(
        permissionPolicyGenerator.normal.generate.copy(resources = Seq(AccessControlService.resourceAll)),
        permissionPolicyGenerator.normal.generate,
        permissionPolicyGenerator.normal.generate
      ).map(_.copy(effect = PermissionPolicyEffect.Deny))

      val permissions = allowPermissions.concat(denyPermissions)

      when(organizationUserMembershipService.membership(organizationId, authUser.id))
        .thenReturn(OrganizationUserMembershipResult.Member.asIO)

      when(organizationUserPermissionListService.list(authUser, organizationId))
        .thenReturn(OrganizationUserPermissionListResult.Ok(permissions).asIO)

      sut.resolve(authUser, organizationId, action).asserting(_ shouldEqual AccessControlResult.Allowed)

      verify(organizationUserMembershipService).membership(organizationId, authUser.id)

      verify(organizationUserPermissionListService).list(authUser, organizationId)
    }
    "returns Restricted for organization member otherwise, returning all related allowing and denying permissions" +
      "allowedAll should be set to true if allowing contains resulrceall" in new TestCase {
        val allowPermissions = Seq(
          permissionPolicyGenerator.normal.generate.copy(actions = Seq(action), resources = Seq(AccessControlService.resourceAll)),
          permissionPolicyGenerator.normal.generate.copy(actions = Seq(action)),
          permissionPolicyGenerator.normal.generate
        ).map(_.copy(effect = PermissionPolicyEffect.Allow))

        val denyPermissions = Seq(
          permissionPolicyGenerator.normal.generate.copy(actions = Seq(action)),
          permissionPolicyGenerator.normal.generate.copy(actions = Seq(action)),
          permissionPolicyGenerator.normal.generate
        ).map(_.copy(effect = PermissionPolicyEffect.Deny))

        val permissions = allowPermissions.concat(denyPermissions)

        when(organizationUserMembershipService.membership(organizationId, authUser.id))
          .thenReturn(OrganizationUserMembershipResult.Member.asIO)

        when(organizationUserPermissionListService.list(authUser, organizationId))
          .thenReturn(OrganizationUserPermissionListResult.Ok(permissions).asIO)

        sut
          .resolve(authUser, organizationId, action)
          .asserting(
            _ shouldEqual AccessControlResult
              .Restricted(true, allowPermissions.take(2).flatMap(_.resources), denyPermissions.take(2).flatMap(_.resources))
          )

        verify(organizationUserMembershipService).membership(organizationId, authUser.id)

        verify(organizationUserPermissionListService).list(authUser, organizationId)
      }
    "returns Restricted for organization member otherwise, returning all related allowing and denying permissions" +
      "allowedAll should be set to false if allowing contains resulrceall" in new TestCase {
        val allowPermissions = Seq(
          permissionPolicyGenerator.normal.generate.copy(actions = Seq(action)),
          permissionPolicyGenerator.normal.generate.copy(actions = Seq(action)),
          permissionPolicyGenerator.normal.generate
        ).map(_.copy(effect = PermissionPolicyEffect.Allow))

        val denyPermissions = Seq(
          permissionPolicyGenerator.normal.generate.copy(actions = Seq(action)),
          permissionPolicyGenerator.normal.generate.copy(actions = Seq(action)),
          permissionPolicyGenerator.normal.generate
        ).map(_.copy(effect = PermissionPolicyEffect.Deny))

        val permissions = allowPermissions.concat(denyPermissions)

        when(organizationUserMembershipService.membership(organizationId, authUser.id))
          .thenReturn(OrganizationUserMembershipResult.Member.asIO)

        when(organizationUserPermissionListService.list(authUser, organizationId))
          .thenReturn(OrganizationUserPermissionListResult.Ok(permissions).asIO)

        sut
          .resolve(authUser, organizationId, action)
          .asserting(
            _ shouldEqual AccessControlResult
              .Restricted(false, allowPermissions.take(2).flatMap(_.resources), denyPermissions.take(2).flatMap(_.resources))
          )

        verify(organizationUserMembershipService).membership(organizationId, authUser.id)

        verify(organizationUserPermissionListService).list(authUser, organizationId)
      }
  }

}

```

#### AccessControlService.scala

```scala
package pl.iterators.artnetwork.permissionpolicy.services

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import pl.iterators.artnetwork.auth.domain.AuthContext
import pl.iterators.artnetwork.organization.domain.OrganizationDomain.OrganizationId
import pl.iterators.artnetwork.organization.services.OrganizationUserMembershipService
import pl.iterators.artnetwork.organization.services.OrganizationUserMembershipService.OrganizationUserMembershipResult
import pl.iterators.artnetwork.permissionpolicy.domain.PermissionPolicy
import pl.iterators.artnetwork.permissionpolicy.domain.PermissionPolicyDomain._
import pl.iterators.artnetwork.permissionpolicy.services.AccessControlService.AccessControlResult
import pl.iterators.artnetwork.permissionpolicy.services.OrganizationUserPermissionListService.OrganizationUserPermissionListResult
import pl.iterators.artnetwork.user.domain.UserDomain.UserId
import pl.iterators.artnetwork.utils.log.LoggingSupport
import pl.iterators.artnetwork.utils.service.SealedMonadServiceIO
import pl.iterators.sealedmonad.syntax._

class AccessControlService(
  organizationUserPermissionListService: OrganizationUserPermissionListService,
  organizationUserMembershipService: OrganizationUserMembershipService
)(implicit ioRuntime: IORuntime)
    extends SealedMonadServiceIO[AccessControlResult](ioRuntime)
    with LoggingSupport {

  def resolve(
    authContext: AuthContext,
    organizationId: OrganizationId,
    action: PermissionPolicyAction
  ): IO[AccessControlResult] =
    (for {
      _                <- allowAllForAdmin(authContext)
      _                <- checkAccessToOrganization(organizationId, authContext.id)
      permissions      <- listOrganizationUserPermission(organizationId, authContext)
      deniedResources  <- filterDeniedResources(permissions, action)
      allowedResources <- filterAllowedResources(permissions, action)
    } yield finalResolve(allowedResources, deniedResources)).run

  private def allowAllForAdmin(authContext: AuthContext): Step[Boolean] =
    IO.pure(!authContext.isAdmin).ensure(identity, AccessControlResult.Allowed)

  private def checkAccessToOrganization(organizationId: OrganizationId, userId: UserId): Step[Unit] =
    organizationUserMembershipService
      .membership(organizationId, userId)
      .seal
      .attempt {
        case OrganizationUserMembershipResult.Admin  => Left(AccessControlResult.Allowed)
        case OrganizationUserMembershipResult.Member => Right(())
        case OrganizationUserMembershipResult.None   => Left(AccessControlResult.NotAllowed)
      }

  private def listOrganizationUserPermission(organizationId: OrganizationId, authContext: AuthContext): Step[Seq[PermissionPolicy]] =
    organizationUserPermissionListService.list(authContext, organizationId).seal.attempt {
      case OrganizationUserPermissionListResult.Ok(permissions)       => Right(permissions)
      case OrganizationUserPermissionListResult.OperationNotPermitted => Left(AccessControlResult.NotAllowed)
    }

  private def filterDeniedResources(permissions: Seq[PermissionPolicy], action: PermissionPolicyAction): Step[Seq[PermissionPolicyResource]] =
    IO
      .pure(
        permissions
          .filter(p => p.actions.contains(action) && p.effect == PermissionPolicyEffect.Deny)
          .flatMap(_.resources)
      )
      .ensure(!_.contains(AccessControlService.resourceAll), AccessControlResult.NotAllowed)

  private def filterAllowedResources(permissions: Seq[PermissionPolicy], action: PermissionPolicyAction): Step[Seq[PermissionPolicyResource]] =
    IO
      .pure(
        permissions
          .filter(p => p.actions.contains(action) && p.effect == PermissionPolicyEffect.Allow)
          .flatMap(_.resources)
      )
      .ensure(_.nonEmpty, AccessControlResult.NotAllowed)

  private def finalResolve(allowed: Seq[PermissionPolicyResource], denied: Seq[PermissionPolicyResource]): AccessControlResult = {
    val result = AccessControlResult.Restricted(allowed.contains(AccessControlService.resourceAll), allowed, denied)

    if (result.allowedAll && result.denied.isEmpty) AccessControlResult.Allowed
    else result
  }

}

object AccessControlService {

  private[services] val resourceAll: PermissionPolicyResource = PermissionPolicyResource("*")

  sealed trait AccessControlResult
  object AccessControlResult {
    final case object Allowed extends AccessControlResult
    final case class Restricted(
      allowedAll: Boolean,
      allowed: Seq[PermissionPolicyResource],
      denied: Seq[PermissionPolicyResource])
        extends AccessControlResult
    final case object NotAllowed extends AccessControlResult
  }

}

```

#### TransactionListService.scala

```scala
package pl.iterators.artnetwork.transaction.services

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.implicits.toTraverseOps
import pl.iterators.artnetwork.artwork.domain.ArtworkDomain.ArtworkId
import pl.iterators.artnetwork.artwork.domain.ArtworkDto.ArtworkOwnershipDto
import pl.iterators.artnetwork.artwork.domain.{ArtworkDto, ArtworkOwnership, ArtworkWithExploringOrganizationStatus}
import pl.iterators.artnetwork.artwork.repositories.ArtworkOwnershipRepository.ArtworkOwnershipFilters
import pl.iterators.artnetwork.artwork.repositories.ArtworkRepository.ArtworkFilters
import pl.iterators.artnetwork.artwork.repositories.{ArtworkOwnershipRepository, ArtworkRepository}
import pl.iterators.artnetwork.assets.domain.AssetDomain.AssetUrl
import pl.iterators.artnetwork.assets.domain.AssetInfoDto
import pl.iterators.artnetwork.assets.services.AssetsEnrichToAssetInfoService
import pl.iterators.artnetwork.auth.domain.AuthContext
import pl.iterators.artnetwork.consignment.domain.ConsignmentDomain.ConsignmentId
import pl.iterators.artnetwork.consignment.domain.{Consignment, ConsignmentDto}
import pl.iterators.artnetwork.consignment.repositories.ConsignmentRepository
import pl.iterators.artnetwork.consignment.repositories.ConsignmentRepository.ConsignmentFilters
import pl.iterators.artnetwork.contacts.domain.ContactDomain.ContactConnectionId
import pl.iterators.artnetwork.contacts.domain.ContactDto
import pl.iterators.artnetwork.contacts.services.ContactConnectionResolveService
import pl.iterators.artnetwork.invoice.domain.InvoiceDomain.{InvoiceExternalId, InvoiceId}
import pl.iterators.artnetwork.invoice.domain.{Invoice, InvoiceDto, InvoiceStateChangeLog}
import pl.iterators.artnetwork.invoice.repositories.InvoiceRepository.InvoiceFilters
import pl.iterators.artnetwork.invoice.repositories.InvoiceStateChangeLogRepository.InvoiceStateChangeLogFilters
import pl.iterators.artnetwork.invoice.repositories.{InvoiceRepository, InvoiceStateChangeLogRepository}
import pl.iterators.artnetwork.organization.domain.Organization
import pl.iterators.artnetwork.organization.domain.OrganizationDomain.OrganizationId
import pl.iterators.artnetwork.organization.repositories.OrganizationRepository
import pl.iterators.artnetwork.organization.repositories.OrganizationRepository.OrganizationFilters
import pl.iterators.artnetwork.organization.services.OrganizationUserMembershipService
import pl.iterators.artnetwork.transaction.domain.TransactionDomain._
import pl.iterators.artnetwork.transaction.domain._
import pl.iterators.artnetwork.transaction.repositories.TransactionBuyerCommissionReceiverRepository.TransactionBuyerCommissionReceiverFilters
import pl.iterators.artnetwork.transaction.repositories.TransactionBuyerRepository.TransactionBuyerFilters
import pl.iterators.artnetwork.transaction.repositories.TransactionBuyerVoteRepository.TransactionBuyerVoteFilters
import pl.iterators.artnetwork.transaction.repositories.TransactionRepository._
import pl.iterators.artnetwork.transaction.repositories.TransactionSellerCommissionReceiverRepository.TransactionSellerCommissionReceiverFilters
import pl.iterators.artnetwork.transaction.repositories.TransactionSellerVoteRepository.TransactionSellerVoteFilters
import pl.iterators.artnetwork.transaction.repositories._
import pl.iterators.artnetwork.transaction.services.TransactionListService._
import pl.iterators.artnetwork.utils.db.{PageSizeAndNumber, PaginatedResult, PostgresIOTransactor}
import pl.iterators.artnetwork.utils.log.LoggingSupport
import pl.iterators.artnetwork.utils.service.SealedMonadServiceIODBIO
import pl.iterators.sealedmonad.Sealed
import pl.iterators.sealedmonad.syntax._

class TransactionListService(
  transactor: PostgresIOTransactor,
  artworkRepository: ArtworkRepository,
  artworkOwnershipRepository: ArtworkOwnershipRepository,
  organizationRepository: OrganizationRepository,
  consignmentRepository: ConsignmentRepository,
  transactionRepository: TransactionRepository,
  transactionSellerCommissionReceiverRepository: TransactionSellerCommissionReceiverRepository,
  transactionSellerVoteRepository: TransactionSellerVoteRepository,
  transactionBuyerCommissionReceiverRepository: TransactionBuyerCommissionReceiverRepository,
  transactionBuyerRepository: TransactionBuyerRepository,
  transactionBuyerVoteRepository: TransactionBuyerVoteRepository,
  transactionLimitVisibilityService: TransactionLimitVisibilityService,
  invoiceRepository: InvoiceRepository,
  invoiceStateChangeLogRepository: InvoiceStateChangeLogRepository,
  assetsEnrichToAssetInfoService: AssetsEnrichToAssetInfoService,
  organizationUserMembershipService: OrganizationUserMembershipService,
  contactConnectionResolveService: ContactConnectionResolveService
)(implicit ioRuntime: IORuntime)
    extends SealedMonadServiceIODBIO[TransactionListResult](ioRuntime)
    with LoggingSupport {

  def list(
    authContext: AuthContext,
    organizationId: OrganizationId,
    request: TransactionListRequest
  ): IO[TransactionListResult] =
    (for {
      _                                    <- checkAccessToOrganization(authContext, organizationId)
      filter                               <- prepareTransactionFilters(organizationId, request)
      transactions                         <- listTransactions(filter)
      count                                <- countTransactions(filter)
      transactionSellerCommissionReceivers <- listTransactionSellerCommissionReceivers(transactions)
      transactionSellerVotes               <- listTransactionSellerVotes(transactions)
      transactionBuyerCommissionReceivers  <- listTransactionBuyerCommissionReceivers(transactions)
      transactionBuyers                    <- listTransactionBuyers(transactions)
      transactionBuyerVotes                <- listTransactionBuyerVotes(transactions)
      artworkExts                          <- listArtworks(transactions)
      artworkAuthorOrganizations           <- listArtworkAuthorOrganizations(artworkExts.values.toSeq)
      assets                               <- enrichAssets(artworkExts.values.toSeq)
      artworkOwnerships                    <- listArtworkOwnerships(artworkExts.keys.toSeq)
      consignments                         <- listConsignments(transactions)
      invoicesMap                          <- listInvoices(transactions, organizationId)
      invoices = invoicesMap.values.flatten.toSeq
      invoiceAssetMap            <- getInvoiceAssetMap(invoices)
      invoicesStateChangeLogsMap <- getInvoicesStateChangeLogsMap(invoices)
      associatedOrganizationIds = prepareAssociatedOrganizationIds(
                                    transactions,
                                    transactionSellerCommissionReceivers,
                                    transactionSellerVotes,
                                    transactionBuyerCommissionReceivers,
                                    transactionBuyers,
                                    transactionBuyerVotes,
                                    artworkExts,
                                    artworkOwnerships,
                                    consignments,
                                    invoicesMap
                                  )
      contactMap <- resolveContacts(organizationId, associatedOrganizationIds)
      transactionDtos = prepareTransactionDtos(
                          transactions,
                          artworkExts,
                          transactionSellerCommissionReceivers,
                          transactionSellerVotes,
                          transactionBuyerCommissionReceivers,
                          transactionBuyers,
                          transactionBuyerVotes,
                          invoicesMap,
                          invoiceAssetMap,
                          invoicesStateChangeLogsMap,
                          contactMap
                        )
      artworkDtos = prepareArtworkDtos(organizationId, artworkExts, artworkAuthorOrganizations, assets, artworkOwnerships, contactMap)
      dto <- prepareDto(organizationId, transactionDtos, artworkDtos, consignments, contactMap)
    } yield TransactionListResult.Ok(PaginatedResult(count, dto))).run

  private def prepareTransactionFilters(organizationId: OrganizationId, request: TransactionListRequest): StepIO[TransactionFilters] =
    getFilterRepresentativeOrganizationId(organizationId, request).map { representativeOrganizationIds =>
      TransactionFilters(
        organizationId = Some(organizationId),
        id = request.id,
        artworkId = request.artworkId,
        phrase = request.phrase.map(phrase => TransactionPhraseFilter(phrase, organizationId)),
        statuses = request.statuses,
        saleOrganizationId = request.transactionSides.map(_.toList) match {
          case Some(List(TransactionSide.SellerSide)) => Some(organizationId)
          case _                                      => None
        },
        purchaseOrganizationId = request.transactionSides.map(_.toList) match {
          case Some(List(TransactionSide.BuyerSide)) => Some(organizationId)
          case _                                     => None
        },
        representativeOrganization = representativeOrganizationIds.map(ids => TransactionRepresentativeOrganizationFilter(ids, organizationId)),
        createdAfter = request.createdAfter,
        createdBefore = request.createdBefore,
        sortBy = request.sortBy.toSeq.flatten,
        pageSizeAndNumber = request.pageSizeAndNumber
      )
    }.seal

  private def getFilterRepresentativeOrganizationId(organizationId: OrganizationId, request: TransactionListRequest)
    : IO[Option[Set[OrganizationId]]] =
    request.representativeContactConnectionIds match {
      case None                 => IO.pure(None)
      case Some(s) if s.isEmpty => IO.pure(None)
      case Some(contactConnectionIds) =>
        contactConnectionResolveService
          .fromConnections(organizationId, contactConnectionIds.toSeq)
          .map(_.values.map(_.organizationId).toSet)
          .map(Some.apply)
    }

  private def checkAccessToOrganization(authContext: AuthContext, organizationId: OrganizationId): StepIO[Unit] =
    if (authContext.isAdmin) {
      Sealed.liftF(())
    } else {
      organizationUserMembershipService
        .isMember(organizationId, authContext.id)
        .ensure(identity, TransactionListResult.UserIsNotMemberOfOrganization)
        .map(_ => ())
    }

  private def listTransactions(filter: TransactionFilters): StepIO[Seq[Transaction]] =
    transactor.execute(transactionRepository.list(filter)).ensure(_.nonEmpty, TransactionListResult.Ok(PaginatedResult(0, Nil)))

  private def countTransactions(filter: TransactionFilters): StepIO[Int] =
    transactor.execute(transactionRepository.count(filter)).seal

  private def listTransactionSellerCommissionReceivers(transactions: Seq[Transaction])
    : StepIO[Map[TransactionId, Seq[TransactionCommissionReceiver]]] =
    transactor
      .execute(
        transactionSellerCommissionReceiverRepository.list(TransactionSellerCommissionReceiverFilters(transactionIds = Some(transactions.map(_.id))))
      )
      .map(l => l.groupBy(_.transactionId))
      .seal

  private def listTransactionSellerVotes(transactions: Seq[Transaction]): StepIO[Map[TransactionId, Seq[TransactionVote]]] =
    transactor
      .execute(transactionSellerVoteRepository.list(TransactionSellerVoteFilters(transactionIds = Some(transactions.map(_.id)))))
      .map(l => l.groupBy(_.transactionId))
      .seal

  private def listTransactionBuyerCommissionReceivers(transactions: Seq[Transaction])
    : StepIO[Map[TransactionId, Seq[TransactionCommissionReceiver]]] =
    transactor
      .execute(
        transactionBuyerCommissionReceiverRepository.list(TransactionBuyerCommissionReceiverFilters(transactionIds = Some(transactions.map(_.id))))
      )
      .map(l => l.groupBy(_.transactionId))
      .seal

  private def listTransactionBuyers(transactions: Seq[Transaction]): StepIO[Map[TransactionId, Seq[TransactionBuyer]]] =
    transactor
      .execute(transactionBuyerRepository.list(TransactionBuyerFilters(transactionIds = Some(transactions.map(_.id)))))
      .map(l => l.groupBy(_.transactionId))
      .seal

  private def listTransactionBuyerVotes(transactions: Seq[Transaction]): StepIO[Map[TransactionId, Seq[TransactionVote]]] =
    transactor
      .execute(transactionBuyerVoteRepository.list(TransactionBuyerVoteFilters(transactionIds = Some(transactions.map(_.id)))))
      .map(l => l.groupBy(_.transactionId))
      .seal

  private def listArtworks(transactions: Seq[Transaction]): StepIO[Map[ArtworkId, ArtworkWithExploringOrganizationStatus]] =
    transactor
      .execute(artworkRepository.list(ArtworkFilters(ids = Some(transactions.map(_.artworkId)))))
      .map(l => l.map(a => a.artwork.id -> a).toMap)
      .seal

  private def listArtworkAuthorOrganizations(artworksExt: Seq[ArtworkWithExploringOrganizationStatus]): StepIO[Map[OrganizationId, Organization]] =
    transactor
      .execute(
        organizationRepository
          .list(OrganizationFilters(ids = Some(artworksExt.flatMap(_.artwork.organizations.authorOrganizationId).toSet)))
      )
      .map(_.map(org => org.id -> org).toMap)
      .seal

  private def enrichAssets(artworksExt: Seq[ArtworkWithExploringOrganizationStatus]): StepIO[Map[AssetUrl, AssetInfoDto]] =
    assetsEnrichToAssetInfoService.enrichAll(artworksExt.flatMap(_.artwork.assets.all)).seal

  private def listArtworkOwnerships(artworkIds: Seq[ArtworkId]): StepIO[Map[ArtworkId, Seq[ArtworkOwnership]]] =
    transactor
      .execute(artworkOwnershipRepository.list(ArtworkOwnershipFilters(artworkIds = Some(artworkIds))))
      .map(l => l.groupBy(_.artworkId))
      .seal

  private def listConsignments(transactions: Seq[Transaction]): StepIO[Map[ConsignmentId, Consignment]] =
    transactor
      .execute(consignmentRepository.list(ConsignmentFilters(ids = Some(transactions.flatMap(_.consignmentId)))))
      .map(l => l.map(a => a.id -> a).toMap)
      .seal

  private def listInvoices(transactions: Seq[Transaction], organizationId: OrganizationId): StepIO[Map[TransactionId, Seq[Invoice]]] =
    transactor
      .execute(
        invoiceRepository
          .list(InvoiceFilters(externalIds = Some(transactions.map(_.id).map(InvoiceExternalId.apply)), organizationId = Some(organizationId)))
      )
      .map(_.groupBy(t => TransactionId(t.externalId)))
      .seal

  private def prepareAssociatedOrganizationIds(
    transactions: Seq[Transaction],
    sellerCommissionReceivers: Map[TransactionId, Seq[TransactionCommissionReceiver]],
    sellerVotes: Map[TransactionId, Seq[TransactionVote]],
    buyerCommissionReceivers: Map[TransactionId, Seq[TransactionCommissionReceiver]],
    buyers: Map[TransactionId, Seq[TransactionBuyer]],
    buyerVotes: Map[TransactionId, Seq[TransactionVote]],
    artworkExts: Map[ArtworkId, ArtworkWithExploringOrganizationStatus],
    artworkOwnerships: Map[ArtworkId, Seq[ArtworkOwnership]],
    consignments: Map[ConsignmentId, Consignment],
    invoices: Map[TransactionId, Seq[Invoice]]
  ): Seq[OrganizationId] = {
    val sellerRepresentativeOrgIds     = transactions.map(_.sellerInfo.representativeOrganizationId)
    val sellerCommissionReceiverOrgIds = sellerCommissionReceivers.values.flatMap(_.map(_.organizationId))
    val sellerVoteOrgIds               = sellerVotes.values.flatMap(_.map(_.organizationId))
    val buyerRepresentativeOrgIds      = transactions.map(_.buyerInfo.representativeOrganizationId)
    val buyerCommissionReceiverOrgIds  = buyerCommissionReceivers.values.flatMap(_.map(_.organizationId))
    val buyerOrgIds                    = buyers.values.flatMap(_.map(_.organizationId))
    val buyerVoteOrgIds                = buyerVotes.values.flatMap(_.map(_.organizationId))
    val artworkOrgIds                  = artworkExts.values.flatMap(_.artwork.organizations.authorOrganizationId)
    val artworkOwnershipOrgIds         = artworkOwnerships.values.flatMap(_.map(_.organizationId))
    val consignmentOrgIds              = consignments.values.map(_.consigneeOrganizationId)
    val invoiceOrgIds = invoices.values.flatMap(_.flatMap(i => Seq(i.invoiceData.issuingOrganizationId, i.invoiceData.payingOrganizationId)))

    val allOrganizationIds =
      sellerRepresentativeOrgIds ++
        sellerCommissionReceiverOrgIds ++
        sellerVoteOrgIds ++
        buyerRepresentativeOrgIds ++
        buyerCommissionReceiverOrgIds ++
        buyerOrgIds ++
        buyerVoteOrgIds ++
        artworkOrgIds ++
        artworkOwnershipOrgIds ++
        consignmentOrgIds ++
        invoiceOrgIds

    allOrganizationIds.distinct
  }

  private def resolveContacts(organizationId: OrganizationId, organizationIds: Seq[OrganizationId]): StepIO[Map[OrganizationId, ContactDto]] =
    contactConnectionResolveService.fromOrganizations(organizationId, organizationIds).seal

  private def getInvoiceAssetMap(invoices: Seq[Invoice]): StepIO[Map[AssetUrl, AssetInfoDto]] =
    assetsEnrichToAssetInfoService
      .enrichAll(invoices.flatMap(_.fileUrl).map(AssetUrl.apply))
      .seal

  private def getInvoicesStateChangeLogsMap(invoices: Seq[Invoice]): StepIO[Map[InvoiceId, Seq[InvoiceStateChangeLog]]] =
    transactor
      .execute(invoiceStateChangeLogRepository.list(InvoiceStateChangeLogFilters(invoiceIds = Some(invoices.map(_.id)))))
      .map(_.groupBy(_.invoiceId))
      .seal

  private def prepareArtworkDtos(
    organizationId: OrganizationId,
    artworkExts: Map[ArtworkId, ArtworkWithExploringOrganizationStatus],
    authorOrganizations: Map[OrganizationId, Organization],
    assetsMap: Map[AssetUrl, AssetInfoDto],
    ownershipMap: Map[ArtworkId, Seq[ArtworkOwnership]],
    contactMap: Map[OrganizationId, ContactDto]
  ): Map[ArtworkId, ArtworkDto] =
    artworkExts.map { case id -> artworkExt =>
      id -> ArtworkDto.from(
        Some(organizationId),
        artworkExt,
        artworkExt.artwork.organizations.authorOrganizationId.flatMap(authorOrganizations.get),
        assetsMap,
        ownershipMap.get(id).toSeq.flatten.map { ownership =>
          ArtworkOwnershipDto(ownership, contactMap.get(ownership.organizationId))
        }
      )
    }

  private def prepareTransactionDtos(
    transactions: Seq[Transaction],
    artworkExts: Map[ArtworkId, ArtworkWithExploringOrganizationStatus],
    sellerCommissionReceivers: Map[TransactionId, Seq[TransactionCommissionReceiver]],
    sellerVotes: Map[TransactionId, Seq[TransactionVote]],
    buyerCommissionReceivers: Map[TransactionId, Seq[TransactionCommissionReceiver]],
    buyers: Map[TransactionId, Seq[TransactionBuyer]],
    buyerVotes: Map[TransactionId, Seq[TransactionVote]],
    invoices: Map[TransactionId, Seq[Invoice]],
    invoiceAssetMap: Map[AssetUrl, AssetInfoDto],
    invoicesStateChangeLogsMap: Map[InvoiceId, Seq[InvoiceStateChangeLog]],
    contactMap: Map[OrganizationId, ContactDto]
  ): Seq[TransactionDto] = {
    val sellerCommissionReceiverDtos = sellerCommissionReceivers.map { case (transactionId, receivers) =>
      transactionId -> receivers.map(r => TransactionCommissionReceiverDto(r, contactMap.get(r.organizationId)))
    }
    val sellerVoteDtos = sellerVotes.map { case (transactionId, votes) =>
      transactionId -> votes.map(v => TransactionVoteDto(v, contactMap.get(v.organizationId)))
    }
    val buyerCommissionReceiverDtos = buyerCommissionReceivers.map { case (transactionId, receivers) =>
      transactionId -> receivers.map(r => TransactionCommissionReceiverDto(r, contactMap.get(r.organizationId)))
    }
    val buyerVoteDtos = buyerVotes.map { case (transactionId, votes) =>
      transactionId -> votes.map(v => TransactionVoteDto(v, contactMap.get(v.organizationId)))
    }
    val invoiceDtos = invoices.map { case (transactionId, invoices) =>
      transactionId -> invoices.map(invoice =>
        InvoiceDto(
          invoice,
          assetInfo = invoice.fileUrl.flatMap(fileUrl => invoiceAssetMap.get(AssetUrl(fileUrl))),
          contactMap.get(invoice.invoiceData.issuingOrganizationId),
          contactMap.get(invoice.invoiceData.payingOrganizationId),
          stateChangeLog = invoicesStateChangeLogsMap.getOrElse(invoice.id, Seq.empty)
        )
      )
    }

    transactions.map { t =>
      TransactionDto(
        transaction = t,
        artwork = artworkExts.get(t.artworkId).map(_.artwork),
        sellerRepresentative = contactMap.get(t.sellerInfo.representativeOrganizationId),
        sellerCommissionReceivers = sellerCommissionReceiverDtos.getOrElse(t.id, Seq.empty),
        sellerVotes = sellerVoteDtos.getOrElse(t.id, Seq.empty),
        buyerRepresentative = contactMap.get(t.buyerInfo.representativeOrganizationId),
        buyerCommissionReceivers = buyerCommissionReceiverDtos.getOrElse(t.id, Seq.empty),
        buyers = buyers.getOrElse(t.id, Seq.empty),
        buyersContactsMap = contactMap,
        buyerVotes = buyerVoteDtos.getOrElse(t.id, Seq.empty),
        invoices = invoiceDtos.getOrElse(t.id, Seq.empty)
      )
    }
  }

  private def prepareDto(
    organizationId: OrganizationId,
    transactions: Seq[TransactionDto],
    artworks: Map[ArtworkId, ArtworkDto],
    consignments: Map[ConsignmentId, Consignment],
    contactMap: Map[OrganizationId, ContactDto]
  ): StepIO[Seq[TransactionDtoList]] = {

    val consignmentDtos = consignments.map { case (consignmentId, consignment) =>
      consignmentId -> ConsignmentDto(consignment, contactMap.get(consignment.consigneeOrganizationId))
    }

    val res = transactions
      .map { transaction =>
        val artwork = transaction.artworkId.flatMap(artworks.get)

        transactionLimitVisibilityService
          .limit(transaction, isOwner = artwork.exists(_.isOwner), isAuthor = artwork.exists(_.authorOrganizationId.contains(organizationId)))
          .map(transactionDto =>
            TransactionDtoList(
              transaction = transactionDto,
              artwork = transactionDto.artworkId.flatMap(_ => artwork),
              consignment = transactionDto.consignmentId.flatMap(consignmentDtos.get)
            )
          )
      }
      .sequence
      .left
      .map(TransactionListResult.LimitVisibilityServiceError.apply)

    IO.pure(res).fromEither
  }
}

object TransactionListService {
  final case class TransactionListRequest(
    id: Option[TransactionId],
    artworkId: Option[ArtworkId],
    phrase: Option[String],
    statuses: Option[Set[TransactionStatus]],
    transactionSides: Option[Set[TransactionSide]],
    representativeContactConnectionIds: Option[Set[ContactConnectionId]],
    createdAfter: Option[TransactionCreatedAt],
    createdBefore: Option[TransactionCreatedAt],
    pageSizeAndNumber: Option[PageSizeAndNumber],
    sortBy: Option[Seq[TransactionSortBy]])

  final case class TransactionDtoList(
    transaction: TransactionDto,
    artwork: Option[ArtworkDto],
    consignment: Option[ConsignmentDto])

  sealed trait TransactionListResult
  object TransactionListResult {
    final case class Ok(data: PaginatedResult[TransactionDtoList]) extends TransactionListResult
    final case class LimitVisibilityServiceError(message: String)  extends TransactionListResult
    case object UserIsNotMemberOfOrganization                      extends TransactionListResult
  }
}

```

#### TransactionListServiceSpec.scala

```scala
package pl.iterators.artnetwork.transaction.services

import org.mockito.invocation.InvocationOnMock
import pl.iterators.artnetwork.artwork.domain.ArtworkDto.ArtworkOwnershipDto
import pl.iterators.artnetwork.artwork.domain.{ArtworkDto, ArtworkOwnership, ArtworkWithExploringOrganizationStatus}
import pl.iterators.artnetwork.artwork.repositories.ArtworkOwnershipRepository.ArtworkOwnershipFilters
import pl.iterators.artnetwork.artwork.repositories.ArtworkRepository.ArtworkFilters
import pl.iterators.artnetwork.artwork.repositories.{ArtworkOwnershipRepository, ArtworkRepository}
import pl.iterators.artnetwork.assets.domain.AssetDomain._
import pl.iterators.artnetwork.assets.domain.AssetInfoDto
import pl.iterators.artnetwork.assets.services.AssetsEnrichToAssetInfoService
import pl.iterators.artnetwork.consignment.domain.{Consignment, ConsignmentDto}
import pl.iterators.artnetwork.consignment.repositories.ConsignmentRepository
import pl.iterators.artnetwork.consignment.repositories.ConsignmentRepository.ConsignmentFilters
import pl.iterators.artnetwork.contacts.domain.{ContactConnection, ContactConnectionOrganization, ContactDto}
import pl.iterators.artnetwork.contacts.services.ContactConnectionResolveService
import pl.iterators.artnetwork.invoice.domain.InvoiceDomain.{InvoiceExternalId, InvoiceId}
import pl.iterators.artnetwork.invoice.domain.{Invoice, InvoiceDto, InvoiceStateChangeLog}
import pl.iterators.artnetwork.invoice.repositories.InvoiceRepository.InvoiceFilters
import pl.iterators.artnetwork.invoice.repositories.InvoiceStateChangeLogRepository.InvoiceStateChangeLogFilters
import pl.iterators.artnetwork.invoice.repositories.{InvoiceRepository, InvoiceStateChangeLogRepository}
import pl.iterators.artnetwork.organization.domain.Organization
import pl.iterators.artnetwork.organization.domain.OrganizationDomain.OrganizationId
import pl.iterators.artnetwork.organization.repositories.OrganizationRepository
import pl.iterators.artnetwork.organization.repositories.OrganizationRepository.OrganizationFilters
import pl.iterators.artnetwork.organization.services.OrganizationUserMembershipService
import pl.iterators.artnetwork.transaction.domain.TransactionDomain.TransactionSide
import pl.iterators.artnetwork.transaction.domain._
import pl.iterators.artnetwork.transaction.repositories.TransactionBuyerCommissionReceiverRepository.TransactionBuyerCommissionReceiverFilters
import pl.iterators.artnetwork.transaction.repositories.TransactionBuyerRepository.TransactionBuyerFilters
import pl.iterators.artnetwork.transaction.repositories.TransactionBuyerVoteRepository.TransactionBuyerVoteFilters
import pl.iterators.artnetwork.transaction.repositories.TransactionRepository._
import pl.iterators.artnetwork.transaction.repositories.TransactionSellerCommissionReceiverRepository.TransactionSellerCommissionReceiverFilters
import pl.iterators.artnetwork.transaction.repositories.TransactionSellerVoteRepository.TransactionSellerVoteFilters
import pl.iterators.artnetwork.transaction.repositories._
import pl.iterators.artnetwork.transaction.services.TransactionListService._
import pl.iterators.artnetwork.utils.ServiceSpec
import pl.iterators.artnetwork.utils.db.PaginatedResult
import pl.iterators.kebs.scalacheck.AllGenerators

import java.time.Instant
import scala.util.Random

class TransactionListServiceSpec extends ServiceSpec {
  lazy val requestGenerator: AllGenerators[TransactionListRequest]                    = allGenerators[TransactionListRequest]
  lazy val transactionGenerator: AllGenerators[Transaction]                           = allGenerators[Transaction]
  lazy val commissionReceiverGenerator: AllGenerators[TransactionCommissionReceiver]  = allGenerators[TransactionCommissionReceiver]
  lazy val buyerGenerator: AllGenerators[TransactionBuyer]                            = allGenerators[TransactionBuyer]
  lazy val voteGenerator: AllGenerators[TransactionVote]                              = allGenerators[TransactionVote]
  lazy val consignmentGenerator: AllGenerators[Consignment]                           = allGenerators[Consignment]
  lazy val artworkExtGenerator: AllGenerators[ArtworkWithExploringOrganizationStatus] = allGenerators[ArtworkWithExploringOrganizationStatus]
  lazy val artworkOwnershipGenerator: AllGenerators[ArtworkOwnership]                 = allGenerators[ArtworkOwnership]
  lazy val contactConnectionGenerator: AllGenerators[ContactConnection]               = allGenerators[ContactConnection]
  lazy val organizationGenerator: AllGenerators[Organization]                         = allGenerators[Organization]
  lazy val invoiceGenerator: AllGenerators[Invoice]                                   = allGenerators[Invoice]
  lazy val assetInfoDtoGenerator: AllGenerators[AssetInfoDto]                         = allGenerators[AssetInfoDto]
  lazy val invoiceStateChangeLogGenerator: AllGenerators[InvoiceStateChangeLog]       = allGenerators[InvoiceStateChangeLog]

  trait TestCase extends BaseScope {
    val artworkRepository: ArtworkRepository                   = vmock[ArtworkRepository]
    val artworkOwnershipRepository: ArtworkOwnershipRepository = vmock[ArtworkOwnershipRepository]
    val consignmentRepository: ConsignmentRepository           = vmock[ConsignmentRepository]
    val organizationRepository: OrganizationRepository         = vmock[OrganizationRepository]
    val transactionRepository: TransactionRepository           = vmock[TransactionRepository]
    val transactionSellerCommissionReceiverRepository: TransactionSellerCommissionReceiverRepository =
      vmock[TransactionSellerCommissionReceiverRepository]
    val transactionBuyerCommissionReceiverRepository: TransactionBuyerCommissionReceiverRepository =
      vmock[TransactionBuyerCommissionReceiverRepository]
    val transactionBuyerRepository: TransactionBuyerRepository               = vmock[TransactionBuyerRepository]
    val transactionSellerVoteRepository: TransactionSellerVoteRepository     = vmock[TransactionSellerVoteRepository]
    val transactionBuyerVoteRepository: TransactionBuyerVoteRepository       = vmock[TransactionBuyerVoteRepository]
    val invoiceRepository: InvoiceRepository                                 = vmock[InvoiceRepository]
    val invoiceStateChangeLogRepository: InvoiceStateChangeLogRepository     = vmock[InvoiceStateChangeLogRepository]
    val transactionLimitVisibilityService: TransactionLimitVisibilityService = vmock[TransactionLimitVisibilityService]
    val organizationUserMembershipService: OrganizationUserMembershipService = vmock[OrganizationUserMembershipService]
    val contactConnectionResolveService: ContactConnectionResolveService     = vmock[ContactConnectionResolveService]
    val assetsEnrichToAssetInfoService: AssetsEnrichToAssetInfoService       = vmock[AssetsEnrichToAssetInfoService]

    val service: TransactionListService = wire[TransactionListService]

    // data
    val organization: Organization     = organizationGenerator.maximal.generate
    val organizationId: OrganizationId = organization.id

    val minimalRequest: TransactionListRequest = TransactionListRequest(
      id = None,
      artworkId = None,
      phrase = None,
      statuses = None,
      transactionSides = None,
      representativeContactConnectionIds = None,
      createdAfter = None,
      createdBefore = None,
      pageSizeAndNumber = None,
      sortBy = None
    )

    val representativeOrganization: Organization = organizationGenerator.maximal.generate
    val representativeContactConnection: ContactConnection =
      contactConnectionGenerator.maximal.generate.copy(ownerOrganizationId = organizationId, contactOrganizationId = representativeOrganization.id)
    val representativeContactDto: ContactDto = ContactDto(representativeContactConnection, representativeOrganization)

    // transaction 1 has consignment
    // transaction 2 has consignment, but is not visible to user
    // transactions 3 and 4 have no consignment

    // user is artwork owner of transaction 3
    // user is artwork author of transaction 4

    val transaction1: Transaction = transactionGenerator.maximal.generate
    val artworkExt1: ArtworkWithExploringOrganizationStatus = {
      val gen: ArtworkWithExploringOrganizationStatus = artworkExtGenerator.maximal.generate
      gen.copy(artwork = gen.artwork.copy(id = transaction1.artworkId))
    }
    val artwork1AuthorOrganization: Organization =
      organizationGenerator.maximal.generate.copy(id = artworkExt1.artwork.organizations.authorOrganizationId.get)
    val artworkOwnership1: ArtworkOwnership = artworkOwnershipGenerator.maximal.generate.copy(artworkId = artworkExt1.artwork.id)
    val consignment1: Consignment           = consignmentGenerator.maximal.generate.copy(id = transaction1.consignmentId.get)
    val sellerCommissionReceivers1: Seq[TransactionCommissionReceiver] =
      Seq.fill(3)(commissionReceiverGenerator.maximal.generate.copy(transactionId = transaction1.id))
    val buyerCommissionReceivers1: Seq[TransactionCommissionReceiver] =
      Seq.fill(3)(commissionReceiverGenerator.maximal.generate.copy(transactionId = transaction1.id))
    val buyers1: Seq[TransactionBuyer]     = Seq.fill(3)(buyerGenerator.maximal.generate.copy(transactionId = transaction1.id))
    val sellerVotes1: Seq[TransactionVote] = Seq.fill(3)(voteGenerator.maximal.generate.copy(transactionId = transaction1.id))
    val buyerVotes1: Seq[TransactionVote]  = Seq.fill(3)(voteGenerator.maximal.generate.copy(transactionId = transaction1.id))

    val transaction2: Transaction = transactionGenerator.maximal.generate
    val artworkExt2: ArtworkWithExploringOrganizationStatus = {
      val gen: ArtworkWithExploringOrganizationStatus = artworkExtGenerator.maximal.generate
      gen.copy(artwork = gen.artwork.copy(id = transaction2.artworkId))
    }
    val artwork2AuthorOrganization: Organization =
      organizationGenerator.maximal.generate.copy(id = artworkExt2.artwork.organizations.authorOrganizationId.get)
    val artworkOwnership2: ArtworkOwnership = artworkOwnershipGenerator.maximal.generate.copy(artworkId = artworkExt2.artwork.id)
    val consignment2: Consignment           = consignmentGenerator.maximal.generate.copy(id = transaction2.consignmentId.get)
    val sellerCommissionReceivers2: Seq[TransactionCommissionReceiver] =
      Seq.fill(3)(commissionReceiverGenerator.maximal.generate.copy(transactionId = transaction2.id))
    val buyerCommissionReceivers2: Seq[TransactionCommissionReceiver] =
      Seq.fill(3)(commissionReceiverGenerator.maximal.generate.copy(transactionId = transaction2.id))
    val buyers2: Seq[TransactionBuyer]     = Seq.fill(3)(buyerGenerator.maximal.generate.copy(transactionId = transaction2.id))
    val sellerVotes2: Seq[TransactionVote] = Seq.fill(3)(voteGenerator.maximal.generate.copy(transactionId = transaction2.id))
    val buyerVotes2: Seq[TransactionVote]  = Seq.fill(3)(voteGenerator.maximal.generate.copy(transactionId = transaction2.id))
    val invoices: Seq[Invoice]             = Seq.fill(2)(invoiceGenerator.maximal.generate.copy(externalId = InvoiceExternalId(transaction2.id)))

    val transaction3: Transaction = transactionGenerator.maximal.generate.copy(consignmentId = None)
    val artworkExt3: ArtworkWithExploringOrganizationStatus = {
      val gen: ArtworkWithExploringOrganizationStatus = artworkExtGenerator.maximal.generate
      gen.copy(artwork = gen.artwork.copy(id = transaction3.artworkId))
    }
    val artwork3AuthorOrganization: Organization =
      organizationGenerator.maximal.generate.copy(id = artworkExt3.artwork.organizations.authorOrganizationId.get)
    val artworkOwnership3: ArtworkOwnership =
      artworkOwnershipGenerator.maximal.generate.copy(artworkId = artworkExt3.artwork.id, organizationId = organizationId)

    val transaction4: Transaction = transactionGenerator.maximal.generate.copy(consignmentId = None)
    val artworkExt4: ArtworkWithExploringOrganizationStatus = {
      val gen: ArtworkWithExploringOrganizationStatus = artworkExtGenerator.maximal.generate
      gen.copy(artwork =
        gen.artwork.copy(id = transaction4.artworkId, organizations = gen.artwork.organizations.copy(authorOrganizationId = Some(organizationId)))
      )
    }
    val artworkOwnership4: ArtworkOwnership = artworkOwnershipGenerator.maximal.generate.copy(artworkId = artworkExt4.artwork.id)

    val assets: Seq[AssetUrl] = Seq(artworkExt1, artworkExt2, artworkExt3, artworkExt4).flatMap(_.artwork.assets.all)

    val assetMap: Map[AssetUrl, AssetInfoDto] = assets
      .map(u =>
        AssetUrl(u) -> AssetInfoDto(
          key = AssetKey(Random.nextString(32)),
          presignedUrl = Some(AssetPresignedUrl(Random.nextString(32))),
          name = None,
          lastModified = AssetLastModified(Instant.now()),
          size = AssetSize(0L)
        )
      )
      .toMap

    val associatedOrgIds: Seq[OrganizationId] = {
      val all = Seq(
        transaction1.sellerInfo.representativeOrganizationId,
        transaction2.sellerInfo.representativeOrganizationId,
        transaction3.sellerInfo.representativeOrganizationId,
        transaction4.sellerInfo.representativeOrganizationId,
        transaction1.buyerInfo.representativeOrganizationId,
        transaction2.buyerInfo.representativeOrganizationId,
        transaction3.buyerInfo.representativeOrganizationId,
        transaction4.buyerInfo.representativeOrganizationId,
        artworkExt1.artwork.organizations.authorOrganizationId.get,
        artworkExt2.artwork.organizations.authorOrganizationId.get,
        artworkExt3.artwork.organizations.authorOrganizationId.get,
        artworkExt4.artwork.organizations.authorOrganizationId.get,
        artworkOwnership1.organizationId,
        artworkOwnership2.organizationId,
        artworkOwnership3.organizationId,
        artworkOwnership4.organizationId,
        consignment1.consigneeOrganizationId,
        consignment2.consigneeOrganizationId
      ) ++
        sellerCommissionReceivers1.map(_.organizationId) ++
        sellerCommissionReceivers2.map(_.organizationId) ++
        buyerCommissionReceivers1.map(_.organizationId) ++
        buyerCommissionReceivers2.map(_.organizationId) ++
        buyers1.map(_.organizationId) ++
        buyers2.map(_.organizationId) ++
        sellerVotes1.map(_.organizationId) ++
        sellerVotes2.map(_.organizationId) ++
        buyerVotes1.map(_.organizationId) ++
        buyerVotes2.map(_.organizationId) ++
        invoices.map(_.invoiceData.issuingOrganizationId) ++
        invoices.map(_.invoiceData.payingOrganizationId)
      all.distinct
    }

    val fullContactMap: Map[OrganizationId, ContactDto] = associatedOrgIds
      .filterNot(_ == organizationId)
      .map { id =>
        val organization      = organizationGenerator.maximal.generate.copy(id = id)
        val contactConnection = contactConnectionGenerator.maximal.generate.copy(ownerOrganizationId = organizationId, contactOrganizationId = id)
        id -> ContactDto(contactConnection, organization)
      }
      .toMap

    val contactMap: Map[OrganizationId, ContactDto] = fullContactMap
      .drop(3) + // drop 3 elements to simulate missing contacts
      (organizationId -> ContactDto.self(organization))

    val invoiceAssetMap: Map[AssetUrl, AssetInfoDto] =
      invoices.flatMap(_.fileUrl).map(AssetUrl.apply).map(assetUrl => assetUrl -> assetInfoDtoGenerator.normal.generate).toMap

    val invoiceStateChangeLogMap: Map[InvoiceId, Seq[InvoiceStateChangeLog]] = invoices.map { invoice =>
      invoice.id -> Seq(
        invoiceStateChangeLogGenerator.normal.generate.copy(invoiceId = invoice.id),
        invoiceStateChangeLogGenerator.normal.generate.copy(invoiceId = invoice.id)
      )
    }.toMap
  }

  "list" should {
    "return UserIsNotMemberOfOrganization if user is not member of organization" in new TestCase {
      when(organizationUserMembershipService.isMember(organizationId, authUser.id)).thenReturn(false.asIO)

      service
        .list(authUser, organizationId, minimalRequest)
        .asserting(_ shouldEqual TransactionListResult.UserIsNotMemberOfOrganization)

      verify(organizationUserMembershipService).isMember(organizationId, authUser.id)
    }

    "return LimitVisibilityServiceError with error message if limit visibility service returns error" in new TestCase {
      when(organizationUserMembershipService.isMember(organizationId, authUser.id)).thenReturn(true.asIO)
      when(transactionRepository.list(any)).thenReturn(Seq(transaction1, transaction2).asDBIO)
      when(transactionRepository.count(any)).thenReturn(2.asDBIO)
      when(transactionSellerCommissionReceiverRepository.list(any)).thenReturn(Seq.empty.asDBIO)
      when(transactionBuyerCommissionReceiverRepository.list(any)).thenReturn(Seq.empty.asDBIO)
      when(transactionBuyerRepository.list(any)).thenReturn(Seq.empty.asDBIO)
      when(transactionSellerVoteRepository.list(any)).thenReturn(Seq.empty.asDBIO)
      when(transactionBuyerVoteRepository.list(any)).thenReturn(Seq.empty.asDBIO)
      when(invoiceRepository.list(any)).thenReturn(Seq.empty.asDBIO)
      when(invoiceStateChangeLogRepository.list(any)).thenReturn(Seq.empty.asDBIO)
      when(artworkRepository.list(any)).thenReturn(Seq.empty.asDBIO)
      when(organizationRepository.list(any)).thenReturn(Seq.empty.asDBIO)
      when(assetsEnrichToAssetInfoService.enrichAll(any)).thenReturn(Map.empty[AssetUrl, AssetInfoDto].asIO, Map.empty[AssetUrl, AssetInfoDto].asIO)
      when(artworkOwnershipRepository.list(any)).thenReturn(Seq.empty.asDBIO)
      when(consignmentRepository.list(any)).thenReturn(Seq.empty.asDBIO)
      when(contactConnectionResolveService.fromOrganizations(any, any)).thenReturn(Map.empty[OrganizationId, ContactDto].asIO)
      when(transactionLimitVisibilityService.limit(any, any, any)).thenAnswer { (invocation: InvocationOnMock) =>
        val in = invocation.getArguments()(0).asInstanceOf[TransactionDto]
        in.id match {
          case transaction1.id => Right(in)
          case transaction2.id => Left("error")
        }
      }

      service
        .list(authUser, organizationId, minimalRequest)
        .asserting(_ shouldEqual TransactionListResult.LimitVisibilityServiceError("error"))

      verify(organizationUserMembershipService).isMember(any, any)
      verify(transactionRepository).list(any)
      verify(transactionRepository).count(any)
      verify(transactionSellerCommissionReceiverRepository).list(any)
      verify(transactionBuyerCommissionReceiverRepository).list(any)
      verify(transactionBuyerRepository).list(any)
      verify(transactionSellerVoteRepository).list(any)
      verify(transactionBuyerVoteRepository).list(any)
      verify(artworkRepository).list(any)
      verify(organizationRepository).list(any)
      verify(assetsEnrichToAssetInfoService, times(2)).enrichAll(any)
      verify(artworkOwnershipRepository).list(any)
      verify(consignmentRepository).list(any)
      verify(invoiceRepository).list(any)
      verify(invoiceStateChangeLogRepository).list(any)
      verify(contactConnectionResolveService).fromOrganizations(any, any)
      verify(transactionLimitVisibilityService, times(2)).limit(any, any, any)
    }

    "return Ok with data otherwise, with correct data passed to limit visibility service" in new TestCase {
      private val request        = requestGenerator.normal.generate
      private val transactionIds = Seq(transaction1.id, transaction2.id, transaction3.id, transaction4.id)

      private val transactionDto1 = TransactionDto(
        transaction = transaction1,
        artwork = Some(artworkExt1.artwork),
        sellerRepresentative = contactMap.get(transaction1.sellerInfo.representativeOrganizationId),
        sellerCommissionReceivers = sellerCommissionReceivers1.map(r => TransactionCommissionReceiverDto(r, contactMap.get(r.organizationId))),
        sellerVotes = sellerVotes1.map(v => TransactionVoteDto(v, contactMap.get(v.organizationId))),
        buyerRepresentative = contactMap.get(transaction1.buyerInfo.representativeOrganizationId),
        buyerCommissionReceivers = buyerCommissionReceivers1.map(r => TransactionCommissionReceiverDto(r, contactMap.get(r.organizationId))),
        buyers = buyers1,
        buyersContactsMap = contactMap,
        buyerVotes = buyerVotes1.map(v => TransactionVoteDto(v, contactMap.get(v.organizationId)))
      )

      private val artworkDto1 = ArtworkDto.from(
        Some(organizationId),
        artworkExt = artworkExt1,
        author = Some(artwork1AuthorOrganization),
        assetsMap = assetMap,
        owners = Seq(ArtworkOwnershipDto(artworkOwnership1, contactMap.get(artworkOwnership1.organizationId)))
      )

      private val consignmentDto1 = ConsignmentDto(consignment1, contactMap.get(consignment1.consigneeOrganizationId))

      private val transactionDtoList1 =
        TransactionDtoList(transaction = transactionDto1, artwork = Some(artworkDto1), consignment = Some(consignmentDto1))

      private val transactionDto2 = TransactionDto(
        transaction = transaction2,
        artwork = Some(artworkExt2.artwork),
        sellerRepresentative = contactMap.get(transaction2.sellerInfo.representativeOrganizationId),
        sellerCommissionReceivers = sellerCommissionReceivers2.map(r => TransactionCommissionReceiverDto(r, contactMap.get(r.organizationId))),
        sellerVotes = sellerVotes2.map(v => TransactionVoteDto(v, contactMap.get(v.organizationId))),
        buyerRepresentative = contactMap.get(transaction2.buyerInfo.representativeOrganizationId),
        buyerCommissionReceivers = buyerCommissionReceivers2.map(r => TransactionCommissionReceiverDto(r, contactMap.get(r.organizationId))),
        buyers = buyers2,
        buyersContactsMap = contactMap,
        buyerVotes = buyerVotes2.map(v => TransactionVoteDto(v, contactMap.get(v.organizationId))),
        invoices = invoices.map(invoice =>
          InvoiceDto(
            invoice,
            assetInfo = invoice.fileUrl.map(AssetUrl.apply).flatMap(invoiceAssetMap.get),
            contactMap.get(invoice.invoiceData.issuingOrganizationId),
            contactMap.get(invoice.invoiceData.payingOrganizationId),
            stateChangeLog = invoiceStateChangeLogMap.getOrElse(invoice.id, Seq.empty)
          )
        )
      )

      private val transactionDtoList2 =
        TransactionDtoList(transaction = transactionDto2.copy(consignmentId = None, artworkId = None), artwork = None, consignment = None)

      private val transactionDto3 = TransactionDto(
        transaction = transaction3,
        artwork = Some(artworkExt3.artwork),
        sellerRepresentative = contactMap.get(transaction3.sellerInfo.representativeOrganizationId),
        sellerCommissionReceivers = Nil,
        sellerVotes = Nil,
        buyerRepresentative = contactMap.get(transaction3.buyerInfo.representativeOrganizationId),
        buyerCommissionReceivers = Nil,
        buyers = Nil,
        buyerVotes = Nil
      )

      private val transactionDtoList3 = TransactionDtoList(transaction = transactionDto3.copy(artworkId = None), artwork = None, consignment = None)

      private val transactionDto4 = TransactionDto(
        transaction = transaction4,
        artwork = Some(artworkExt4.artwork),
        sellerRepresentative = contactMap.get(transaction4.sellerInfo.representativeOrganizationId),
        sellerCommissionReceivers = Nil,
        sellerVotes = Nil,
        buyerRepresentative = contactMap.get(transaction4.buyerInfo.representativeOrganizationId),
        buyerCommissionReceivers = Nil,
        buyers = Nil,
        buyerVotes = Nil
      )

      private val transactionDtoList4 = TransactionDtoList(transaction = transactionDto4.copy(artworkId = None), artwork = None, consignment = None)

      private val transactionFilters: TransactionFilters =
        TransactionFilters(
          organizationId = Some(organizationId),
          id = request.id,
          artworkId = request.artworkId,
          phrase = request.phrase.map(phrase => TransactionPhraseFilter(phrase, organizationId)),
          statuses = request.statuses,
          saleOrganizationId = Option.when(request.transactionSides.contains(Set(TransactionSide.SellerSide)))(organizationId),
          purchaseOrganizationId = Option.when(request.transactionSides.contains(Set(TransactionSide.BuyerSide)))(organizationId),
          representativeOrganization = Option.when(request.representativeContactConnectionIds.nonEmpty) {
            TransactionRepresentativeOrganizationFilter(Set(representativeOrganization.id), organizationId)
          },
          createdAfter = request.createdAfter,
          createdBefore = request.createdBefore,
          sortBy = request.sortBy.toSeq.flatten,
          pageSizeAndNumber = request.pageSizeAndNumber
        )

      when(organizationUserMembershipService.isMember(organizationId, authUser.id)).thenReturn(true.asIO)
      if (request.representativeContactConnectionIds.isDefined) {
        when(contactConnectionResolveService.fromConnections(organizationId, request.representativeContactConnectionIds.get.toSeq))
          .thenReturn(
            Map(representativeContactConnection.id -> ContactConnectionOrganization(representativeContactConnection, representativeOrganization)).asIO
          )
      }
      when(transactionRepository.list(any)).thenReturn(Seq(transaction1, transaction2, transaction3, transaction4).asDBIO)
      when(transactionRepository.count(any)).thenReturn(4.asDBIO)
      when(transactionSellerCommissionReceiverRepository.list(any)).thenReturn((sellerCommissionReceivers1 ++ sellerCommissionReceivers2).asDBIO)
      when(transactionBuyerCommissionReceiverRepository.list(any)).thenReturn((buyerCommissionReceivers1 ++ buyerCommissionReceivers2).asDBIO)
      when(transactionBuyerRepository.list(any)).thenReturn((buyers1 ++ buyers2).asDBIO)
      when(transactionSellerVoteRepository.list(any)).thenReturn((sellerVotes1 ++ sellerVotes2).asDBIO)
      when(transactionBuyerVoteRepository.list(any)).thenReturn((buyerVotes1 ++ buyerVotes2).asDBIO)
      when(invoiceRepository.list(any)).thenReturn(invoices.asDBIO)
      when(invoiceStateChangeLogRepository.list(any)).thenReturn(invoiceStateChangeLogMap.values.toSeq.flatten.asDBIO)
      when(artworkRepository.list(any)).thenReturn(Seq(artworkExt1, artworkExt2, artworkExt3, artworkExt4).asDBIO)
      when(organizationRepository.list(any))
        .thenReturn(Seq(artwork1AuthorOrganization, artwork2AuthorOrganization, artwork3AuthorOrganization, organization).asDBIO)
      when(assetsEnrichToAssetInfoService.enrichAll(any)).thenReturn(assetMap.asIO, invoiceAssetMap.asIO)
      when(artworkOwnershipRepository.list(any)).thenReturn(Seq(artworkOwnership1, artworkOwnership2, artworkOwnership3, artworkOwnership4).asDBIO)
      when(consignmentRepository.list(any)).thenReturn(Seq(consignment1, consignment2).asDBIO)
      when(contactConnectionResolveService.fromOrganizations(any, any)).thenReturn(contactMap.asIO)
      when(transactionLimitVisibilityService.limit(any, any, any)).thenAnswer { (invocation: InvocationOnMock) =>
        val in = invocation.getArguments()(0).asInstanceOf[TransactionDto]
        in.id match {
          case transaction1.id => Right(in)
          case transaction2.id => Right(in.copy(consignmentId = None, artworkId = None))
          case transaction3.id => Right(in.copy(artworkId = None))
          case transaction4.id => Right(in.copy(artworkId = None))
        }
      }

      service
        .list(authUser, organizationId, request)
        .asserting(
          _ shouldEqual TransactionListResult
            .Ok(PaginatedResult(4, Seq(transactionDtoList1, transactionDtoList2, transactionDtoList3, transactionDtoList4)))
        )

      verify(organizationUserMembershipService).isMember(organizationId, authUser.id)
      if (request.representativeContactConnectionIds.isDefined) {
        verify(contactConnectionResolveService).fromConnections(organizationId, request.representativeContactConnectionIds.get.toSeq)
      }
      verify(transactionRepository).list(transactionFilters)
      verify(transactionRepository).count(transactionFilters)
      verify(transactionSellerCommissionReceiverRepository).list(TransactionSellerCommissionReceiverFilters(transactionIds = Some(transactionIds)))
      verify(transactionBuyerCommissionReceiverRepository).list(TransactionBuyerCommissionReceiverFilters(transactionIds = Some(transactionIds)))
      verify(transactionBuyerRepository).list(TransactionBuyerFilters(transactionIds = Some(transactionIds)))
      verify(transactionSellerVoteRepository).list(TransactionSellerVoteFilters(transactionIds = Some(transactionIds)))
      verify(transactionBuyerVoteRepository).list(TransactionBuyerVoteFilters(transactionIds = Some(transactionIds)))
      verify(artworkRepository).list(
        ArtworkFilters(ids = Some(Seq(artworkExt1.artwork.id, artworkExt2.artwork.id, artworkExt3.artwork.id, artworkExt4.artwork.id)))
      )
      verify(organizationRepository).list(
        OrganizationFilters(ids =
          Some(Set(artwork1AuthorOrganization.id, artwork2AuthorOrganization.id, artwork3AuthorOrganization.id, organizationId))
        )
      )
      verify(assetsEnrichToAssetInfoService).enrichAll(assets)
      verify(artworkOwnershipRepository).list(
        ArtworkOwnershipFilters(artworkIds =
          Some(Seq(artworkExt1.artwork.id, artworkExt2.artwork.id, artworkExt3.artwork.id, artworkExt4.artwork.id))
        )
      )
      verify(consignmentRepository).list(ConsignmentFilters(ids = Some(Seq(consignment1.id, consignment2.id))))
      verify(invoiceRepository).list(
        InvoiceFilters(
          externalIds = Some(
            Seq(
              InvoiceExternalId(transaction1.id),
              InvoiceExternalId(transaction2.id),
              InvoiceExternalId(transaction3.id),
              InvoiceExternalId(transaction4.id)
            )
          ),
          organizationId = Some(organizationId)
        )
      )
      verify(invoiceStateChangeLogRepository).list(InvoiceStateChangeLogFilters(invoiceIds = Some(invoices.map(_.id))))
      verify(contactConnectionResolveService).fromOrganizations(
        same(organizationId),
        argThat { (ids: Seq[OrganizationId]) =>
          ids.toSet == associatedOrgIds.toSet
        }
      )
      verify(transactionLimitVisibilityService).limit(argThat((t: TransactionDto) => t.id == transaction1.id), same(false), same(false))
      verify(transactionLimitVisibilityService).limit(argThat((t: TransactionDto) => t.id == transaction2.id), same(false), same(false))
      verify(transactionLimitVisibilityService).limit(argThat((t: TransactionDto) => t.id == transaction3.id), same(true), same(false))
      verify(transactionLimitVisibilityService).limit(argThat((t: TransactionDto) => t.id == transaction4.id), same(false), same(true))
      verify(assetsEnrichToAssetInfoService).enrichAll(invoices.flatMap(_.fileUrl).map(AssetUrl.apply))
    }
  }
}

```

### Examples from: /Users/jglodek/dev-local-only/iteratorshq-sekcje/tasks/08-sealed-monad/input/sealed-monad-master/examples/src/main/scala/pl/iterators/sealedmonad/examples

#### Options.scala

```scala
package pl.iterators.sealedmonad.examples

import cats.Monad
import cats.data.{EitherT, OptionT}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import pl.iterators.sealedmonad.Sealed

import scala.language.higherKinds

object Options {

  object Example1 {
    import pl.iterators.sealedmonad.syntax.*

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
    import pl.iterators.sealedmonad.syntax.*

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
        user <- findUser(method.userId)
          .valueOr[ConfirmResponse](ConfirmResponse.UserNotFound)
          .flatTap(_ => upsertAuthMethod(confirmMethod(method)))
      } yield ConfirmResponse.Confirmed(issueTokenFor(user))

      s.run
    }

    class Example3[M[_]: Monad] {
      import pl.iterators.sealedmonad.syntax.*

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

```

