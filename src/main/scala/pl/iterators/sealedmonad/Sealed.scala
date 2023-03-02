package pl.iterators.sealedmonad

import cats._
import Function.const
import cats.syntax.bitraverse._
import scala.language.higherKinds

sealed trait Sealed[F[_], +A, +ADT] {
  protected def step[A1 >: A, ADT1 >: ADT](implicit F: Applicative[F]): F[Either[Sealed[F, A1, ADT1], Either[ADT1, A1]]]

  def map[B](f: A => B): Sealed[F, B, ADT]                                    = Sealed.FlatMap(this, (a: A) => Sealed.Value(f(a)))
  def flatMap[B, ADT1 >: ADT](f: A => Sealed[F, B, ADT1]): Sealed[F, B, ADT1] = Sealed.FlatMap(this, f)

  /** Transforms `A` to `B` using an effectful function.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax._
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[Id, Int, Response] = Id(Option(1)).valueOr(NotFound)
    * scala> (for { x <- sealedSome.semiflatMap(_ => Id(42)) } yield Value(x)).run
    * res0: cats.Id[Response] = Value(42)
    * }}}
    */
  final def semiflatMap[B](f: A => F[B]): Sealed[F, B, ADT] = flatMap(a => Sealed.Effect(Eval.later(f(a))))

  final def leftSemiflatMap[ADT1](f: ADT => F[ADT1])(implicit F: Applicative[F]): Sealed[F, A, ADT1] =
    either
      .semiflatMap(_.leftTraverse(f))
      .asInstanceOf[Sealed[F, Either[ADT1, A], ADT1]]
      .rethrow

  /** Executes a side effect if ADT has been reached, and returns unchanged `Sealed[F, A, ADT]`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax._
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[Id, Int, Response] = Id(Option(1)).valueOr(NotFound)
    * scala> (for { x <- sealedSome.leftSemiflatTap(_ => Id(println("left"))) } yield Value(x)).run
    * res0: cats.Id[Response] = Value(1)
    * scala> val sealedNone: Sealed[Id, Int, Response] = Id(Option.empty).valueOr(NotFound)
    * scala> (for { x <- sealedNone.leftSemiflatTap(_ => Id(println("left"))) } yield Value(x)).run
    * res1: cats.Id[Response] = NotFound
    * // prints 'left'
    * }}}
    */
  final def leftSemiflatTap[C](f: ADT => F[C])(implicit F: Applicative[F]): Sealed[F, A, ADT] =
    leftSemiflatMap(adt => F.as(f(adt), adt))

  final def biSemiflatMap[B, ADT1](fa: ADT => F[ADT1], fb: A => F[B])(implicit F: Applicative[F]): Sealed[F, B, ADT1] =
    either
      .semiflatMap(_.bitraverse(fa, fb))
      .asInstanceOf[Sealed[F, Either[ADT1, B], ADT1]]
      .rethrow

  /** Executes appropriate side effect depending on whether `A` or `ADT` has been reached, and returns unchanged `Sealed[F, A, ADT]`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax._
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[Id, Int, Response] = Id(Option(1)).valueOr(NotFound)
    * scala> (for { x <- sealedSome.biSemiflatTap(_ => Id(println("left")), _ => Id(println("right"))) } yield Value(x)).run
    * // prints 'right'
    * res0: cats.Id[Response] = Value(1)
    * scala> val sealedNone: Sealed[Id, Int, Response] = Id(Option.empty).valueOr(NotFound)
    * scala> (for { x <- sealedNone.biSemiflatTap(_ => Id(println("left")), _ => Id(println("right"))) } yield Value(x)).run
    * res1: cats.Id[Response] = NotFound
    * // prints 'left'
    * }}}
    */
  final def biSemiflatTap[B, C](fa: ADT => F[C], fb: A => F[B])(implicit F: Applicative[F]): Sealed[F, A, ADT] =
    biSemiflatMap[A, ADT](adt => F.as(fa(adt), adt), a => F.as(fb(a), a))

  /** Finishes the computation by returning Sealed with given ADT.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax._
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> case class Transformed(i: Int) extends Response
    * scala> val sealedSome: Sealed[Id, Int, Response] = Id(Option(1)).valueOr(NotFound)
    * scala> (for { x <- sealedSome.complete(_ => Transformed(2)) } yield Value(x)).run
    * res0: cats.Id[Response] = Transformed(2)
    * }}}
    */
  final def complete[ADT1 >: ADT](f: A => ADT1): Sealed[F, Nothing, ADT1] = flatMap(a => Sealed.Result(f(a)))

