package pl.iterators.sealedmonad

import cats._
import cats.syntax.all._

import scala.Function.const

sealed trait Sealed[F[_], +A, +ADT] {
  import Sealed._
  def map[B](f: A => B): Sealed[F, B, ADT] = Transform(this, f.andThen(left[F, B, ADT]), right[F, B, ADT])
  def flatMap[B, ADT1 >: ADT](f: A => Sealed[F, B, ADT1]): Sealed[F, B, ADT1] = Transform(this, f, right[F, B, ADT1])

  /** Alias for map(_ => ()) */
  final def void: Sealed[F, Unit, ADT] = map(_ => ())

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
  final def semiflatMap[B](f: A => F[B]): Sealed[F, B, ADT] = Transform(this, f.andThen(leftF), right[F, B, ADT])

  final def leftSemiflatMap[ADT1 >: ADT](f: ADT => F[ADT1]): Sealed[F, A, ADT1] =
    Transform(this, left[F, A, ADT1], f.andThen(rightF))

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
    Transform(
      this,
      left[F, A, ADT],
      (adt: ADT) => Transform(leftF(f(adt)), (_: C) => right[F, A, ADT](adt), (_: Any) => right(adt))
    )

  /** Combine leftSemiflatMap and semiflatMap together.
    */
  final def biSemiflatMap[B, ADT1 >: ADT](fa: ADT => F[ADT1], fb: A => F[B]): Sealed[F, B, ADT1] =
    Transform(this, (a: A) => leftF[F, B, ADT1](fb(a)), (adt: ADT) => rightF[F, B, ADT1](fa(adt)))

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
    Transform(
      this,
      (a: A) => Transform(leftF[F, B, ADT](fb(a)), (_: B) => left[F, A, ADT](a), (adt: ADT) => right[F, A, ADT](adt)),
      (adt: ADT) => Transform(leftF[F, C, ADT](fa(adt)), (_: C) => right[F, A, ADT](adt), (adt: ADT) => right[F, A, ADT](adt))
    )

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
  final def complete[ADT1 >: ADT](f: A => ADT1): Sealed[F, Nothing, ADT1] = flatMap(a => right(f(a)))

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
  final def completeWith[ADT1 >: ADT](f: A => F[ADT1]): Sealed[F, Nothing, ADT1] = flatMap(f.andThen(rightF))

  /** Converts `Sealed[F, Either[ADT1, B], ADT]` into `Sealed[F, B, ADT1]`. Usually paired with `either`. See `Sealed#either` for example
    * usage.
    */
  final def rethrow[B, ADT1 >: ADT](implicit ev: A <:< Either[ADT1, B]): Sealed[F, B, ADT1] =
    flatMap(a => ev(a).fold(right, left))

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
  final def foldM[B, ADT1 >: ADT](left: ADT => Sealed[F, B, ADT1], right: A => Sealed[F, B, ADT1]): Sealed[F, B, ADT1] =
    Transform(this, right, left)

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
    foldM(adt => left(Either.left(adt)), a => left(Either.right(a)))

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

  /** Executes an effect F and returns unchanged `Sealed[F, A, ADT]`. Works irrespectively of Sealed's current state, in contrary to `tap`.
    * Useful for effectful logging purposes.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax._
    * scala> import cats.Eval
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[Eval, Int, Response] = Eval.later(Option(1)).valueOr(NotFound)
    * scala> (for { x <- sealedSome.inspectF(either => either.fold(adt => Eval.later(println(adt)), number => Eval.later(println(number)))) } yield Value(x)).run.value
    * val res0: Value = Value(1)
    * // prints '1'
    * scala> val sealedNone: Sealed[Eval, Int, Response] = Eval.later(Option.empty).valueOr(NotFound)
    * scala> (for { x <- sealedNone.inspectF(either => either.fold(adt => Eval.later(println(adt)), number => Eval.later(println(number)))) } yield Value(x)).run.value
    * val res1: Response = NotFound
    * // prints 'NotFound'
    * }}}
    */
  final def inspectF(pf: PartialFunction[Either[ADT, A], F[Any]]): Sealed[F, A, ADT] =
    either.flatTapWhen(e => pf.isDefinedAt(e), pf).rethrow

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
    flatMap(a => if (pred(a)) left(a) else completeWith(orElse))

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
    flatMap(a => if (cond(a)) flatTap(f) else left(a))

  final def run[ADT1 >: ADT](implicit ev: A <:< ADT1, F: Monad[F]): F[ADT1] = eval(this).map(_.fold(ev, identity))
}

object Sealed extends SealedInstances {

  import cats.syntax.either._

  def apply[F[_], A](value: => F[A]): Sealed[F, A, Nothing] = defer(leftF(value))
  def liftF[F[_], A](value: A): Sealed[F, A, Nothing]       = defer(left(value))

  def seal[F[_], A](value: A): Sealed[F, Nothing, A] = defer(right(value))

  def result[F[_], ADT](value: => F[ADT]): Sealed[F, Nothing, ADT] = defer(rightF(value))

  def valueOr[F[_], A, ADT](fa: => F[Option[A]], orElse: => ADT): Sealed[F, A, ADT] = apply(fa).flatMap {
    case Some(a) => left(a)
    case None    => right(orElse)
  }

