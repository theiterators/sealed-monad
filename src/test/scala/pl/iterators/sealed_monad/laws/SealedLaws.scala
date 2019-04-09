package pl.iterators.sealed_monad.laws
import cats.Monad
import cats.kernel.laws._
import pl.iterators.sealed_monad.Sealed

import scala.language.higherKinds

trait SealedLaws[F[_]] {
  implicit def M: Monad[F]

  import cats.syntax.either._
  import cats.syntax.flatMap._
  import cats.syntax.functor._

  def valueMapReduction[A, B, C](fa: F[A], f: A => B) = Sealed.value[C](fa).map(f) <-> Sealed.value[C](fa.map(f))

  def resultMapElimination[A, B](fb: F[B], f: A => B)                   = Sealed.resultF(fb).map(f) <-> Sealed.resultF(fb)
  def resultFlatMapElimination[A, B](fb: F[B], f: A => Sealed[F, A, B]) = Sealed.resultF(fb).flatMap(f) <-> Sealed.resultF(fb)

  def valueSemiflatMapReduction[A, B, C](fa: F[A], f: A => F[B]) = Sealed.value[C](fa).semiflatMap(f) <-> Sealed.value[C](fa >>= f)
  def resultSemiflatMapElimination[A, B](fb: F[B], f: A => F[B]) = Sealed.resultF(fb).semiflatMap(f) <-> Sealed.resultF(fb)

  def valueCompleteIdentity[A, B](fa: F[A], f: A => F[B])     = Sealed.value[B](fa).complete(f) <-> Sealed.resultF(fa >>= f)
  def resultCompleteElimination[A, B](fb: F[B], f: A => F[B]) = Sealed.resultF(fb).complete(f) <-> Sealed.resultF(fb)

  def rethrowRightIdentity[A, B](s: Sealed[F, A, B]) = s.map(Right(_)).rethrow <-> s
  def rethrowLeftIdentity[A](s: Sealed[F, A, A])     = s.map(Left(_): Either[A, A]).rethrow <-> s.complete(M.pure)

  def attemptRightIdentity[A, B, C](s: Sealed[F, A, C], f: A => B)         = s.attempt(a => Right(f(a))) <-> s.map(f)
  def attemptLeftIdentity[A, B](s: Sealed[F, A, B], f: A => B)             = s.attempt(a => Left(f(a)): Either[B, B]) <-> s.complete(a => M.pure(f(a)))
  def attemptFCoherence[A, B, C](s: Sealed[F, A, C], f: A => Either[C, B]) = s.attemptF(a => M.pure(f(a))) <-> s.attempt(f)

  def attemptRethrowCoherence[A, B, C](s: Sealed[F, A, C], f: A => Either[C, B]) = s.attempt(f) <-> s.map(f).rethrow

  def ensureTrueIdentity[A, B](s: Sealed[F, A, B], b: B)  = s.ensure(_ => true, b) <-> s
  def ensureFalseIdentity[A, B](s: Sealed[F, A, B], b: B) = s.ensure(_ => false, b) <-> s.complete(_ => M.pure(b))

  def ensureRethrowCoherence[A, B, C](s: Sealed[F, A, C], f: A => Boolean, c: C) =
    s.ensure(f, c) <-> s.map(a => Either.cond(f(a), a, c)).rethrow

  def ensureCoherence[A, B, C](s: Sealed[F, A, C], f: A => Boolean, c: C) = s.ensure(f, c) <-> s.ensureNot(a => !f(a), c)

  def inspectElimination[A, B, C](s: Sealed[F, A, C], f: Either[C, A] => Option[B]) = s.inspect(Function.unlift(f)) <-> s

  def valueOrIdentity[A, B](fa: F[Option[A]], b: B) =
    Sealed.valueOr(fa, b) <-> Sealed.value[B](fa).attempt(Either.fromOption(_, b))
  def mergeIdentity[A, B, C](fab: F[Either[A, B]], f: Either[A, B] => C) = Sealed.merge(fab)(f) <-> Sealed.resultF(fab.map(f))

  def handleErrorIdentity[A, B, C](fab: F[Either[A, B]], f: A => C) =
    Sealed.handleError(fab)(f) <-> Sealed.value[C](fab).attempt(_.leftMap(f))

  lazy val semiflatMapStackSafety = {
    val n = 50000

    @scala.annotation.tailrec
    def loop(s: Sealed[F, Int, Int], i: Int = 0): Sealed[F, Int, Int] =
      if (i < n) loop(s.semiflatMap(i => M.pure(i + 1)), i + 1) else s.complete(i => M.pure(i))

    val s   = Sealed.liftF[F, Int](0)
    val res = loop(s)
    res.run <-> M.pure(n)
  }

  lazy val computationMapStackSafety = {
    val n = 50000

    @scala.annotation.tailrec
    def loop(s: Sealed[F, Int, Int], i: Int = 0): Sealed[F, Int, Int] =
      if (i < n) loop(s.map(_ + 1), i + 1) else s.complete(i => M.pure(i))

    val s   = Sealed.liftF[F, Int](-1).flatMap(_ => Sealed.liftF(0))
    val res = loop(s)
    res.run <-> M.pure(n)
  }
}

object SealedLaws {

  def apply[F[_]](implicit ev: Monad[F]) = new SealedLaws[F] {
    override implicit val M: Monad[F] = ev
  }

}
