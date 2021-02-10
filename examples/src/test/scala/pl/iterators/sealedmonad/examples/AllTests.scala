package pl.iterators.sealedmonad.examples

import cats.{Eq, Monad}
import org.scalacheck.{Arbitrary, Cogen}
import Options._

import scala.language.higherKinds

trait AllTests[M[_]] extends OptionsTests[M] {
  override implicit def M: Monad[M]

  def examples(implicit
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
  ) = new RuleSet {
    override val name    = "all"
    override val bases   = Seq.empty
    override val parents = Seq(options)
    override val props   = Seq.empty
  }
}

object AllTests {

  def apply[M[_]](implicit ev: Monad[M]) = new AllTests[M] {
    override implicit val M: Monad[M] = ev
  }

}
