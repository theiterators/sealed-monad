package pl.iterators.sealed_monad

import cats._
import cats.syntax.either._

import scala.Function.const
import scala.language.higherKinds

sealed trait Sealed[F[_], +A, ADT] {
  import Sealed.{Computation, Result, Value}

  final def run(implicit ev: A <:< ADT, F: Monad[F]): F[ADT] = F.fmap(F.tailRecM(this)(_.step))(_.map(ev).merge)

  protected def step[A1 >: A](implicit F: Monad[F]): F[Either[Sealed[F, A1, ADT], Either[ADT, A1]]]

  def map[B](f: A => B)(implicit F: Functor[F]): Sealed[F, B, ADT]
  def flatMap[B](f: A => Sealed[F, B, ADT]): Sealed[F, B, ADT] = Computation(this, f)

  def semiflatMap[B](f: A => F[B]): Sealed[F, B, ADT]      = flatMap(a => Value(f(a)))
  def complete[B](f: A => F[ADT]): Sealed[F, Nothing, ADT] = flatMap(a => Result(f(a)))

  def attempt[B](f: A => Either[ADT, B])(implicit F: Monad[F]): Sealed[F, B, ADT]     = map(f).rethrow
  def attemptF[B](f: A => F[Either[ADT, B]])(implicit F: Monad[F]): Sealed[F, B, ADT] = semiflatMap(f).rethrow

  def rethrow[B](implicit ev: A <:< Either[ADT, B], F: Monad[F]): Sealed[F, B, ADT] =
    flatMap(a => ev(a).fold(adt => Result(F.pure(adt)), b => Value(F.pure(b))))

  def ensure(pred: A => Boolean, orElse: => ADT)(implicit F: Monad[F]): Sealed[F, A, ADT]    = ensureOr(pred, _ => orElse)
  def ensureNot(pred: A => Boolean, orElse: => ADT)(implicit F: Monad[F]): Sealed[F, A, ADT] = ensure(a => !pred(a), orElse)

  def ensureOr(pred: A => Boolean, orElse: A => ADT)(implicit F: Monad[F]): Sealed[F, A, ADT] =
    attempt(a => Either.cond(pred(a), a, orElse(a)))

  def tap[B](f: A => F[B])(implicit F: Functor[F]): Sealed[F, A, ADT] = flatMap(a => Value(F.as(f(a), a)))

  def tapWhen[B](cond: A => Boolean, f: A => F[B])(implicit F: Applicative[F]): Sealed[F, A, ADT] =
    flatMap(a => if (cond(a)) Value(F.as(f(a), a)) else Value(F.pure(a)))

  def inspect(pf: PartialFunction[Either[ADT, A], Any])(implicit F: Functor[F]): Sealed[F, A, ADT]
}

object Sealed extends SealedInstances {
  private def piggybackF[A, B, U](x: B, pf: PartialFunction[B, U]): A => A = a => pf.andThen(const(a)).applyOrElse(x, const(a))

