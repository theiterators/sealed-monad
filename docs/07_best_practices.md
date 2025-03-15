# Sealed Monad Best Practices

This document gathers best practices for leveraging the Sealed Monad pattern in your Scala applications. It includes concrete examples extracted from real-world code, guidelines for error modeling and side effect management, and recommendations for writing testable and composable logic. By following these guidelines, you’ll create domain‐rich, type‐safe code that communicates intent clearly and minimizes runtime surprises.

---

## 1. Core Design Patterns

### 1.1 Result Type Pattern

**Best Practice:** Define a sealed Result type for each operation so that all possible outcomes are represented explicitly.

**Example:** In the login process below, different outcomes such as successful login, invalid credentials, or account deletion are encoded as variants of a sealed trait.

```scala
sealed trait LoginResponse
object LoginResponse {
  final case class LoggedIn(token: String) extends LoginResponse
  case object InvalidCredentials extends LoginResponse
  case object Deleted extends LoginResponse
}

// A sample login function using traditional and sealed monad styles:
object LoginExamples {
  import cats.Monad
  import cats.data.OptionT
  import cats.syntax.flatMap._
  import cats.syntax.functor._
  import sealedmonad.syntax._

  // Traditional handling with for-comprehension:
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
      case None => M.pure(LoginResponse.InvalidCredentials)
      case Some(user) if user.archived =>
        M.pure(LoginResponse.Deleted)
      case Some(user) =>
        val authMethod = authMethodFromUserIdF(user.id)
        val actionT = OptionT(findAuthMethod(user.id, authMethod.provider))
          .map(checkAuthMethodAction)
        actionT.value flatMap {
          case Some(true)  => M.pure(LoginResponse.LoggedIn(issueTokenFor(user)))
          case Some(false) => M.pure(LoginResponse.InvalidCredentials)
          case None        => mergeAccountsAction(authMethod, user)
        }
    }

  // Sealed Monad style for early returns and validations:
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
    } yield if (checkAuthMethodAction(authMethod))
              LoginResponse.LoggedIn(issueTokenFor(user))
            else LoginResponse.InvalidCredentials

    s.run
  }
}

// Domain traits used in the example:
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
```

**Benefits:**
- Explicitly document all expected outcomes.
- Leverage the type system to enforce handling of each case.
- Enhance code readability and self-documentation.

---

### 1.2 Comprehensive Result Types

**Best Practice:** Model operations with comprehensive result types that capture both success and failure scenarios with context.

**Example:** For email confirmation, a custom result type includes both error cases and the success response.

```scala
sealed trait ConfirmResponse
object ConfirmResponse {
  case object MethodNotFound extends ConfirmResponse
  case object UserNotFound extends ConfirmResponse
  final case class Confirmed(token: String) extends ConfirmResponse
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
    user   <- findUser(method.userId)
                .valueOr[ConfirmResponse](ConfirmResponse.UserNotFound)
                .flatTap(_ => upsertAuthMethod(confirmMethod(method)))
  } yield ConfirmResponse.Confirmed(issueTokenFor(user))
  s.run
}
```

---

### 1.3 Monadic Composition

**Best Practice:** Chain dependent operations using flatMap, map, and for-comprehensions for clarity and robust error propagation.

**Example:** In a permission checking service, multiple operations (membership check, fetching permissions, and final resolution) are elegantly composed:

```scala
def resolvePermission[M[_]: Monad](
    authContext: AuthContext,
    organizationId: OrganizationId,
    action: PermissionPolicyAction,
    membershipService: (OrganizationId, Long) => M[OrganizationUserMembershipResult],
    permissionListService: (AuthContext, OrganizationId) => M[OrganizationUserPermissionListResult]
): M[AccessControlResult] = {
  val s = for {
    _ <- membershipService(organizationId, authContext.id)
           .seal
           .attempt {
             case OrganizationUserMembershipResult.Admin  => Left(AccessControlResult.Allowed)
             case OrganizationUserMembershipResult.Member => Right(())
             case OrganizationUserMembershipResult.None   => Left(AccessControlResult.NotAllowed)
           }
    perms <- permissionListService(authContext, organizationId)
              .seal
              .attempt {
                case OrganizationUserPermissionListResult.Ok(permissions) => Right(permissions)
                case OrganizationUserPermissionListResult.OperationNotPermitted =>
                  Left(AccessControlResult.NotAllowed)
              }
    allowedResources = perms.filter(p => p.actions.contains(action) && p.effect == PermissionPolicyEffect.Allow).flatMap(_.resources)
    deniedResources  = perms.filter(p => p.actions.contains(action) && p.effect == PermissionPolicyEffect.Deny).flatMap(_.resources)
  } yield {
    if (allowedResources.contains(AccessControlService.resourceAll) && deniedResources.isEmpty)
      AccessControlResult.Allowed
    else
      AccessControlResult.Restricted(allowedResources.contains(AccessControlService.resourceAll), allowedResources, deniedResources)
  }
  s.run
}
```

