package pl.iterators.sealedmonad.laws

import cats.Monad
import cats.kernel.laws._
import pl.iterators.sealedmonad.Sealed
import pl.iterators.sealedmonad.SealedTestInstances.ADT

import scala.language.higherKinds

trait SealedLaws[F[_]] {
  implicit def M: Monad[F]

  import cats.syntax.either._
  import cats.syntax.flatMap._
  import cats.syntax.functor._

  def valueMapReduction[A, B](fa: F[A], f: A => B) = Sealed(fa).map(f) <-> Sealed(fa.map(f))

  def resultMapElimination[A, B](fb: F[B], f: A => B)                   = Sealed.result(fb).map(f) <-> Sealed.result(fb)
  def resultFlatMapElimination[A, B](fb: F[B], f: A => Sealed[F, A, B]) = Sealed.result(fb).flatMap(f) <-> Sealed.result(fb)

  def valueSemiflatMapReduction[A, B](fa: F[A], f: A => F[B])    = Sealed(fa).semiflatMap(f) <-> Sealed(fa >>= f)
  def resultSemiflatMapElimination[A, B](fb: F[B], f: A => F[B]) = Sealed.result(fb).semiflatMap(f) <-> Sealed.result(fb)

  def valueLeftSemiflatMapElimination[A, B](fa: F[A], fab: A => F[B]) =
    Sealed(fa).leftSemiflatMap(fab) <-> Sealed(fa)

  def resultBiSemiflatMapCoherence[A, B, C](fa: F[A], fab: A => F[B], fcd: A => F[C]) =
    Sealed.apply(fa).biSemiflatMap(fab, fcd) <-> Sealed.apply(fa).leftSemiflatMap(fab).semiflatMap(fcd)

  def valueBiSemiflatMapCoherence[A, B, C](fa: F[A], fab: A => F[B], fcd: A => F[C]) =
    Sealed(fa).biSemiflatMap(fab, fcd) <-> Sealed(fa).leftSemiflatMap(fab).semiflatMap(fcd)

  def valueCompleteIdentity[A, B](fa: F[A], f: A => F[B])  = Sealed(fa).completeWith(f) <-> Sealed.result(fa >>= f)
  def resultCompleteElimination[A, B](fb: F[B], f: A => B) = Sealed.result(fb).complete(f) <-> Sealed.result(fb)

  def completeWithCoherence[A, B](s: Sealed[F, Nothing, B], f: A => B) = s.completeWith(a => M.pure(f(a))) <-> s.complete(f)

  def rethrowRightIdentity[A, B](s: Sealed[F, A, B]) = s.map(Right(_)).rethrow <-> s
  def rethrowLeftIdentity[A](s: Sealed[F, A, A])     = s.map(Left(_): Either[A, A]).rethrow <-> s.complete(identity)

  def attemptRightIdentity[A, B, C](s: Sealed[F, A, C], f: A => B)         = s.attempt(a => Right(f(a))) <-> s.map(f)
  def attemptLeftIdentity[A, B](s: Sealed[F, A, B], f: A => B)             = s.attempt(a => Left(f(a)): Either[B, B]) <-> s.complete(f)
  def attemptFCoherence[A, B, C](s: Sealed[F, A, C], f: A => Either[C, B]) = s.attemptF(a => M.pure(f(a))) <-> s.attempt(f)
  def attemptRethrowCoherence[A, B, C](s: Sealed[F, A, C], f: A => Either[C, B]) = s.attempt(f) <-> s.map(f).rethrow

  def eitherIdentity[A, B](s: Sealed[F, A, B]) = s.either.rethrow <-> s

  def ensureTrueIdentity[A, B](s: Sealed[F, A, B], b: B)  = s.ensure(_ => true, b) <-> s
  def ensureFalseIdentity[A, B](s: Sealed[F, A, B], b: B) = s.ensure(_ => false, b) <-> s.complete(_ => b)

