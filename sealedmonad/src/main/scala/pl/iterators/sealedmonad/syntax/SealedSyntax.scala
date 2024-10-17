package pl.iterators.sealedmonad.syntax

import pl.iterators.sealedmonad.Sealed

import scala.language.implicitConversions

trait SealedSyntax {
  implicit final def faSyntax[F[_], A](fa: F[A]): SealedFAOps[F, A]                              = new SealedFAOps(fa)
  implicit final def faOps[A](a: A): SealedOps[A]                                                = new SealedOps(a)
  implicit final def faOptSyntax[F[_], A](fa: F[Option[A]]): SealedFOptAOps[F, A]                = new SealedFOptAOps(fa)
  implicit final def faEitherSyntax[F[_], A, B](fa: F[Either[A, B]]): SealedFAEitherOps[F, A, B] = new SealedFAEitherOps(fa)
  implicit final def eitherSyntax[ADT, A](condEither: Either[ADT, A]): SealedEitherOps[ADT, A]   = new SealedEitherOps[ADT, A](condEither)
}

final class SealedFAOps[F[_], A](private val self: F[A]) extends AnyVal {

  /** Creates a Sealed instance from a value of type `F[A]` as intermediate value.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedFAOps
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[List, Int, Response] = List(1).seal[Response]
    * scala> (for {value <- sealedSome} yield Value(value)).run
    * res0: List[Response] = List(Value(1))
    * }}}
    */
  def seal[ADT]: Sealed[F, A, ADT] = Sealed(self)

  /** Creates Sealed instance from a value of type `F[A]` as intermediate value if condition is met, otherwise ends execution with
    * specified `ADT`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedFAOps
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[List, Int, Response] = List(1).ensure(_ == 1, NotFound)
    * scala> (for {value <- sealedSome} yield Value(value)).run
    * res0: List[Response] = List(Value(1))
    * scala> val sealedNone: Sealed[List, Int, Response] = List(2).ensure(_ == 1, NotFound)
    * scala> (for {value <- sealedNone} yield Value(value)).run
    * res1: List[Response] = List(NotFound)
    * }}}
    */
  def ensure[ADT](pred: A => Boolean, orElse: => ADT): Sealed[F, A, ADT] = seal[ADT].ensure(pred, orElse)

  /** Creates Sealed instance from a value of type `F[A]` as intermediate value if condition is met, otherwise ends execution with
    * specified F[`ADT`].
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedFAOps
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[List, Int, Response] = List(1).ensureF(_ == 1, List(NotFound))
    * scala> (for {value <- sealedSome} yield Value(value)).run
    * res0: List[Response] = List(Value(1))
    * scala> val sealedNone: Sealed[List, Int, Response] = List(2).ensureF(_ == 1, List(NotFound))
    * scala> (for {value <- sealedNone} yield Value(value)).run
    * res1: List[Response] = List(NotFound)
    * }}}
    */
  def ensureF[ADT](pred: A => Boolean, orElse: => F[ADT]): Sealed[F, A, ADT] = seal[ADT].ensureF(pred, orElse)

  /** Creates Sealed instance from a value of type `F[A]` as intermediate value if condition is not met, otherwise ends execution with
    * specified `ADT`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedFAOps
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[List, Int, Response] = List(1).ensureNot(_ == 2, NotFound)
    * scala> (for {value <- sealedSome} yield Value(value)).run
    * res0: List[Response] = List(Value(1))
    * scala> val sealedNone: Sealed[List, Int, Response] = List(2).ensureNot(_ == 2, NotFound)
    * scala> (for {value <- sealedNone} yield Value(value)).run
    * res1: List[Response] = List(NotFound)
    * }}}
    */
  def ensureNot[ADT](pred: A => Boolean, orElse: => ADT): Sealed[F, A, ADT] = seal[ADT].ensureNot(pred, orElse)

