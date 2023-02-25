package pl.iterators.sealedmonad.syntax

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
  def seal[ADT]: Sealed[F, A, ADT]                                           = Sealed(self)
  def ensure[ADT](pred: A => Boolean, orElse: => ADT): Sealed[F, A, ADT]     = seal[ADT].ensure(pred, orElse)
  def ensureOr[ADT](pred: A => Boolean, orElse: A => ADT): Sealed[F, A, ADT] = seal[ADT].ensureOr(pred, orElse)
  def ensureF[ADT](pred: A => Boolean, orElse: => F[ADT]): Sealed[F, A, ADT] = seal[ADT].ensureF(pred, orElse)
  def attempt[ADT, B](f: A => Either[ADT, B]): Sealed[F, B, ADT]             = seal[ADT].attempt(f)
  def attemptF[ADT, B](f: A => F[Either[ADT, B]]): Sealed[F, B, ADT]         = seal[ADT].attemptF(f)
}

final class SealedFOptAOps[F[_], A](private val self: F[Option[A]]) extends AnyVal {

  /** Returns a Sealed instance containing value `A` if it's defined, else short-circuits with given `ADT` value.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedFOptAOps
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[List, Int, Response] = List(Option(1)).valueOr(NotFound)
    * scala> (for {value <- sealedSome} yield Value(value)).run
    * res0: List[Response] = List(Value(1))
    * scala> val sealedNone: Sealed[List, Int, Response] = List(Option.empty[Int]).valueOr(NotFound)
    * scala> (for {value <- sealedNone} yield Value(value)).run
    * res1 : List[Response] = List(NotFound)
    * }}}
    */
  def valueOr[ADT](orElse: => ADT): Sealed[F, A, ADT] = Sealed.valueOr(self, orElse)

  /** Returns a Sealed instance containing value `A` if it's defined, else short-circuits with given effectful `ADT` value.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedFOptAOps
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[List, Int, Response] = List(Option(1)).valueOrF(List(NotFound))
    * scala> (for {value <- sealedSome} yield Value(value)).run
    * res0: List[Response] = List(Value(1))
    * scala> val sealedNone: Sealed[List, Int, Response] = List(Option.empty[Int]).valueOrF(List(NotFound))
    * scala> (for {value <- sealedNone} yield Value(value)).run
    * res1 : List[Response] = List(NotFound)
    * }}}
    */
  def valueOrF[ADT](orElse: => F[ADT]): Sealed[F, A, ADT] = Sealed.valueOrF(self, orElse)
}

final class SealedFAEitherOps[F[_], A, B](private val self: F[Either[A, B]]) extends AnyVal {
  def merge[ADT](f: Either[A, B] => ADT): Sealed[F, ADT, ADT]     = Sealed(self).complete(f)
  def mergeF[ADT](f: Either[A, B] => F[ADT]): Sealed[F, ADT, ADT] = Sealed(self).completeWith(f)
  def handleError[ADT](f: A => ADT): Sealed[F, B, ADT]            = Sealed.handleError(self)(f)
}

final class SealedOps[A](private val self: A) extends AnyVal {
  def seal[F[_]]: Sealed[F, Nothing, A]        = Sealed.seal(self)
  def liftSealed[F[_], ADT]: Sealed[F, A, ADT] = Sealed.liftF(self)
}

final class SealedOptOps[A](private val self: Option[A]) extends AnyVal {
  def sealCond[F[_], ADT] = new SealOptCondPartiallyApplied(self)
}

final class SealedEitherOps[ADT, A](private val self: Either[ADT, A]) extends AnyVal {
  def rethrow[F[_]]: Sealed[F, A, ADT] = Sealed.liftF(self).rethrow
}

final class SealOptCondPartiallyApplied[F[_], A](private val self: Option[A]) {
  def apply[ADT](ifNone: => ADT): Sealed[F, A, ADT] = self.fold[Sealed[F, A, ADT]](Sealed.seal(ifNone))(a => Sealed.liftF(a))
}

final class SealedSymbolic[F[_], A, ADT](private val self: Sealed[F, A, ADT]) extends AnyVal {
  def >>![B](f: A => F[B]): Sealed[F, A, ADT]   = self.flatTap(f)
  def ![B](sideEffect: F[B]): Sealed[F, A, ADT] = >>!(_ => sideEffect)
}
