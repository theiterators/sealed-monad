package pl.iterators.sealedmonad

import cats._
import Function.const

import scala.language.higherKinds

sealed trait Sealed[F[_], +A, +ADT] {
  protected def step[A1 >: A, ADT1 >: ADT](implicit F: Applicative[F]): F[Either[Sealed[F, A1, ADT1], Either[ADT1, A1]]]

  def map[B](f: A => B): Sealed[F, B, ADT]                                    = Sealed.FlatMap(this, (a: A) => Sealed.Value(f(a)))
  def flatMap[B, ADT1 >: ADT](f: A => Sealed[F, B, ADT1]): Sealed[F, B, ADT1] = Sealed.FlatMap(this, f)

  final def semiflatMap[B](f: A => F[B]): Sealed[F, B, ADT]                      = flatMap(a => Sealed.Effect(Eval.later(f(a))))
  final def complete[ADT1 >: ADT](f: A => ADT1): Sealed[F, Nothing, ADT1]        = flatMap(a => Sealed.Result(f(a)))
  final def completeWith[ADT1 >: ADT](f: A => F[ADT1]): Sealed[F, Nothing, ADT1] = flatMap(a => Sealed.ResultF(Eval.later(f(a))))

  final def rethrow[B, ADT1 >: ADT](implicit ev: A <:< Either[ADT1, B]): Sealed[F, B, ADT1] =
    flatMap(a => ev(a).fold(Sealed.Result(_), Sealed.Value(_)))
  final def attempt[B, ADT1 >: ADT](f: A => Either[ADT1, B]): Sealed[F, B, ADT1]     = map(f).rethrow
  final def attemptF[B, ADT1 >: ADT](f: A => F[Either[ADT1, B]]): Sealed[F, B, ADT1] = semiflatMap(f).rethrow

  final def foldM[B, ADT1 >: ADT](left: ADT1 => Sealed[F, B, ADT1], right: A => Sealed[F, B, ADT1]): Sealed[F, B, ADT1] =
    Sealed.Fold(this, left, right)
  final def either: Sealed[F, Either[ADT, A], ADT] = foldM((adt: ADT) => Sealed.Value(Left(adt)), a => Sealed.Value(Right(a)))
  final def inspect(pf: PartialFunction[Either[ADT, A], Any]): Sealed[F, A, ADT] =
    either.map(e => pf.andThen(const(e)).applyOrElse(e, const(e))).rethrow

  final def ensureOr[ADT1 >: ADT](pred: A => Boolean, orElse: A => ADT1): Sealed[F, A, ADT1] =
    attempt(a => Either.cond(pred(a), a, orElse(a)))
  final def ensure[ADT1 >: ADT](pred: A => Boolean, orElse: => ADT1): Sealed[F, A, ADT1]    = ensureOr(pred, _ => orElse)
  final def ensureNot[ADT1 >: ADT](pred: A => Boolean, orElse: => ADT1): Sealed[F, A, ADT1] = ensure(a => !pred(a), orElse)

  final def tap[B](f: A => B): Sealed[F, A, ADT]        = flatMap(a => map(f andThen const(a)))
  final def flatTap[B](f: A => F[B]): Sealed[F, A, ADT] = flatMap(a => semiflatMap(f).map(_ => a))
  final def flatTapWhen[B](cond: A => Boolean, f: A => F[B]): Sealed[F, A, ADT] =
    flatMap(a => if (cond(a)) flatTap(f) else Sealed.Value(a))

  final def run[ADT1 >: ADT](implicit coerce: A <:< ADT1, F: Monad[F]): F[ADT1] = F.fmap(F.tailRecM(this)(_.step))(_.map(coerce).merge)
}

object Sealed extends SealedInstances {