---

## 2. Error Handling Best Practices

### 2.1 Explicit Error Modeling

**Best Practice:** Integrate errors into your return types rather than using exceptions.

**Benefits:**
- Errors become part of the function’s signature.
- Callers are forced to handle failures explicitly.
- Enhances predictability and maintainability.

**Example:** In the login-by-crypto verification service, errors are propagated in a sealed monad pipeline:

```scala
def findUserWithIdentities(request: LoginByCryptoVerificationRequest): Step[UserWithIdentities] =
  userWithIdentitiesFindService.find(request.userId).attemptF {
    case UserWithIdentitiesFindResult.Ok(user)      => Sync[IO].pure(Right(user))
    case UserWithIdentitiesFindResult.UserDoNotExist  =>
      Logger[IO].warning(s"User ${request.userId} does not exist") *>
      Sync[IO].pure(Left(LoginByCryptoVerificationResult.UserNotFound))
  }
```

---

### 2.2 Consistent Error Type Hierarchy

**Best Practice:** Use a uniform set of error types across your application, organizing them in a single hierarchy.

**Example:** For deletion of an artwork folder, errors such as access denial, missing organization, and child existence are modeled explicitly:

```scala
sealed trait ArtworkFolderDeleteResult
object ArtworkFolderDeleteResult {
  final case object Deleted extends ArtworkFolderDeleteResult
  final case object NoAccessToOrganization extends ArtworkFolderDeleteResult
  final case object OrganizationNotFound extends ArtworkFolderDeleteResult
  final case object ChildExists extends ArtworkFolderDeleteResult
  final case object ArtworkFolderNotFound extends ArtworkFolderDeleteResult
}

def deleteArtworkFolder[M[_]: Monad](
    authContext: AuthContext,
    organizationId: OrganizationId,
    folderId: ArtworkFolderId,
    membershipService: (OrganizationId, Long) => M[OrganizationUserMembershipResult],
    orgRepository: OrganizationId => M[Option[Organization]],
    folderRepository: ArtworkFolderFilters => M[Option[ArtworkFolder]],
    updateFolder: ArtworkFolder => M[Option[ArtworkFolder]]
): M[ArtworkFolderDeleteResult] =
  (for {
    _ <- Logger[IO].info(s"Deleting folder $folderId").seal
    _ <- membershipService(organizationId, authContext.id)
          .ensureF(_ != OrganizationUserMembershipResult.None,
                   Logger[IO].warning(s"user ${authContext.id} denied access").as(NoAccessToOrganization))
          .map(_ => ())
    org <- orgRepository(organizationId)
             .valueOrF(Logger[IO].error(s"Organization $organizationId not found").as(OrganizationNotFound))
    _ <- folderRepository(ArtworkFolderFilters(parentId = Some(folderId)))
          .ensure(_.isEmpty, ChildExists)
          .map(_ => ())
    updated <- updateFolder(
                 // perform soft delete by updating modifiedBy and archivedAt
                 _.copy(modifiedBy = authContext.id, archivedAt = Some(Instant.now()))
               )
             .valueOrF(Logger[IO].error(s"Folder $folderId not found").as(ArtworkFolderNotFound))
    _ <- Logger[IO].info(s"Folder $folderId deleted successfully").seal
  } yield Deleted).run
```

---

## 3. Function Composition and Code Organization

### 3.1 Helper Functions & Modularization

**Best Practice:** Break down complex workflows into small, well-named helper functions.

**Example:** In a service that composes user login operations, helper functions clearly separate user validation, authentication checks, and token issuance:

```scala
class LoginByCryptoVerificationService[M[_]: Monad] {
  import sealedmonad.syntax._

  private def validatePrevAuthContext(
      prevAuthContext: Option[AuthContext],
      loginRequest: LoginByCryptoVerificationRequest
  ): Step[Boolean] =
    Sync[IO].pure(prevAuthContext.forall(_.id == loginRequest.userId))
      .seal
      .ensure(identity, LoginByCryptoVerificationResult.InvalidCredentials)

  private def filterPublicKeys(user: UserWithIdentities): Step[Seq[UserIdentity]] =
    Sync[IO].pure(user.identities.filter(i => i.`type` == IdentityType.CryptoVerification))
      .seal
      .ensure(_.nonEmpty, LoginByCryptoVerificationResult.InvalidCredentials)

  def login(
      prevAuthContext: Option[AuthContext],
      loginRequest: LoginByCryptoVerificationRequest
  ): M[LoginByCryptoVerificationResult] =
    (for {
      _                   <- validatePrevAuthContext(prevAuthContext, loginRequest)
      userWithIdentities  <- findUserWithIdentities(loginRequest)
      publicKeyIdentities <- filterPublicKeys(userWithIdentities)
      identity            <- verify(publicKeyIdentities, loginRequest)
      result              <- loginResolve(loginRequest, userWithIdentities, prevAuthContext, identity)
    } yield LoginByCryptoVerificationResult.Ok(result)).run
}
```