  /** Creates Sealed instance from a value of type `F[A]` as intermediate value if condition is not met, otherwise ends execution with
    * specified F[`ADT`].
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedFAOps
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[List, Int, Response] = List(1).ensureNotF(_ == 2, List(NotFound))
    * scala> (for {value <- sealedSome} yield Value(value)).run
    * res0: List[Response] = List(Value(1))
    * scala> val sealedNone: Sealed[List, Int, Response] = List(2).ensureNotF(_ == 2, List(NotFound))
    * scala> (for {value <- sealedNone} yield Value(value)).run
    * res1: List[Response] = List(NotFound)
    * }}}
    */
  def ensureNotF[ADT](pred: A => Boolean, orElse: => F[ADT]): Sealed[F, A, ADT] = seal[ADT].ensureNotF(pred, orElse)

  /** Creates Sealed instance from a value of type `F[A]` as intermediate value if condition is met, otherwise ends execution with
    * specified `ADT` produced from `A`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedFAOps
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[List, Int, Response] = List(1).ensureOr(_ == 1, x => Value(x))
    * scala> (for {value <- sealedSome} yield NotFound).run
    * res0: List[Response] = List(NotFound)
    * scala> val sealedNone: Sealed[List, Int, Response] = List(2).ensureOr(_ == 1, x => Value(x))
    * scala> (for {value <- sealedNone} yield NotFound).run
    * res1: List[Response] = List(Value(2))
    * }}}
    */
  def ensureOr[ADT](pred: A => Boolean, orElse: A => ADT): Sealed[F, A, ADT] = seal[ADT].ensureOr(pred, orElse)

  /** Creates Sealed instance from a value of type `F[A]` as intermediate value if condition is met, otherwise ends execution with
    * specified F[`ADT`] produced from `A`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedFAOps
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[List, Int, Response] = List(1).ensureOrF(_ == 1, x => List(Value(x)))
    * scala> (for {value <- sealedSome} yield NotFound).run
    * res0: List[Response] = List(NotFound)
    * scala> val sealedNone: Sealed[List, Int, Response] = List(2).ensureOrF(_ == 1, x => List(Value(x)))
    * scala> (for {value <- sealedNone} yield NotFound).run
    * res1: List[Response] = List(Value(2))
    * }}}
    */
  def ensureOrF[ADT](pred: A => Boolean, orElse: A => F[ADT]): Sealed[F, A, ADT] = seal[ADT].ensureOrF(pred, orElse)

  /** Creates Sealed instance from a value of type `F[A]` as intermediate value if condition is not met, otherwise ends execution with
    * specified `ADT` produced from `A`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedFAOps
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[List, Int, Response] = List(1).ensureNotOr(_ == 2, x => Value(x))
    * scala> (for {value <- sealedSome} yield NotFound).run
    * res0: List[Response] = List(NotFound)
    * scala> val sealedNone: Sealed[List, Int, Response] = List(2).ensureNotOr(_ == 2, x => Value(x))
    * scala> (for {value <- sealedNone} yield NotFound).run
    * res1: List[Response] = List(Value(2))
    * }}}
    */
  def ensureNotOr[ADT](pred: A => Boolean, orElse: A => ADT): Sealed[F, A, ADT] = seal[ADT].ensureNotOr(pred, orElse)

  /** Creates Sealed instance from a value of type `F[A]` as intermediate value if condition is not met, otherwise ends execution with
    * specified F[`ADT`] produced from `A`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedFAOps
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[List, Int, Response] = List(1).ensureNotOrF(_ == 2, x => List(Value(x)))
    * scala> (for {value <- sealedSome} yield NotFound).run
    * res0: List[Response] = List(NotFound)
    * scala> val sealedNone: Sealed[List, Int, Response] = List(2).ensureNotOrF(_ == 2, x => List(Value(x)))
    * scala> (for {value <- sealedNone} yield NotFound).run
    * res1: List[Response] = List(Value(2))
    * }}}
    */
  def ensureNotOrF[ADT](pred: A => Boolean, orElse: A => F[ADT]): Sealed[F, A, ADT] =
    seal[ADT].ensureNotOrF(pred, orElse)

