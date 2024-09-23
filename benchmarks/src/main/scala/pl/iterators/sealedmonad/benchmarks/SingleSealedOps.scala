package pl.iterators.sealedmonad.benchmarks

import java.util.concurrent.TimeUnit

import cats.Monad
import cats.data.EitherT
import cats.instances.TryInstances
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import pl.iterators.sealedmonad.Sealed

import scala.util._

object SingleSealedOps extends TryInstances {
  sealed trait ADT
  case object Negative  extends ADT
  case object Positive  extends ADT
  case object Zero      extends ADT
  case object TooSmall  extends ADT
  case object TooBig    extends ADT
  case class Ok(i: Int) extends ADT
}

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class SingleSealedOps {
  import SingleSealedOps._

  @Param(Array("0", "2", "4", "8", "16", "32", "64", "128"))
  var tokens: Int = _

  var x: Int = _

  @Setup(Level.Iteration)
  def setup(): Unit = x = Random.nextInt()

  private val completeWork = (i: Int) => { Blackhole.consumeCPU(tokens); Success(if (i > 0) Positive else if (i < 0) Negative else Zero) }

  @Benchmark
  def completeBenchmark =
    Sealed.liftF(x).complete(completeWork).run

  @Benchmark
  def completeCatsBaseline = Monad[Try].flatMap(Monad[Try].pure(x))(completeWork)

  private val ensureWork = (i: Int) => Ok { Blackhole.consumeCPU(tokens); i }

  @Benchmark
  def ensureBenchmark =
    Sealed.liftF(x).ensureOr(_.abs < 100, orElse = i => if (i > 100) TooBig else TooSmall).map(ensureWork).run

  @Benchmark
  def ensureCatsBaseline = EitherT.rightT[Try, ADT](x).ensureOr(i => if (i > 100) TooBig else TooSmall)(_.abs < 100).map(ensureWork).merge

  private val attemptFWork = (i: Int) => {
    Blackhole.consumeCPU(tokens); Try(if (i > 100) Left(TooBig) else if (i < 100) Left(TooSmall) else Right(i))
  }

  @Benchmark
  def attemptFBenchmark = Sealed.liftF(x).attemptF(attemptFWork).map(Ok.apply).run

  @Benchmark
  def attemptFCatsBaseline = EitherT.rightT[Try, ADT](x).flatMapF(attemptFWork).map(Ok.apply).value
}
