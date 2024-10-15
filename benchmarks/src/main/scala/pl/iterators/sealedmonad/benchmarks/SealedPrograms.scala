package pl.iterators.sealedmonad.benchmarks

import java.util.concurrent.TimeUnit

import cats.Monad
import cats.data.{EitherT, OptionT}
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import scala.util.{Random, Try}

object SealedPrograms {
  import cats.instances.try_.*

  type M[A] = Try[A]
  implicit val M: Monad[Try] = Monad[Try]

  sealed abstract class ADT
  case object SomeCase           extends ADT
  case object SomeOtherCase      extends ADT
  case object SomeYetAnotherCase extends ADT
  case class Result(i: Int)      extends ADT
}

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class SealedPrograms {

  import SealedPrograms.*
  import pl.iterators.sealedmonad.syntax.*

  private val tokens = 1024

  var x: Int    = _
  var s: String = _

  @Setup(Level.Iteration)
  def setup(): Unit = {
    x = Random.nextInt()
    s = Random.nextString(100)
  }

  private def returnOption: M[Option[Int]]               = M.pure { Blackhole.consumeCPU(tokens); Some(x) }
  private def doSomeWork: M[Option[String]]              = M.pure { Blackhole.consumeCPU(tokens); Some(s) }
  private def doSomeOtherWork(i: Int): M[Option[String]] = M.pure { Blackhole.consumeCPU(tokens); Some(i.toString) }

  @Benchmark
  def baseline1 = returnOption.flatMap {
    case None                        => M.pure(SomeCase)
    case Some(number) if number == 0 => M.pure(SomeOtherCase)
    case Some(_) =>
      val actionT = OptionT(doSomeWork).map(_.last == 'a')
      actionT.value flatMap {
        case Some(true)  => M.pure(SomeCase)
        case Some(false) => M.pure(SomeOtherCase)
        case None        => M.pure(SomeYetAnotherCase)
      }
  }

  @Benchmark
  def benchmark1 = {
    val s = for {
      _           <- returnOption.valueOr(SomeCase).ensure(_ != 0, SomeOtherCase)
      actionValue <- doSomeWork.valueOr(SomeYetAnotherCase)
    } yield if (actionValue.last == 'a') SomeCase else SomeOtherCase

    s.run
  }

  @Benchmark
  def baseline2 = {
    val userT = for {
      m <- EitherT.fromOptionF(returnOption, ifNone = SomeCase)
      s <- EitherT.fromOptionF(doSomeOtherWork(m), ifNone = SomeOtherCase: ADT)
    } yield (m, s)

    userT.semiflatMap { case (m, s) =>
      M.pure(m + s.toInt).map(_ => Result(m))
    }.merge
  }

  @Benchmark
  def benchmark2 = {
    val s = for {
      m <- returnOption.valueOr(SomeCase)
      _ <- doSomeOtherWork(m).valueOr(SomeOtherCase) >>! (s => M.pure(m + s.toInt))
    } yield Result(m)

    s.run
  }
}