  /** Converts `A` into `Either[ADT1, B]` and creates a Sealed instance from the result.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedFAOps
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[List, Int, Response] = List(1).attempt { case i if i == 1 => Right(i); case _ => Left(NotFound) }
    * scala> (for {value <- sealedSome} yield Value(value)).run
    * res0: List[Response] = List(Value(1))
    * scala> val sealedNone: Sealed[List, Int, Response] = List(2).attempt { case i if i == 1 => Right(i); case _ => Left(NotFound) }
    * scala> (for {value <- sealedNone} yield Value(value)).run
    * res1: List[Response] = List(NotFound)
    * }}}
    */
  def attempt[B, ADT](f: A => Either[ADT, B]): Sealed[F, B, ADT] = seal[ADT].attempt(f)

  /** Converts `A` into `F[Either[ADT1, B]]` and creates a Sealed instance from the result.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedFAOps
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[List, Int, Response] = List(1).attemptF(i => List(Right(i)))
    * scala> (for {value <- sealedSome} yield Value(value)).run
    * res0: List[Response] = List(Value(1))
    * scala> val sealedNone: Sealed[List, Int, Response] = List(2).attemptF(i => List(Left(NotFound)))
    * scala> (for {value <- sealedNone} yield Value(value)).run
    * res1: List[Response] = List(NotFound)
    * }}}
    */
  def attemptF[B, ADT](f: A => F[Either[ADT, B]]): Sealed[F, B, ADT] = seal[ADT].attemptF(f)
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
    * res1: List[Response] = List(NotFound)
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
    * res1: List[Response] = List(NotFound)
    * }}}
    */
  def valueOrF[ADT](orElse: => F[ADT]): Sealed[F, A, ADT] = Sealed.valueOrF(self, orElse)

  /** Creates a Sealed instance from `F[Option[A]]` with Unit as intermediate value if not present, or `ADT` based on `A` as final value
    * otherwise.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedFOptAOps
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[List, Unit, Response] = List(Option(1)).emptyOr(Value(_))
    * scala> (for {value <- sealedSome} yield NotFound).run
    * res0: List[Response] = List(Value(1))
    * scala> val sealedNone: Sealed[List, Unit, Response] = List(Option.empty[Int]).emptyOr(Value(_))
    * scala> (for {value <- sealedNone} yield NotFound).run
    * res1: List[Response] = List(NotFound)
    * }}}
    */
  def emptyOr[ADT](orElse: A => ADT): Sealed[F, Unit, ADT] = Sealed.emptyOr(self, orElse)

  /** Creates a Sealed instance from `F[Option[A]]` with Unit as intermediate value if not present, or `F[ADT]` based on `A` as final value
    * otherwise.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedFOptAOps
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedSome: Sealed[List, Unit, Response] = List(Option(1)).emptyOrF(i => List(Value(i)))
    * scala> (for {value <- sealedSome} yield NotFound).run
    * res0: List[Response] = List(Value(1))
    * scala> val sealedNone: Sealed[List, Unit, Response] = List(Option.empty[Int]).emptyOrF(i => List(Value(i)))
    * scala> (for {value <- sealedNone} yield NotFound).run
    * res1: List[Response] = List(NotFound)
    * }}}
    */
  def emptyOrF[ADT](orElse: A => F[ADT]): Sealed[F, Unit, ADT] = Sealed.emptyOrF(self, orElse)
}

final class SealedFAEitherOps[F[_], A, B](private val self: F[Either[A, B]]) extends AnyVal {

  /** Transforms `F[Either[A, B]]` into `Sealed[F, ADT, ADT]` using `f`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedFAEitherOps
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> List(Right(1): Either[String, Int]).merge[Response] { case Right(i) => Value(i); case Left(_) => NotFound }.run
    * res0: List[Response] = List(Value(1))
    * scala> List(Left("it"): Either[String, Int]).merge[Response] { case Right(i) => Value(i); case Left(_) => NotFound }.run
    * res1: List[Response] = List(NotFound)
    * }}}
    */
  def merge[ADT](f: Either[A, B] => ADT): Sealed[F, ADT, ADT] = Sealed(self).complete(f)

