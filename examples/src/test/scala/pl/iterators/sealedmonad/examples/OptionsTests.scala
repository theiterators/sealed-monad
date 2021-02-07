package pl.iterators.sealedmonad.examples

import cats.{Eq, Monad}
import org.scalacheck.Prop._
import org.scalacheck._
import org.typelevel.discipline.Laws
import pl.iterators.sealedmonad.examples.Options._
import pl.iterators.sealedmonad.examples.Options._

import scala.language.higherKinds

trait OptionsTests[M[_]] extends Laws {
  implicit def M: Monad[M]

  import cats.kernel.laws._
  import discipline._

  def loginEqv(
      email: String,
      findUser: String => M[Option[User]],
      findAuthMethod: (Long, Provider) => M[Option[AuthMethod]],
      issueTokenFor: User => String,
      checkAuthMethodAction: AuthMethod => Boolean,
      authMethodFromUserIdF: Long => AuthMethod,
      mergeAccountsAction: (AuthMethod, User) => M[LoginResponse]
  ) =
    Example1
      .login[M](
        email,
        findUser,
        findAuthMethod,
        issueTokenFor,
        checkAuthMethodAction,
        authMethodFromUserIdF,
        mergeAccountsAction
      ) <-> Example1
      .sealedLogin[M](email, findUser, findAuthMethod, issueTokenFor, checkAuthMethodAction, authMethodFromUserIdF, mergeAccountsAction)

  def confirmEmailEqv(
      token: String,
      findAuthMethod: String => M[Option[AuthMethod]],
      findUser: Long => M[Option[User]],
      upsertAuthMethod: AuthMethod => M[Int],
      issueTokenFor: User => String,
      confirmMethod: AuthMethod => AuthMethod
  ) =
    Example2.confirmEmail[M](token, findAuthMethod, findUser, upsertAuthMethod, issueTokenFor, confirmMethod) <-> Example2
      .sealedConfirmEmail[M](token, findAuthMethod, findUser, upsertAuthMethod, issueTokenFor, confirmMethod)

  def options(implicit
      ArbAuth: Arbitrary[AuthMethod],
      ArbMOptUser: Arbitrary[M[Option[User]]],
      ArbMOptAuth: Arbitrary[M[Option[AuthMethod]]],
      ArbLoginResponse: Arbitrary[M[LoginResponse]],
      ArbMInt: Arbitrary[M[Int]],
      CoProvider: Cogen[Provider],
      CoAuth: Cogen[AuthMethod],
      CoUser: Cogen[User],
      EqMLogResp: Eq[M[LoginResponse]],
      EqMConfirmResp: Eq[M[ConfirmResponse]]
  ) =
    new SimpleRuleSet(name = "Options", "Example1.login" -> forAll(loginEqv _), "Example2.confirmEmail" -> forAll(confirmEmailEqv _))
}

object OptionsTests {
  def apply[M[_]](implicit ev: Monad[M]): OptionsTests[M] = new OptionsTests[M] { override implicit val M: Monad[M] = ev }
}

trait OptionsTestInstances {
  implicit val ArbitraryProvider: Arbitrary[Provider] = Arbitrary(Gen.oneOf(Provider.EmailPass, Provider.Facebook, Provider.LinkedIn))

  implicit val CoProvider: Cogen[Provider] = Cogen[Int].contramap {
    case Provider.EmailPass => 0
    case Provider.Facebook  => 1
    case Provider.LinkedIn  => 2
  }

  implicit val ArbitraryAuthMethod: Arbitrary[AuthMethod] = Arbitrary(for {
    provider_ <- Arbitrary.arbitrary[Provider]
    userId_   <- Arbitrary.arbitrary[Long]
  } yield new AuthMethod {
    override val provider = provider_
    override val userId   = userId_
  })
  implicit val CoAuthMethod: Cogen[AuthMethod] = Cogen[(Long, Provider)].contramap(am => (am.userId, am.provider))

  implicit val ArbitraryUser: Arbitrary[User] = Arbitrary(for {
    id_       <- Arbitrary.arbitrary[Long]
    archived_ <- Arbitrary.arbitrary[Boolean]
  } yield new User {
    override val id       = id_
    override val archived = archived_
  })
  implicit val CoUser: Cogen[User] = Cogen[(Long, Boolean)].contramap(u => (u.id, u.archived))

  implicit val ArbitraryLoginResponse: Arbitrary[LoginResponse] = Arbitrary(
    Gen.oneOf(
      Gen.oneOf(
        LoginResponse.Deleted,
        LoginResponse.AccountsMergeRequested,
        LoginResponse.InvalidCredentials,
        LoginResponse.ProviderAuthFailed
      ),
      Arbitrary.arbitrary[String].flatMap(s => Gen.oneOf(LoginResponse.LoggedIn(s), LoginResponse.AccountsMerged(s)))
    )
  )
  implicit val eqLoginResponse: Eq[LoginResponse] = Eq.fromUniversalEquals[LoginResponse]

  implicit val eqConfirmResponse: Eq[ConfirmResponse] = Eq.fromUniversalEquals[ConfirmResponse]
}
