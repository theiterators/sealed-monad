package pl.iterators.sealedmonad

import cats._
import cats.syntax.all._

import scala.Function.const
import scala.language.higherKinds

sealed trait Sealed[F[_], +A, +ADT] {
  import Sealed._
  def map[B](f: A => B): Sealed[F, B, ADT]                                    = FlatMap(this, (a: A) => Intermediate(f(a)))
  def flatMap[B, ADT1 >: ADT](f: A => Sealed[F, B, ADT1]): Sealed[F, B, ADT1] = FlatMap(this, f)

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
  final def semiflatMap[B](f: A => F[B]): Sealed[F, B, ADT] = flatMap(a => Sealed.IntermediateF(Eval.later(f(a))))

  final def leftSemiflatMap[ADT1](f: ADT => F[ADT1]): Sealed[F, A, ADT1] =
    foldM[A, ADT]((adt: ADT) => ResultF(Eval.later(f(adt))).asInstanceOf[Sealed[F, A, ADT]], a => Intermediate(a))
      .asInstanceOf[Sealed[F, A, ADT1]]

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
  final def leftSemiflatTap[C](f: ADT => F[C]): Sealed[F, A, ADT] =
    foldM[A, ADT](
      (adt: ADT) => ResultF(Eval.later(f(adt))).flatMap(_ => Result(adt)).asInstanceOf[Sealed[F, A, ADT]],
      a => Intermediate(a)
    )

  /** Combine leftSemiflatMap and semiflatMap together.
    */
  final def biSemiflatMap[B, ADT1](fa: ADT => F[ADT1], fb: A => F[B]): Sealed[F, B, ADT1] =
    leftSemiflatMap(fa).semiflatMap(fb)

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
  final def biSemiflatTap[B, C](fa: ADT => F[C], fb: A => F[B]): Sealed[F, A, ADT] =
    leftSemiflatTap(fa).tap(fb)

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
  final def complete[ADT1 >: ADT](f: A => ADT1): Sealed[F, Nothing, ADT1] = flatMap(a => Result(f(a)))

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
    flatMap(a => ev(a).fold(Result(_), Intermediate(_)))

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
    Fold(this, right, left)

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
    * scala> (for { x <- sealedSome.either.map(_.fold(adt => Intermediate(Transformed(2)), number => Result(42))).rethrow } yield Value(x)).run
    * val res0: cats.Id[Response] = Value(42)
    * scala> val sealedNone: Sealed[Id, Int, Response] = Id(Option.empty).valueOr(NotFound)
    * scala> (for { x <- sealedNone.either.map(_.fold(adt => Intermediate(Transformed(2)), number => Result(42))).rethrow } yield Value(x)).run
    * val res1: cats.Id[Response] = Transformed(2)
    * }}}
    */
  final def either: Sealed[F, Either[ADT, A], ADT] =
    foldM((adt: ADT) => Intermediate(Either.left(adt)), a => Intermediate(Either.right(a)))

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
    flatMap(a => if (pred(a)) Sealed.Intermediate(a) else completeWith(orElse))

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
    flatMap(a => if (cond(a)) flatTap(f) else Sealed.Intermediate(a))

  private def feval[A1 >: A, ADT1 >: ADT](implicit
      F: Monad[F]
  ): Eval[F[Either[A1, ADT1]]] = this match {
    case Intermediate(value)  => Eval.later(value.asLeft[ADT1].pure[F]).asInstanceOf[Eval[F[Either[A1, ADT1]]]]
    case IntermediateF(value) => value.map(_.map(_.asLeft[ADT1]))
    case Result(value)        => Eval.later(value.asRight[A1].pure[F]).asInstanceOf[Eval[F[Either[A1, ADT1]]]]
    case ResultF(value)       => value.map(_.map(_.asRight[A1]))
    case FlatMap(current, next) =>
      current.feval
        .map { feither =>
          feither.flatMap {
            case scala.Left(value) =>
              next(value).feval[A1, ADT1].value
            case either =>
              either.pure[F].asInstanceOf[F[Either[A1, ADT1]]]
          }
        }
        .asInstanceOf[Eval[F[Either[A1, ADT1]]]]
    case Fold(current, left, right) =>
      current.feval
        .map { feither =>
          feither.flatMap {
            case scala.Left(value) =>
              left(value).feval[A1, ADT1].value
            case scala.Right(value) =>
              right(value).feval[A1, ADT1].value
          }
        }
        .asInstanceOf[Eval[F[Either[A1, ADT1]]]]
  }

  final def run[ADT1 >: ADT](implicit ev: A <:< ADT1, F: Monad[F]): F[ADT1] = feval[A, ADT].value.map(_.fold(ev, identity))
}

object Sealed extends SealedInstances {

  import cats.syntax.either._

  def apply[F[_], A](value: => F[A]): Sealed[F, A, Nothing] = IntermediateF(Eval.later(value))
  def liftF[F[_], A](value: A): Sealed[F, A, Nothing]       = Intermediate(value)

  def seal[F[_], A](value: A): Sealed[F, Nothing, A] = Result(value)

  def result[F[_], ADT](value: => F[ADT]): Sealed[F, Nothing, ADT] = ResultF(Eval.later(value))

  def valueOr[F[_], A, ADT](fa: => F[Option[A]], orElse: => ADT): Sealed[F, A, ADT] = apply(fa).flatMap {
    case Some(a) => Intermediate(a)
    case None    => Result(orElse)
  }

  def valueOrF[F[_], A, ADT](fa: => F[Option[A]], orElse: => F[ADT]): Sealed[F, A, ADT] =
    apply(fa).flatMap {
      case Some(a) => liftF(a)
      case None    => result(orElse)
    }

  def handleError[F[_], A, B, ADT](fa: F[Either[A, B]])(f: A => ADT): Sealed[F, B, ADT] = apply(fa).attempt(_.leftMap(f))

  def bimap[F[_], A, B, C, ADT](fa: F[Either[A, B]])(f: A => ADT)(fb: B => C): Sealed[F, C, ADT] =
    apply(fa).attempt(_.leftMap(f).map(fb))

  private final case class Intermediate[F[_], A](value: A) extends Sealed[F, A, Nothing]

  private final case class IntermediateF[F[_], A](value: Eval[F[A]]) extends Sealed[F, A, Nothing]

  private final case class Result[F[_], ADT](value: ADT) extends Sealed[F, Nothing, ADT]

  private final case class ResultF[F[_], ADT](value: Eval[F[ADT]]) extends Sealed[F, Nothing, ADT]

  private final case class FlatMap[F[_], A0, A, ADT](
      current: Sealed[F, A0, ADT],
      next: A0 => Sealed[F, A, ADT]
  ) extends Sealed[F, A, ADT]

  private final case class Fold[F[_], A0, A, ADT](
      current: Sealed[F, A0, ADT],
      left: A0 => Sealed[F, A, ADT],
      right: ADT => Sealed[F, A, ADT]
  ) extends Sealed[F, A, ADT]
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