---

### 3.2 For-Comprehensions for Readability

**Best Practice:** Favor for-comprehensions to flatten nested monadic operations and handle errors gracefully.

**Example:** The following snippet from a permission service demonstrates how conditional logic and complex checks are expressed clearly:

```scala
def checkAccess(authContext: AuthContext, organizationId: OrganizationId): Step[Unit] =
  membershipService(organizationId, authContext.id)
    .seal
    .attempt {
      case OrganizationUserMembershipResult.Admin  => Left(AccessControlResult.Allowed)
      case OrganizationUserMembershipResult.Member => Right(())
      case OrganizationUserMembershipResult.None   => Left(AccessControlResult.NotAllowed)
    }
    .map(_ => ())
```

---

## 4. Side Effect Management

### 4.1 Controlled Side Effects with tap and flatTap

**Best Practice:** Use operators like `tap`, `flatTap`, and `semiflatTap` to execute side effects without disrupting the main pipeline.

**Example:** In the email confirmation service, logging is performed via side effects without altering the result:

```scala
userT.semiflatMap { case (method, user) =>
  upsertAuthMethod(confirmMethod(method))
    .map(_ => ConfirmResponse.Confirmed(issueTokenFor(user)))
}
.merge
```

### 4.2 Branching Side Effects Based on Monad State

**Best Practice:** Differentiate side effects based on the success or error path for accurate logging and debugging.

**Example:** In our permission list service, caching occurs only when a fresh state is computed:

```scala
calculateAndCacheCurrentPermissionsState(organizationId, authUserId).biSemiflatTap(
  {
    case OrganizationUserPermissionListResult.Ok(permissions) =>
      cacheCurrentPermissionsState(organizationId, authUserId, permissions, Instant.now())
    case _ =>
      IO.unit
  },
  permissions => cacheCurrentPermissionsState(organizationId, authUserId, permissions, Instant.now()).map(_.permissions)
)
```

---

## 5. Testing Strategies

### 5.1 Test All Possible Results

**Best Practice:** Write tests for every variant of your sealed result types.

**Example:** In the service for deleting artwork folders, tests assert that each outcome (e.g., no access, folder not found, child exists) is handled correctly.

```scala
// Example test snippet using a testing framework:
"class ArtworkFolderDeleteServiceSpec extends ServiceSpec" in {
  // When user is not member, the operation returns NoAccessToOrganization.
  when(membershipService.membership(orgId, authUser.id)).thenReturn(OrganizationUserMembershipResult.None.asIO)
  sut.delete(authUser, orgId, folderId).asserting(_ shouldEqual ArtworkFolderDeleteResult.NoAccessToOrganization)
}
```

### 5.2 Property-Based and Equivalence Testing

**Best Practice:** Use libraries like ScalaCheck to test that your sealed monad-based operations comply with expected laws and that alternative implementations yield equivalent results.

---

## 6. Integration Patterns

### 6.1 Interfacing with Effectful Libraries

**Best Practice:** Convert your monadic flows to external IO or DBIO monads only at the boundaries of your system.

**Example:** Instead of directly integrating with a real database transactor, wrap conversions inside the monad pipe:

```scala
val transactor: DatabaseTransactor = ???
def findUserFromDB(userId: Long): StepIO[Option[User]] =
  OptionT(transactor.execute(userRepository.find(userId))).seal
```

### 6.2 Polymorphic Design

**Best Practice:** Write your services generically, abstracting over the monad (e.g., F[_]) for maximum reuse with different effect types such as IO, Future, etc.

```scala
def processRequest[F[_]: Monad](req: Request)(implicit sync: Sync[F]): F[Response] = {
  for {
    result <- serviceOperation(req)
    _      <- Logger[F].info(s"Operation completed with $result")
  } yield result
}
```

---

## Conclusion

By following the guidelines presented above and using the concrete Scala examples provided, you can harness the power of Sealed Monad to build applications that are:

•   Highly expressive and self-documenting  
•   Type-safe with exhaustive error handling  
•   Composable and modular  
•   Easy to test and maintain  

These best practices ensure that business logic remains robust, maintainable, and transparent, facilitating smoother integration with effectful libraries and real-world systems. Adopt these patterns to build scalable and resilient applications.
