---
sidebar_position: 7
---

# Migration Guide

This guide provides step-by-step instructions for migrating existing code to use Sealed Monad.

## Migrating from Pattern Matching

If you're currently using nested pattern matching with Options or Either, here's how to migrate to Sealed Monad.

### Before:

```scala
def confirmEmail(
  token: String,
  findAuthMethod: String => Future[Option[AuthMethod]],
  findUser: Long => Future[Option[User]],
  upsertAuthMethod: AuthMethod => Future[Int],
  issueTokenFor: User => String,
  confirmMethod: AuthMethod => AuthMethod
): Future[ConfirmResponse] = {
  findAuthMethod(token).flatMap {
    case None =>
      Future.successful(ConfirmResponse.MethodNotFound)
    case Some(method) =>
      findUser(method.userId).flatMap {
        case None =>
          Future.successful(ConfirmResponse.UserNotFound)
        case Some(user) =>
          upsertAuthMethod(confirmMethod(method)).map { _ =>
            ConfirmResponse.Confirmed(issueTokenFor(user))
          }
      }
  }
}
```

### Migration Steps:

1. **Identify the result ADT**: Make sure you have a sealed trait hierarchy for the responses.

```scala
sealed trait ConfirmResponse
object ConfirmResponse {
  case object MethodNotFound extends ConfirmResponse
  case object UserNotFound extends ConfirmResponse
  final case class Confirmed(jwt: String) extends ConfirmResponse
}
```

2. **Import the Sealed Monad syntax**: Add the import at the top of your file.

```scala
import pl.iterators.sealedmonad.syntax._
```

3. **Identify the pattern matching branches**: Look for places where you're pattern matching on Option/Either results.

4. **Rewrite using Sealed Monad operators**:
   - Use `valueOr` for Option extraction
   - Use `ensure` for conditional checks
   - Use `attempt` for Either conversion
5. **Structure as a for-comprehension**: Put the steps in a for-comprehension.

6. **Add the .run call**: Complete the computation with `.run`.

### After:

```scala
def confirmEmail(
  token: String,
  findAuthMethod: String => Future[Option[AuthMethod]],
  findUser: Long => Future[Option[User]],
  upsertAuthMethod: AuthMethod => Future[Int],
  issueTokenFor: User => String,
  confirmMethod: AuthMethod => AuthMethod
): Future[ConfirmResponse] = {
  val s = for {
    method <- findAuthMethod(token)
              .valueOr[ConfirmResponse](ConfirmResponse.MethodNotFound)

    user <- findUser(method.userId)
            .valueOr[ConfirmResponse](ConfirmResponse.UserNotFound)

    _ <- upsertAuthMethod(confirmMethod(method)).seal
  } yield ConfirmResponse.Confirmed(issueTokenFor(user))

  s.run
}
```

## Migrating from EitherT

If you're currently using Cats' EitherT, here's how to migrate to Sealed Monad.

### Before:

```scala
def confirmEmail(
  token: String,
  findAuthMethod: String => Future[Option[AuthMethod]],
  findUser: Long => Future[Option[User]],
  upsertAuthMethod: AuthMethod => Future[Int],
  issueTokenFor: User => String,
  confirmMethod: AuthMethod => AuthMethod
): Future[ConfirmResponse] = {
  val userT = for {
    method <- EitherT.fromOptionF(
                findAuthMethod(token),
                ifNone = ConfirmResponse.MethodNotFound
              )
    user <- EitherT.fromOptionF(
              findUser(method.userId),
              ifNone = ConfirmResponse.UserNotFound
            )
  } yield (method, user)

  userT.semiflatMap { case (method, user) =>
    upsertAuthMethod(confirmMethod(method))
      .map(_ => ConfirmResponse.Confirmed(issueTokenFor(user)))
  }.merge
}
```

### Migration Steps:

1. **Import the Sealed Monad syntax**: Add the import at the top of your file.

```scala
import pl.iterators.sealedmonad.syntax._
```

2. **Replace EitherT operations with Sealed Monad equivalents**:

   - `EitherT.fromOptionF(opt, ifNone)` → `opt.valueOr(ifNone)`
   - `EitherT.cond(test, right, left)` → `right.pure[F].ensure(_ => test, left)`
   - `eitherT.semiflatMap` → `sealed.flatMap` or `sealed.flatTap`
   - `.merge` → `.run`

3. **Structure as a for-comprehension**: Put the steps in a for-comprehension.

4. **Add the .run call**: Complete the computation with `.run`.

### After:

```scala
def confirmEmail(
  token: String,
  findAuthMethod: String => Future[Option[AuthMethod]],
  findUser: Long => Future[Option[User]],
  upsertAuthMethod: AuthMethod => Future[Int],
  issueTokenFor: User => String,
  confirmMethod: AuthMethod => AuthMethod
): Future[ConfirmResponse] = {
  val s = for {
    method <- findAuthMethod(token)
              .valueOr[ConfirmResponse](ConfirmResponse.MethodNotFound)

    user <- findUser(method.userId)
            .valueOr[ConfirmResponse](ConfirmResponse.UserNotFound)

    _ <- upsertAuthMethod(confirmMethod(method)).seal
  } yield ConfirmResponse.Confirmed(issueTokenFor(user))

  s.run
}
```

## Gradual Migration Strategy

When migrating a large codebase, consider this gradual approach:

1. **Start with leaf methods**: Begin with methods that don't depend on other methods returning ADTs.

2. **Create ADT wrappers**: For methods you're not ready to migrate, create wrappers that return the appropriate ADT.

```scala
// Original method
def findUser(id: String): Future[Option[User]] = ???

// Wrapper for use with Sealed Monad
def findUserOrError(id: String): Future[UserResponse] =
  findUser(id).map(_.fold[UserResponse](UserResponse.NotFound)(UserResponse.Found))
```

3. **Migrate core business logic first**: Focus on complex business logic with multiple error cases first, as these will benefit most from Sealed Monad.

4. **Update tests**: Make sure to update tests to verify both success and failure paths.

5. **Refactor in small, focused PRs**: Don't try to migrate everything at once. Focus on small, manageable pull requests.

## Mixing Sealed Monad with Other Approaches

During migration, you might need to mix Sealed Monad with existing approaches:

### Integrating with EitherT

```scala
// Convert from EitherT to Sealed
def fromEitherT[F[_], A, B](eitherT: EitherT[F, B, A]): Sealed[F, A, B] =
  Sealed(eitherT.value).rethrow

// Convert from Sealed to EitherT
def toEitherT[F[_]: Monad, A, B](sealed: Sealed[F, A, B]): EitherT[F, B, A] =
  EitherT(sealed.either.run)
```

### Integrating with Option-returning functions

```scala
// When calling external code that returns Option
def callLegacyCode(id: String): Sealed[F, User, UserError] =
  legacyService.findUserById(id).valueOr(UserError.NotFound)
```

## Final Checklist

Before considering a migration complete, ensure you've:

- [ ] Imported `pl.iterators.sealedmonad.syntax._` wherever needed
- [ ] Replaced all pattern matching with Sealed Monad operators
- [ ] Added `.run` to execute computations
- [ ] Updated tests to verify both success and error paths
- [ ] Updated documentation to reflect the new approach
- [ ] Reviewed for readability and consistency

With these steps, you should be able to successfully migrate to Sealed Monad and enjoy cleaner, more maintainable error handling in your code.
