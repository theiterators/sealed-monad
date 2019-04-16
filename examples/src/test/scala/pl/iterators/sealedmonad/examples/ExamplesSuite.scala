package pl.iterators.sealedmonad.examples

import cats.Eq
import cats.tests.CatsSuite
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.{ExecutionContext, Future}

trait ExamplesSuite extends CatsSuite with ScalaFutures {
  implicit final val executionContext: ExecutionContext      = ExecutionContext.global
  override implicit final val patienceConfig: PatienceConfig = super.patienceConfig.copy(timeout = scaled(Span(4, Seconds)))

  implicit private val throwableEq: Eq[Throwable] = Eq.fromUniversalEquals

  private val arbitraryNonFatalThrowable: Arbitrary[Throwable] = Arbitrary(Arbitrary.arbitrary[Exception].map(identity))
  implicit protected def arbNonFatalFuture[T: Arbitrary]: Arbitrary[Future[T]] =
    Arbitrary(Gen.oneOf(Arbitrary.arbitrary[T].map(Future.successful), arbitraryNonFatalThrowable.arbitrary.map(t => Future.failed(t))))

  private def futureEither[A](fut: Future[A]): Future[Either[Throwable, A]] =
    fut.map(Right(_)).recover { case t => Left(t) }
  implicit protected def eqFuture[A: Eq]: Eq[Future[A]] =
    (fx: Future[A], fy: Future[A]) => {
      val fz = futureEither(fx) zip futureEither(fy)
      fz.map { case (tx, ty) => Eq[Either[Throwable, A]].eqv(tx, ty) }.futureValue
    }
}