  def valueOrF[F[_], A, ADT](fa: => F[Option[A]], orElse: => F[ADT]): Sealed[F, A, ADT] =
    apply(fa).flatMap {
      case Some(a) => left(a)
      case None    => rightF(orElse)
    }

  def handleError[F[_], A, B, ADT](fa: F[Either[A, B]])(f: A => ADT): Sealed[F, B, ADT] = apply(fa).attempt(_.leftMap(f))

  def bimap[F[_], A, B, C, ADT](fa: F[Either[A, B]])(f: A => ADT)(fb: B => C): Sealed[F, C, ADT] =
    apply(fa).attempt(_.leftMap(f).map(fb))

  /** Represents either an intermediate A or a final ADT.
    */
  private final case class Pure[F[_], A, ADT](
      value: Either[A, ADT]
  ) extends Sealed[F, A, ADT]

  /** Represents an intermediate F[A] or a final F[ADT].
    */
  private final case class Suspend[F[_], A, ADT](
      fa: Either[F[A], F[ADT]]
  ) extends Sealed[F, A, ADT]

  /** Represents a deferred computation.
    */
  private final case class Defer[F[_], A, ADT](
      value: () => Sealed[F, A, ADT]
  ) extends Sealed[F, A, ADT]

  /** Represents a transformation on either intermediate A0 or final ADT0 value.
    *
    * Mind that the naming here might be a bit confusing because `left` is a transformation that is applied when we haven't reached the
    * final ADT yet, and `right` is a transformation that is applied when we have reached the final ADT.
    *
    * On the user side Sealed behaves similar to EitherT, so `left` applies to final ADT and right applies to intermediate A. See `foldM`
    * for an example.
    */
  private final case class Transform[F[_], A0, A, ADT0, ADT](
      current: Sealed[F, A0, ADT0],
      left: A0 => Sealed[F, A, ADT],
      right: ADT0 => Sealed[F, A, ADT]
  ) extends Sealed[F, A, ADT]

  private def left[F[_], A, ADT](value: A): Sealed[F, A, ADT]        = Pure(Left(value))
  private def leftF[F[_], A, ADT](value: F[A]): Sealed[F, A, ADT]    = Suspend(Left(value))
  private def right[F[_], A, ADT](value: ADT): Sealed[F, A, ADT]     = Pure(Right(value))
  private def rightF[F[_], A, ADT](value: F[ADT]): Sealed[F, A, ADT] = Suspend(Right(value))
  private def defer[F[_], A, ADT](thunk: => Sealed[F, A, ADT])       = Defer(() => thunk)

  /** Does the heavy lifting. There's a trampoline to advance only one step forward. Transform is unrolled and rewritten to avoid nested
    * functions and offer stack safety.
    */
  private def eval[F[_], A, ADT](value: Sealed[F, A, ADT])(implicit F: Monad[F]): F[Either[A, ADT]] = {
    type Intermediate = Sealed[F, A, ADT]
    type Final        = Either[A, ADT]
    def recur(value: Intermediate): F[Either[Intermediate, Final]] = value.asLeft[Final].pure[F]
    def returns(value: Final): F[Either[Intermediate, Final]]      = value.asRight[Intermediate].pure[F]
    value.tailRecM {
      case Pure(either)         => returns(either)
      case Suspend(Left(fa))    => fa.flatMap(a => returns(a.asLeft[ADT]))
      case Suspend(Right(fadt)) => fadt.flatMap(adt => returns(adt.asRight[A]))
      case Defer(value)         => recur(value())
      case Transform(current, onA, onADT) =>
        current match {
          case Pure(Left(a))                 => recur(onA(a))
          case Pure(Right(adt))              => recur(onADT(adt))
          case Suspend(Left(fa))             => fa.flatMap(a => recur(Transform(Pure(Left(a)), onA, onADT)))
          case Suspend(Right(fadt))          => fadt.flatMap(adt => recur(Transform(Pure(Right(adt)), onA, onADT)))
          case Defer(value)                  => recur(Transform(value(), onA, onADT))
          case Transform(next, onA0, onADT0) =>
            // the asInstanceOf below are for cross Scala 2/3 compatibility and can be avoided when src code would be split
            recur(
              Transform[F, Any, A, Any, ADT](
                next,
                (a0: Any) =>
                  Transform[F, Any, A, Any, ADT](
                    defer(onA0(a0)),
                    onA,
                    onADT
                  ),
                (adt0: Any) =>
                  Transform[F, Any, A, Any, ADT](
                    defer(onADT0(adt0)),
                    onA,
                    onADT
                  )
              )
            )
        }
    }
  }
}

private final class SealedMonad[F[_], ADT] extends StackSafeMonad[Sealed[F, _, ADT]] {
  override def pure[A](x: A): Sealed[F, A, ADT]                                                   = Sealed.liftF(x)
  override def flatMap[A, B](fa: Sealed[F, A, ADT])(f: A => Sealed[F, B, ADT]): Sealed[F, B, ADT] = fa.flatMap(f)
  override def map[A, B](fa: Sealed[F, A, ADT])(f: A => B): Sealed[F, B, ADT]                     = fa.map(f)
  override def widen[A, B >: A](fa: Sealed[F, A, ADT]): Sealed[F, B, ADT]                         = fa
}

trait SealedInstances {
  implicit def sealedMonad[F[_], ADT]: Monad[Sealed[F, _, ADT]] = new SealedMonad
}
