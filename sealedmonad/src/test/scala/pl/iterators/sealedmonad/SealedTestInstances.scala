package pl.iterators.sealedmonad

import cats.laws.discipline.SemigroupalTests.Isomorphisms
import cats.{Eq, Monad}
import org.scalacheck._

import scala.language.higherKinds

trait SealedTestInstances {
  import SealedTestInstances.ADT

  implicit def ArbSealed1[F[_], A: Arbitrary, B: Arbitrary]: Arbitrary[Sealed[F, A, B]] =
    Arbitrary[Sealed[F, A, B]] {
      Gen.oneOf(ArbSealed2[F, A].arbitrary, ArbSealed3[F, B].arbitrary)
    }

  implicit def ArbSealed2[F[_], A: Arbitrary]: Arbitrary[Sealed[F, A, Nothing]] =
    Arbitrary[Sealed[F, A, Nothing]] {
      Arbitrary.arbitrary[A].map(a => Sealed.liftF[F, A](a))
    }

  implicit def ArbSealed3[F[_], A: Arbitrary]: Arbitrary[Sealed[F, Nothing, A]] =
    Arbitrary[Sealed[F, Nothing, A]] {
      Arbitrary.arbitrary[A].map(a => Sealed.seal[F, A](a))
    }

  implicit def EqSealed1[F[_]: Monad, A](implicit eqF: Eq[F[ADT]]): Eq[Sealed[F, A, ADT]] =
    (x: Sealed[F, A, ADT], y: Sealed[F, A, ADT]) => {
      val resultX = x.map(ADT.Case4(_)).run
      val resultY = y.map(ADT.Case4(_)).run
      Eq[F[ADT]].eqv(resultX, resultY)
    }

  implicit def EqSealed2[F[_]: Monad, A](implicit eqF: Eq[F[ADT]]): Eq[Sealed[F, A, Nothing]] =
    EqSealed1[F, A].asInstanceOf[Eq[Sealed[F, A, Nothing]]]

  implicit def iso[F[_]]: Isomorphisms[Sealed[F, *, ADT]] = Isomorphisms.invariant[Sealed[F, *, ADT]]
}

object SealedTestInstances {
  sealed trait ADT

  object ADT {
    case object Case1         extends ADT
    case object Case2         extends ADT
    case object Case3         extends ADT
    case class Case4[A](a: A) extends ADT

    implicit val EqADT: Eq[ADT] = Eq.fromUniversalEquals[ADT]

    implicit val ArbADT: Arbitrary[ADT] = Arbitrary[ADT] {
      Gen.oneOf(Gen.const(Case1), Gen.const(Case2), Gen.const(Case3))
    }

    implicit val CogenADT: Cogen[ADT] = Cogen[Int].contramap {
      case Case1 => 1
      case Case2 => 2
      case Case3 => 3
      case _     => 0
    }
  }
}
