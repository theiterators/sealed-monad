package pl.iterators.sealedmonad.syntax

import cats.{Functor, Monad}
import pl.iterators.sealedmonad.Sealed

import scala.language.{higherKinds, implicitConversions}

trait SealedSyntax {
  implicit final def faSyntax[F[_], A](fa: F[A]): SealedFAOps[F, A]                              = new SealedFAOps(fa)
  implicit final def faOps[A](a: A): SealedOps[A]                                                = new SealedOps(a)
  implicit final def faOptSyntax[F[_], A](fa: F[Option[A]]): SealedFOptAOps[F, A]                = new SealedFOptAOps(fa)
  implicit final def faEitherSyntax[F[_], A, B](fa: F[Either[A, B]]): SealedFAEitherOps[F, A, B] = new SealedFAEitherOps(fa)
  implicit final def sealedSyntax[F[_], A, ADT](s: Sealed[F, A, ADT]): SealedSymbolic[F, A, ADT] = new SealedSymbolic(s)
  implicit final def optSyntax[A](condOpt: Option[A]): SealedOptOps[A]                           = new SealedOptOps[A](condOpt)
  implicit final def eitherSyntax[ADT, A](condEither: Either[ADT, A]): SealedEitherOps[ADT, A]   = new SealedEitherOps[ADT, A](condEither)
}

final class SealedFAOps[F[_], A](private val self: F[A]) extends AnyVal {
  def seal[ADT]: Sealed[F, A, ADT]                                                                  = Sealed.value[ADT](self)
  def ensure[ADT](pred: A => Boolean, orElse: => ADT)(implicit ev: Monad[F]): Sealed[F, A, ADT]     = seal[ADT].ensure(pred, orElse)
  def ensureOr[ADT](pred: A => Boolean, orElse: A => ADT)(implicit ev: Monad[F]): Sealed[F, A, ADT] = seal[ADT].ensureOr(pred, orElse)
  def attempt[ADT, B](f: A => Either[ADT, B])(implicit ev: Monad[F]): Sealed[F, B, ADT]             = seal[ADT].attempt(f)
  def attemptF[ADT, B](f: A => F[Either[ADT, B]])(implicit ev: Monad[F]): Sealed[F, B, ADT]         = seal[ADT].attemptF(f)
}

final class SealedFOptAOps[F[_], A](private val self: F[Option[A]]) extends AnyVal {
  def valueOr[ADT](orElse: => ADT)(implicit ev: Monad[F]): Sealed[F, A, ADT]     = Sealed.valueOr(self, orElse)
  def valueOrF[ADT](orElse: => F[ADT])(implicit ev: Monad[F]): Sealed[F, A, ADT] = Sealed.valueOrF(self, orElse)
}

final class SealedFAEitherOps[F[_], A, B](private val self: F[Either[A, B]]) extends AnyVal {
  def merge[ADT](f: Either[A, B] => ADT)(implicit ev: Monad[F]): Sealed[F, ADT, ADT] = Sealed.merge(self)(f)
  def mergeF[ADT](f: Either[A, B] => F[ADT]): Sealed[F, ADT, ADT]                    = Sealed.mergeF(self)(f)
  def handleError[ADT](f: A => ADT)(implicit ev: Monad[F]): Sealed[F, B, ADT]        = Sealed.handleError(self)(f)
}

final class SealedOps[A](private val self: A) extends AnyVal {
  def result[F[_]: Monad]: Sealed[F, Nothing, A]      = Sealed.result[F](self)
  def liftSealed[F[_]: Monad, ADT]: Sealed[F, A, ADT] = Sealed.liftF[F, ADT](self)
}

final class SealedOptOps[A](private val self: Option[A]) extends AnyVal {
  def sealCond[F[_]: Monad, ADT] = new SealOptCondPartiallyApplied(self)
}

final class SealedEitherOps[ADT, A](private val self: Either[ADT, A]) extends AnyVal {
  def sealCond[F[_]: Monad]: Sealed[F, A, ADT] = Sealed.liftF[F, ADT](self).rethrow
}

final class SealOptCondPartiallyApplied[A, F[_]: Monad](private val self: Option[A]) {
  def apply[ADT](ifNone: => ADT): Sealed[F, A, ADT] = self.fold[Sealed[F, A, ADT]](Sealed.result(ifNone))(a => Sealed.liftF[F, ADT](a))
}

final class SealedSymbolic[F[_], A, ADT](private val self: Sealed[F, A, ADT]) extends AnyVal {
  def >>![B](f: A => F[B])(implicit ev: Functor[F]): Sealed[F, A, ADT]   = self.tap(f)
  def ![B](sideEffect: F[B])(implicit ev: Functor[F]): Sealed[F, A, ADT] = >>!(_ => sideEffect)
}