  /** Effectful version of `complete`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax._
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> case class Transformed(i: Int) extends Response
    * scala> val sealedSome: Sealed[Id, Int, Response] = Id(Option(1)).valueOr(NotFound)
    * scala> (for { x <- sealedSome.completeWith(_ => Id(Transformed(2))) } yield Value(x)).run
    * res0: cats.Id[Response] = Transformed(2)
    * }}}
    */
  final def completeWith[ADT1 >: ADT](f: A => F[ADT1]): Sealed[F, Nothing, ADT1] = flatMap(a => Sealed.ResultF(Eval.later(f(a))))

  /** Converts `Sealed[F, Either[ADT1, B], ADT]` into `Sealed[F, B, ADT1]`. Usually paired with `either`. See `Sealed#either` for example
    * usage.
    */
  final def rethrow[B, ADT1 >: ADT](implicit ev: A <:< Either[ADT1, B]): Sealed[F, B, ADT1] =
    flatMap(a => ev(a).fold(Sealed.Result(_), Sealed.Value(_)))

  /** Converts `A` into `Either[ADT1, B]` and creates a Sealed instance from the result.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax._
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> case class UnwantedNumber(i: Int) extends Response
    * scala> val sealedSome: Sealed[Id, Int, Response] = Id(Option(1)).valueOr(NotFound)
    * scala> (for { x <- sealedSome.attempt(num => Either.cond(num == 1, Value(num), UnwantedNumber(num))) } yield x).run
    * res0: cats.Id[Response] = Value(1)
    * scala> (for { x <- sealedSome.map(_ => 2).attempt(num => Either.cond(num == 1, Value(num), UnwantedNumber(num))) } yield x).run
    * res1: cats.Id[Response] = UnwantedNumber(2)
    * scala> val sealedNone: Sealed[Id, Int, Response] = Id(Option.empty).valueOr(NotFound)
    * scala> (for { x <- sealedNone.attempt(num => Either.cond(num == 1, Value(num), UnwantedNumber(num))) } yield x).run
    * res2: cats.Id[Response] = NotFound
    * }}}
    */
  final def attempt[B, ADT1 >: ADT](f: A => Either[ADT1, B]): Sealed[F, B, ADT1] = map(f).rethrow

  /** Effectful version of `attempt`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax._
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> case class UnwantedNumber(i: Int) extends Response
    * scala> val sealedSome: Sealed[Id, Int, Response] = Id(Option(1)).valueOr(NotFound)
    * scala> (for { x <- sealedSome.attemptF(num => Id(Either.cond(num == 1, Value(num), UnwantedNumber(num)))) } yield x).run
    * res0: cats.Id[Response] = Value(1)
    * scala> (for { x <- sealedSome.map(_ => 2).attemptF(num => Id(Either.cond(num == 1, Value(num), UnwantedNumber(num)))) } yield x).run
    * res1: cats.Id[Response] = UnwantedNumber(2)
    * scala> val sealedNone: Sealed[Id, Int, Response] = Id(Option.empty).valueOr(NotFound)
    * scala> (for { x <- sealedNone.attemptF(num => Id(Either.cond(num == 1, Value(num), UnwantedNumber(num)))) } yield x).run
    * res2: cats.Id[Response] = NotFound
    * }}}
    */
  final def attemptF[B, ADT1 >: ADT](f: A => F[Either[ADT1, B]]): Sealed[F, B, ADT1] = semiflatMap(f).rethrow

  /** Applies `left` if this Sealed already reached `ADT`, or `right` if `A` is present.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax._
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> case object Reached extends Response
    * scala> val sealedSome: Sealed[Id, Int, Response] = Id(Option(1)).valueOr(NotFound)
    * scala> (for { x <- sealedSome.foldM((adt: Response) => Reached.seal, num => Value(num).seal ) } yield x).run
    * res0: cats.Id[Response] = Value(1)
    * scala> val sealedNone: Sealed[Id, Int, Response] = Id(Option.empty).valueOr(NotFound)
    * scala> (for { x <- sealedNone.foldM((adt: Response) => Reached.seal, num => Value(num).seal ) } yield x).run
    * res1: cats.Id[Response] = Reached
    * }}}
    */
  final def foldM[B, ADT1 >: ADT](left: ADT1 => Sealed[F, B, ADT1], right: A => Sealed[F, B, ADT1]): Sealed[F, B, ADT1] =
    Sealed.Fold(this, left, right)