  def ensureFTrueIdentity[A, B](s: Sealed[F, A, B], b: F[B])  = s.ensureF(_ => true, b) <-> s
  def ensureFFalseIdentity[A, B](s: Sealed[F, A, B], b: F[B]) = s.ensureF(_ => false, b) <-> s.completeWith(_ => b)

  def ensureOrFTrueIdentity[A, B](s: Sealed[F, A, B], b: A => F[B])  = s.ensureOrF(_ => true, b) <-> s
  def ensureOrFFalseIdentity[A, B](s: Sealed[F, A, B], b: A => F[B]) = s.ensureOrF(_ => false, b) <-> s.completeWith(b)

  def foldMCoherentWithFlatMap[A, B](fa: F[Option[A]], b: B) =
    Sealed(fa).attempt(Either.fromOption(_, b)).foldM[Int, B](_ => Sealed.liftF(0), _ => Sealed.liftF(1)) <-> Sealed(fa).flatMap {
      case None => Sealed.liftF(0)
      case _    => Sealed.liftF(1)
    }

  def ensureRethrowCoherence[A, B](s: Sealed[F, A, B], f: A => Boolean, b: B) =
    s.ensure(f, b) <-> s.map(a => Either.cond(f(a), a, b)).rethrow

  def ensureCoherence[A, B](s: Sealed[F, A, B], f: A => Boolean, b: B) = s.ensure(f, b) <-> s.ensureNot(a => !f(a), b)

  def ensureFAttemptFCoherence[A, B](s: Sealed[F, A, B], f: A => Boolean, b: F[B]) =
    s.ensureF(f, b) <-> s.attemptF(a => if (f(a)) M.pure(Right(a): Either[B, A]) else b.map(x => Left(x): Either[B, A]))

  def ensureFEnsureCoherence[A, B](s: Sealed[F, A, B], f: A => Boolean, b: B) =
    s.ensureF(f, M.pure(b)) <-> s.ensure(f, b)

  def ensureOrFEnsureFCoherence[A, B](s: Sealed[F, A, B], f: A => Boolean, b: F[B]) =
    s.ensureOrF(f, _ => b) <-> s.ensureF(f, b)

  def inspectElimination[A, B, C](s: Sealed[F, A, C], f: Either[C, A] => Option[B]) = s.inspect(Function.unlift(f)) <-> s

  def valueOrIdentity[A, B](fa: F[Option[A]], b: B) =
    Sealed.valueOr(fa, b) <-> Sealed(fa).attempt(Either.fromOption(_, b))

  def handleErrorIdentity[A, B, C](fab: F[Either[A, B]], f: A => C) =
    Sealed.handleError(fab)(f) <-> Sealed(fab).attempt(_.leftMap(f))

  def bimapIdentity[A, B, C, D](fab: F[Either[A, B]], f: A => C, fb: B => D) =
    Sealed.bimap(fab)(f)(fb) <-> Sealed(fab).attempt(_.leftMap(f).map(fb))

  lazy val semiflatMapStackSafety = {
    val n = 50000

    @scala.annotation.tailrec
    def loop(s: Sealed[F, Int, Int], i: Int = 0): Sealed[F, Int, Int] =
      if (i < n) loop(s.semiflatMap(i => M.pure(i + 1)), i + 1) else s.completeWith(i => M.pure(i))

    val s   = Sealed.liftF[F, Int](0)
    val res = loop(s)
    res.run <-> M.pure(n)
  }

  lazy val computationMapStackSafety = {
    val n = 50000

    @scala.annotation.tailrec
    def loop(s: Sealed[F, Int, Int], i: Int = 0): Sealed[F, Int, Int] =
      if (i < n) loop(s.map(_ + 1), i + 1) else s.completeWith(i => M.pure(i))

    val s   = Sealed.liftF[F, Int](-1).flatMap(_ => Sealed.liftF(0))
    val res = loop(s)
    res.run <-> M.pure(n)
  }
}

object SealedLaws {

  def apply[F[_]](implicit ev: Monad[F]) =
    new SealedLaws[F] {
      override implicit val M: Monad[F] = ev
    }

}