  private case class Computation[F[_], A0, A, ADT](current: Sealed[F, A0, ADT], cont: A0 => Sealed[F, A, ADT]) extends Sealed[F, A, ADT] {
    self =>
    override protected def step[A1 >: A](implicit F: Monad[F]) = current match {
      case Result(result)       => F.fmap(result)(adt => Right(Left(adt)))
      case Value(fa0)           => F.fmap(fa0)(a0 => Left(cont(a0)))
      case Computation(prev, g) => F.pure(Left(prev.flatMap(a0 => g(a0).flatMap(cont))))
    }

    /**
      * Please note that this is NOT stack-safe.
      * Stack will blow up when doing things like
      * sealed_monad.flatMap { ... }.map(f1).map(f2).(...)
      * Anyhow, this is not as bad as it sounds as f1, f2, ... can be coalesced into one function
      * (which is not the case generally with `flatMap`). Thus it is more imporant that `flatMap` is kept stack-safe (and it is)
      */
    override def map[B](f: A => B)(implicit F: Functor[F]) = Computation(current, cont andThen (_.map(f)))
    override def inspect(pf: PartialFunction[Either[ADT, A], Any])(implicit F: Functor[F]) =
      Computation(current, cont andThen (_.inspect(pf)))

    override def toString = s"Computation($current, ...)"
  }
  private case class Result[F[_], ADT](result: F[ADT]) extends Sealed[F, Nothing, ADT] {
    override protected def step[A1 >: Nothing](implicit F: Monad[F]) = F.fmap(result)(adt => Right(Left(adt)))
    override def map[B](f: Nothing => B)(implicit F: Functor[F])     = this
    override def flatMap[B](f: Nothing => Sealed[F, B, ADT])         = this

    override def inspect(pf: PartialFunction[Either[ADT, Nothing], Any])(implicit F: Functor[F]) =
      Result(F.fmap(result)(adt => piggybackF(Left(adt), pf)(adt)))

    override def toString = s"Result($result)"
  }
  private case class Value[F[_], A, ADT](fa: F[A]) extends Sealed[F, A, ADT] {
    override protected def step[A1 >: A](implicit F: Monad[F]) = F.fmap(fa)(a => Right(Right(a)))
    override def map[B](f: A => B)(implicit F: Functor[F])     = Value(F.fmap(fa)(f))

    override def toString                                                                  = s"Value($fa)"
    override def inspect(pf: PartialFunction[Either[ADT, A], Any])(implicit F: Functor[F]) = map(a => piggybackF(Right(a), pf)(a))
  }

  def liftF[F[_], ADT]                                       = new LiftFPartiallyApplied[F, ADT]
  def value[ADT]                                             = new ValuePartiallyApplied[ADT]
  def result[F[_]]                                           = new ResultPartiallyApplied[F]
  def resultF[F[_], ADT](f: F[ADT]): Sealed[F, Nothing, ADT] = Result(f)

  def valueOr[F[_]: Monad, A, ADT](fa: F[Option[A]], orElse: => ADT): Sealed[F, A, ADT] =
    value[ADT](fa).attempt(Either.fromOption(_, orElse))

  def valueOrF[F[_]: Monad, A, ADT](fa: F[Option[A]], orElse: => F[ADT]): Sealed[F, A, ADT] =
    value[ADT](fa).flatMap {
      case Some(a) => Value[F, A, ADT](Monad[F].pure(a))
      case None    => Result(orElse)
    }

  def merge[F[_]: Monad, A, B, ADT](fa: F[Either[A, B]])(f: Either[A, B] => ADT): Sealed[F, ADT, ADT] =
    mergeF(fa)(either => Monad[F].pure(f(either)))
  def mergeF[F[_], A, B, ADT](fa: F[Either[A, B]])(f: Either[A, B] => F[ADT]): Sealed[F, ADT, ADT] = value[ADT](fa).complete(f)

  def handleError[F[_]: Monad, A, B, ADT](fa: F[Either[A, B]])(f: A => ADT): Sealed[F, B, ADT] = value[ADT](fa).attempt(_.leftMap(f))

  final class ValuePartiallyApplied[ADT] {
    def apply[F[_], A](value: F[A]): Sealed[F, A, ADT] = Value[F, A, ADT](value)
  }
  final class LiftFPartiallyApplied[F[_], ADT] {
    def apply[A](value: A)(implicit M: Monad[F]): Sealed[F, A, ADT] = Value[F, A, ADT](Monad[F].pure(value))
  }
  final class ResultPartiallyApplied[F[_]] {
    def apply[A](value: A)(implicit M: Monad[F]): Sealed[F, Nothing, A] = resultF(Monad[F].pure(value))
  }
}

private final class SealedMonad[F[_]: Monad, ADT] extends StackSafeMonad[Sealed[F, ?, ADT]] {
  override def pure[A](x: A)                                                   = Sealed.liftF[F, ADT](x)
  override def flatMap[A, B](fa: Sealed[F, A, ADT])(f: A => Sealed[F, B, ADT]) = fa.flatMap(f)
  override def map[A, B](fa: Sealed[F, A, ADT])(f: A => B)                     = fa.map(f)
  override def widen[A, B >: A](fa: Sealed[F, A, ADT])                         = fa
}

trait SealedInstances {
  implicit def sealedMonad[F[_]: Monad, ADT]: Monad[Sealed[F, ?, ADT]] = new SealedMonad
}