  /** Converts `A` into `Either[ADT, A]`. Usually paired with `rethrow`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax._
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> case class Transformed(i: Int) extends Response
    * scala> val sealedSome: Sealed[Id, Int, Response] = Id(Option(1)).valueOr(NotFound)
    * scala> (for { x <- sealedSome.either.map(_.fold(adt => Left(Transformed(2)), number => Right(42))).rethrow } yield Value(x)).run
    * val res0: cats.Id[Response] = Value(42)
    * scala> val sealedNone: Sealed[Id, Int, Response] = Id(Option.empty).valueOr(NotFound)
    * scala> (for { x <- sealedNone.either.map(_.fold(adt => Left(Transformed(2)), number => Right(42))).rethrow } yield Value(x)).run
    * val res1: cats.Id[Response] = Transformed(2)
    * }}}
    */
  final def either: Sealed[F, Either[ADT, A], ADT] = foldM((adt: ADT) => Sealed.Value(Left(adt)), a => Sealed.Value(Right(a)))

  /** Executes a fire-and-forget side effect and returns unchanged `Sealed[F, A, ADT]`. Works irrespectively of Sealed's current state, in
    * contrary to `tap`. Useful for logging purposes.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax._
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[Id, Int, Response] = Id(Option(1)).valueOr(NotFound)
    * scala> (for { x <- sealedSome.inspect(either => either.fold(adt => println(adt), number => println(number))) } yield Value(x)).run
    * val res0: cats.Id[Response] = Value(1)
    * // prints '1'
    * scala> val sealedNone: Sealed[Id, Int, Response] = Id(Option.empty).valueOr(NotFound)
    * scala> (for { x <- sealedNone.inspect(either => either.fold(adt => println(adt), number => println(number))) } yield Value(x)).run
    * val res1: cats.Id[Response] = NotFound
    * // prints 'NotFound'
    * }}}
    */
  final def inspect(pf: PartialFunction[Either[ADT, A], Any]): Sealed[F, A, ADT] =
    either.map(e => pf.andThen(const(e)(_)).applyOrElse(e, const(e))).rethrow

  /** Variation of `ensure` that allows you to access `A` in `orElse` parameter. Returns unchanged Sealed instance if condition is met,
    * otherwise ends execution with specified `ADT`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax._
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> case class UnwantedNumber(i: Int) extends Response
    * scala> val sealedSome: Sealed[Id, Int, Response] = Id(Option(1)).valueOr(NotFound)
    * scala> (for { x <- sealedSome.ensureOr(num => num == 2, num => UnwantedNumber(num)) } yield Value(x)).run
    * res0: cats.Id[Response] = UnwantedNumber(1)
    * }}}
    */
  final def ensureOr[ADT1 >: ADT](pred: A => Boolean, orElse: A => ADT1): Sealed[F, A, ADT1] =
    attempt(a => Either.cond(pred(a), a, orElse(a)))

  /** Returns unchanged Sealed instance if condition is met, otherwise ends execution with specified `ADT`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax._
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> case object ConditionNotMet extends Response
    * scala> val sealedSome: Sealed[Id, Int, Response] = Id(Option(1)).valueOr(NotFound)
    * scala> (for { x <- sealedSome.ensure(num => num == 1, ConditionNotMet) } yield Value(x)).run
    * res0: cats.Id[Response] = Value(1)
    * }}}
    */
  final def ensure[ADT1 >: ADT](pred: A => Boolean, orElse: => ADT1): Sealed[F, A, ADT1] = ensureOr(pred, _ => orElse)

  /** Returns unchanged Sealed instance if condition is not met, otherwise ends execution with specified `ADT`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax._
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> case object ConditionNotMet extends Response
    * scala> val sealedSome: Sealed[Id, Int, Response] = Id(Option(1)).valueOr(NotFound)
    * scala> (for { x <- sealedSome.ensureNot(num => num == 1, ConditionNotMet) } yield Value(x)).run
    * res0: cats.Id[Response] = ConditionNotMet
    * }}}
    */
  final def ensureNot[ADT1 >: ADT](pred: A => Boolean, orElse: => ADT1): Sealed[F, A, ADT1] = ensure(a => !pred(a), orElse)