  private[sealedmonad] final case class FlatMap[F[_], A0, A, ADT](current: Sealed[F, A0, ADT], cont: A0 => Sealed[F, A, ADT])
      extends Sealed[F, A, ADT] {
    def runCont[B](f: B => Sealed[F, A0, ADT]) = f andThen (_.flatMap(cont))
    override def step[A1 >: A, ADT1 >: ADT](implicit F: Applicative[F]) = current match {
      case Value(a)         => F.pure(Left(cont(a)))
      case Effect(fa)       => F.fmap(fa.value)(a0 => Left(cont(a0)))
      case FlatMap(prev, g) => F.pure(Left(prev.flatMap(runCont(g))))
      case Fold(prev, l, r) => F.pure(Left(Fold(prev, runCont(l), runCont(r))))
      case _                => sys.error("impossible")
    }
  }
  private[sealedmonad] final case class Effect[F[_], A](fa: Eval[F[A]]) extends Sealed[F, A, Nothing] {
    override protected def step[A1 >: A, ADT1 >: Nothing](implicit F: Applicative[F]) = F.fmap(fa.value)(a => Right(Right(a)))
  }
  private[sealedmonad] final case class ResultF[F[_], ADT](fadt: Eval[F[ADT]]) extends Sealed[F, Nothing, ADT] {
    override def map[B](f: Nothing => B)                                                = this
    override def flatMap[B, ADT1 >: ADT](f: Nothing => Sealed[F, B, ADT1])              = this
    override protected def step[A1 >: Nothing, ADT1 >: ADT](implicit F: Applicative[F]) = F.fmap(fadt.value)(adt => Right(Left(adt)))
  }
  private[sealedmonad] final case class Result[F[_], ADT](result: ADT) extends Sealed[F, Nothing, ADT] {
    override def map[B](f: Nothing => B)                                      = this
    override def flatMap[B, ADT1 >: ADT](f: Nothing => Sealed[F, B, ADT1])    = this
    override def step[A1 >: Nothing, ADT1 >: ADT](implicit F: Applicative[F]) = F.pure(Right(Left(result)))
  }
  private[sealedmonad] final case class Value[F[_], A](a: A) extends Sealed[F, A, Nothing] {
    override def step[A1 >: A, ADT1 >: Nothing](implicit F: Applicative[F]) = F.pure(Right(Right(a)))
  }
  private[sealedmonad] final case class Fold[F[_], A0, A, ADT](value: Sealed[F, A0, ADT],
                                                               left: ADT => Sealed[F, A, ADT],
                                                               right: A0 => Sealed[F, A, ADT])
      extends Sealed[F, A, ADT] {
    def runFold[B](f: B => Sealed[F, A0, ADT]) = f andThen (_.foldM(left, right))
    override def step[A1 >: A, ADT1 >: ADT](implicit F: Applicative[F]) = value match {
      case Result(adt)      => F.pure(Left(left(adt)))
      case ResultF(fadt)    => F.fmap(fadt.value)(adt => Left(left(adt)))
      case Value(a)         => F.pure(Left(right(a)))
      case Effect(fa)       => F.fmap(fa.value)(a0 => Left(right(a0)))
      case FlatMap(prev, f) => F.pure(Left(prev.flatMap(runFold(f))))
      case Fold(v0, l0, r0) => F.pure(Left(Fold(v0, runFold(l0), runFold(r0))))
    }
  }

  import cats.syntax.either._

  def liftF[F[_], A](value: A): Sealed[F, A, Nothing]              = Value(value)
  def apply[F[_], A](value: => F[A]): Sealed[F, A, Nothing]        = Effect(Eval.later(value))
  def seal[F[_], A](value: A): Sealed[F, Nothing, A]               = Result(value)
  def result[F[_], ADT](value: => F[ADT]): Sealed[F, Nothing, ADT] = ResultF(Eval.later(value))

  def valueOr[F[_], A, ADT](fa: => F[Option[A]], orElse: => ADT): Sealed[F, A, ADT] = Sealed(fa).attempt(Either.fromOption(_, orElse))

  def valueOrF[F[_], A, ADT](fa: => F[Option[A]], orElse: => F[ADT]): Sealed[F, A, ADT] =
    Sealed(fa).flatMap {
      case Some(a) => liftF(a)
      case None    => result(orElse)
    }

  def handleError[F[_], A, B, ADT](fa: F[Either[A, B]])(f: A => ADT): Sealed[F, B, ADT] = Sealed(fa).attempt(_.leftMap(f))
}

private final class SealedMonad[F[_], ADT] extends StackSafeMonad[Sealed[F, ?, ADT]] {
  override def pure[A](x: A)                                                   = Sealed.liftF(x)
  override def flatMap[A, B](fa: Sealed[F, A, ADT])(f: A => Sealed[F, B, ADT]) = fa.flatMap(f)
  override def map[A, B](fa: Sealed[F, A, ADT])(f: A => B)                     = fa.map(f)
  override def widen[A, B >: A](fa: Sealed[F, A, ADT])                         = fa
}

trait SealedInstances {
  implicit def sealedMonad[F[_], ADT]: Monad[Sealed[F, ?, ADT]] = new SealedMonad
}