  /** Transforms `F[Either[A, B]]` into `Sealed[F, ADT, ADT]` using effectful `f`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedFAEitherOps
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> List(Right(1): Either[String, Int]).mergeF[Response] { case Right(i) => List(Value(i)); case Left(_) => List(NotFound) }.run
    * res0: List[Response] = List(Value(1))
    * scala> List(Left("it"): Either[String, Int]).mergeF[Response] { case Right(i) => List(Value(i)); case Left(_) => List(NotFound) }.run
    * res1: List[Response] = List(NotFound)
    * }}}
    */
  def mergeF[ADT](f: Either[A, B] => F[ADT]): Sealed[F, ADT, ADT] = Sealed(self).completeWith(f)

  /** Transforms `F[Either[A, B]]` into `Sealed[F, B, ADT]` by transforming `A` into `ADT` using `f`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedFAEitherOps
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedRight: Sealed[List, Int, Response] = List(Right(1): Either[String, Int]).handleError(_ => NotFound)
    * scala> (for {value <- sealedRight} yield Value(value)).run
    * res0: List[Response] = List(Value(1))
    * scala> val sealedLeft: Sealed[List, String, Response] = List(Left(1): Either[Int, String]).handleError(Value(_))
    * scala> (for {value <- sealedLeft} yield NotFound).run
    * res1: List[Response] = List(Value(1))
    * }}}
    */
  def handleError[ADT](f: A => ADT): Sealed[F, B, ADT] = Sealed.handleError(self)(f)

  /** Returns a Sealed instance containing value `A` if it is Left in Either, else returns a Sealed instance containing value `B` if it is
    * Right in Either.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedFAEitherOps
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> case class UnwantedNumber(i: Int) extends Response
    * scala> val sealedRight: Sealed[Id, Int, Response] = Id(Right(1): Either[Response, Int]).fromEither
    * scala> (for {value <- sealedRight} yield Value(value)).run
    * res0: cats.Id[Response] = Value(1)
    * scala> val sealedLeft: Sealed[Id, Int, Response] = Id(Left(NotFound): Either[Response, Int]).fromEither
    * scala> (for {value <- sealedLeft} yield Value(value)).run
    * res1: cats.Id[Response] = NotFound
    * }}}
    */
  def fromEither: Sealed[F, B, A] = Sealed(self).rethrow
}

final class SealedOps[A](private val self: A) extends AnyVal {

  /** Creates a Sealed instance containing intermediate value `A`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.syntax.SealedOps
    * scala> import cats.Id
    * scala> 1.seal[Id].run
    * res0: Int = 1
    * }}}
    */
  def seal[F[_]]: Sealed[F, Nothing, A] = Sealed.seal(self)

  /** Creates a Sealed instance containing intermediate value `A` and lifts it to a given `ADT`.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.syntax.SealedOps
    * scala> import cats.Id
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> 1.liftSealed[Id, Response].map(Value(_)).run
    * res0: Response = Value(1)
    * }}}
    */
  def liftSealed[F[_], ADT]: Sealed[F, A, ADT] = Sealed.liftF(self)
}

final class SealedEitherOps[ADT, A](private val self: Either[ADT, A]) extends AnyVal {

  /** Creates a Sealed instance containing intermediate value `A` if it is Right in Either, else short-circuits with given `ADT` value.
    *
    * Example:
    * {{{
    * scala> import pl.iterators.sealedmonad.Sealed
    * scala> import pl.iterators.sealedmonad.syntax.SealedEitherOps
    * scala> sealed trait Response
    * scala> case class Value(i: Int) extends Response
    * scala> case object NotFound extends Response
    * scala> val sealedRight: Sealed[List, Int, Response] = (Right(1): Either[Response, Int]).rethrow[List]
    * scala> (for {value <- sealedRight} yield Value(value)).run
    * res0: List[Response] = List(Value(1))
    * }}}
    */
  def rethrow[F[_]]: Sealed[F, A, ADT] = Sealed.liftF(self).rethrow
}