  /** Effectful version of `ensureOr`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax._
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> case class Transformed(i: Int) extends Response
    * scala> val sealedSome: Sealed[Id, Int, Response] = Id(Option(1)).valueOr(NotFound)
    * scala> (for { x <- sealedSome.ensureOrF(num => num == 1, _ => Id(Transformed(2))) } yield Value(x)).run
    * res0: cats.Id[Response] = Value(1)
    * scala> (for { x <- sealedSome.ensureOrF(num => num == 2, _ => Id(Transformed(2))) } yield Value(x)).run
    * res1: cats.Id[Response] = Transformed(2)
    * scala> val sealedNone: Sealed[Id, Int, Response] = Id(Option.empty).valueOr(NotFound)
    * scala> (for { x <- sealedNone.ensureOrF(num => num == 1, _ => Id(Transformed(2))) } yield Value(x)).run
    * res2: cats.Id[Response] = NotFound
    * }}}
    */

  final def ensureOrF[ADT1 >: ADT](pred: A => Boolean, orElse: A => F[ADT1]): Sealed[F, A, ADT1] =
    flatMap(a => if (pred(a)) Sealed.Value(a) else completeWith(orElse))

  /** Effectful version of `ensure`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax._
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> case class Transformed(i: Int) extends Response
    * scala> val sealedSome: Sealed[Id, Int, Response] = Id(Option(1)).valueOr(NotFound)
    * scala> (for { x <- sealedSome.ensureF(num => num == 1, Id(Transformed(2))) } yield Value(x)).run
    * res0: cats.Id[Response] = Value(1)
    * scala> (for { x <- sealedSome.ensureF(num => num == 2, Id(Transformed(2))) } yield Value(x)).run
    * res1: cats.Id[Response] = Transformed(2)
    * scala> val sealedNone: Sealed[Id, Int, Response] = Id(Option.empty).valueOr(NotFound)
    * scala> (for { x <- sealedNone.ensureF(num => num == 1, Id(Transformed(2))) } yield Value(x)).run
    * res2: cats.Id[Response] = NotFound
    * }}}
    */

  final def ensureF[ADT1 >: ADT](pred: A => Boolean, orElse: => F[ADT1]): Sealed[F, A, ADT1] =
    ensureOrF(pred, _ => orElse)

  /** Executes a side effect on value `A` if present, and returns unchanged `Sealed[F, A, ADT]`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax._
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[Id, Int, Response] = Id(Option(1)).valueOr(NotFound)
    * scala> (for { x <- sealedSome.tap(_ => println("right")) } yield Value(x)).run
    * res0: cats.Id[Response] = Value(1)
    * // prints 'right'
    * scala> val sealedNone: Sealed[Id, Int, Response] = Id(Option.empty).valueOr(NotFound)
    * scala> (for { x <- sealedNone.flatTap(_ => println("left")) } yield Value(x)).run
    * res1: cats.Id[Response] = NotFound
    * // doesn't print anything
    * }}}
    */
  final def tap[B](f: A => B): Sealed[F, A, ADT] = flatMap(a => map(f andThen const(a)))

  /** Effectful version of `tap`. Executes a side effect on value `A` if present, and returns unchanged `Sealed[F, A, ADT]`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax._
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[Id, Int, Response] = Id(Option(1)).valueOr(NotFound)
    * scala> (for { x <- sealedSome.flatTap(_ => Id(println("right"))) } yield Value(x)).run
    * res0: cats.Id[Response] = Value(1)
    * // prints 'right'
    * scala> val sealedNone: Sealed[Id, Int, Response] = Id(Option.empty).valueOr(NotFound)
    * scala> (for { x <- sealedNone.flatTap(_ => Id(println("left"))) } yield Value(x)).run
    * res1: cats.Id[Response] = NotFound
    * // doesn't print anything
    * }}}
    */
  final def flatTap[B](f: A => F[B]): Sealed[F, A, ADT] = flatMap(a => semiflatMap(f).map(_ => a))

  /** Executes a side effect on value `A` if present and given condition is met. Returns unchanged `Sealed[F, A, ADT]`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax._
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[Id, Int, Response] = Id(Option(1)).valueOr(NotFound)
    * scala> (for { x <- sealedSome.flatTapWhen(num => num == 1, _ => Id(println("right"))) } yield Value(x)).run
    * res0: cats.Id[Response] = Value(1)
    * // prints 'right'
    * scala> (for { x <- sealedSome.flatTapWhen(num => num == 2, _ => Id(println("left"))) } yield Value(x)).run
    * res1: cats.Id[Response] = Value(1)
    * // doesn't print anything
    * scala> val sealedNone: Sealed[Id, Int, Response] = Id(Option.empty).valueOr(NotFound)
    * scala> (for { x <- sealedNone.flatTapWhen(num => num == 1, _ => Id(println("left"))) } yield Value(x)).run
    * res1: cats.Id[Response] = NotFound
    * // doesn't print anything
    * }}}
    */
  final def flatTapWhen[B](cond: A => Boolean, f: A => F[B]): Sealed[F, A, ADT] =
    flatMap(a => if (cond(a)) flatTap(f) else Sealed.Value(a))

  final def run[ADT1 >: ADT](implicit coerce: A <:< ADT1, F: Monad[F]): F[ADT1] = F.fmap(F.tailRecM(this)(_.step))(_.map(coerce).merge)
}

object Sealed extends SealedInstances {

  private[sealedmonad] final case class FlatMap[F[_], A0, A, ADT](current: Sealed[F, A0, ADT], cont: A0 => Sealed[F, A, ADT])
      extends Sealed[F, A, ADT] {
    def runCont[B](f: B => Sealed[F, A0, ADT]) = f andThen (_.flatMap(cont))

    override def step[A1 >: A, ADT1 >: ADT](implicit F: Applicative[F]) =
      current match {
        case Value(a)         => F.pure(Left(cont(a)))
        case Effect(fa)       => F.fmap(fa.value)(a0 => Left(cont(a0)))
        case FlatMap(prev, g) => F.pure(Left(prev.flatMap(runCont(g))))
        case Fold(prev, l, r) => F.pure(Left(Fold(prev, runCont(l.asInstanceOf[ADT => Sealed[F, A0, ADT]]), runCont(r))))
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

  private[sealedmonad] final case class Fold[F[_], A0, A, ADT](
      value: Sealed[F, A0, ADT],
      left: ADT => Sealed[F, A, ADT],
      right: A0 => Sealed[F, A, ADT]
  ) extends Sealed[F, A, ADT] {
    def runFold[B](f: B => Sealed[F, A0, ADT]) = f andThen (_.foldM(left, right))

    override def step[A1 >: A, ADT1 >: ADT](implicit F: Applicative[F]): F[Either[Sealed[F, A1, ADT1], Either[ADT1, A1]]] =
      value match {
        case Result(adt)      => F.pure(Left(left(adt)))
        case ResultF(fadt)    => F.fmap(fadt.value)(adt => Left(left(adt)))
        case Value(a)         => F.pure(Left(right(a)))
        case Effect(fa)       => F.fmap(fa.value)(a0 => Left(right(a0)))
        case FlatMap(prev, f) => F.pure(Left(prev.flatMap(runFold(f))))
        case Fold(v0, l0, r0) => F.pure(Left(Fold(v0, runFold(l0.asInstanceOf[ADT => Sealed[F, A0, ADT]]), runFold(r0))))
      }
  }

  import cats.syntax.either._

  def liftF[F[_], A](value: A): Sealed[F, A, Nothing]       = Value(value)
  def apply[F[_], A](value: => F[A]): Sealed[F, A, Nothing] = Effect(Eval.later(value))

  def seal[F[_], A](value: A): Sealed[F, Nothing, A] = Result(value)

  def result[F[_], ADT](value: => F[ADT]): Sealed[F, Nothing, ADT] = ResultF(Eval.later(value))

  def valueOr[F[_], A, ADT](fa: => F[Option[A]], orElse: => ADT): Sealed[F, A, ADT] = Sealed(fa).attempt(Either.fromOption(_, orElse))

  def valueOrF[F[_], A, ADT](fa: => F[Option[A]], orElse: => F[ADT]): Sealed[F, A, ADT] =
    Sealed(fa).flatMap {
      case Some(a) => liftF(a)
      case None    => result(orElse)
    }

  def handleError[F[_], A, B, ADT](fa: F[Either[A, B]])(f: A => ADT): Sealed[F, B, ADT] = Sealed(fa).attempt(_.leftMap(f))

  def bimap[F[_], A, B, C, ADT](fa: F[Either[A, B]])(f: A => ADT)(fb: B => C): Sealed[F, C, ADT] =
    Sealed(fa).attempt(_.leftMap(f).map(fb))

}

private final class SealedMonad[F[_], ADT] extends StackSafeMonad[Sealed[F, *, ADT]] {
  override def pure[A](x: A)                                                   = Sealed.liftF(x)
  override def flatMap[A, B](fa: Sealed[F, A, ADT])(f: A => Sealed[F, B, ADT]) = fa.flatMap(f)
  override def map[A, B](fa: Sealed[F, A, ADT])(f: A => B)                     = fa.map(f)
  override def widen[A, B >: A](fa: Sealed[F, A, ADT])                         = fa
}

trait SealedInstances {
  implicit def sealedMonad[F[_], ADT]: Monad[Sealed[F, *, ADT]] = new SealedMonad
}
