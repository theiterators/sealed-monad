package pl.iterators.sealedmonad.side_effects

import cats.{Applicative, Id}
import pl.iterators.sealedmonad.syntax._
import cats.catsInstancesForId

trait SealedSideEffectsSemiflatTap {

  def notInvokeOnValueLeftSemiflatTap: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke(): Id[Unit] =
      shouldBeInvoked = true

    0.liftSealed[Id, Nothing]
      .leftSemiflatTap(_ => invoke())
      .run

    !shouldBeInvoked
  }

  def notInvokeOnValueOrOnSomeLeftSemiflatTap: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke() =
      shouldBeInvoked = true

    val m: Id[Option[Int]] = Some(1)

    m.valueOr(30)
      .leftSemiflatTap(_ => invoke())
      .run

    !shouldBeInvoked
  }

  def invokeOnValueOrOnNoneLeftSemiflatTap: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke() =
      shouldBeInvoked = true

    val m: Id[Option[Int]] = None

    m.valueOr(30)
      .leftSemiflatTap(_ => invoke())
      .run

    shouldBeInvoked
  }

  def notInvokeOnPositiveEnsureLeftSemiflatTap: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke() =
      shouldBeInvoked = true

    val m: Id[Option[Int]] = Id(Some(10))

    m.valueOr(30)
      .ensure(_ => true, 30)
      .leftSemiflatTap(_ => invoke())
      .run

    !shouldBeInvoked
  }

  def invokeOnNegativeEnsureLeftSemiflatTap: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke() =
      shouldBeInvoked = true

    0.liftSealed[Id, Nothing]
      .ensure(_ => false, 30)
      .leftSemiflatTap(_ => invoke())
      .run

    shouldBeInvoked
  }

  def notInvokeLeftFunctionOnValueBiSemiflatTap: Boolean = {
    var shouldBeInvoked: Int = 0

    def invokeLeft(): Id[Unit] =
      shouldBeInvoked = 1

    def invokeRight(): Id[Unit] =
      shouldBeInvoked = 2

    0.liftSealed[Id, Nothing]
      .biSemiflatTap(_ => invokeLeft(), _ => invokeRight())
      .run

    shouldBeInvoked != 1
  }

  def notInvokeLeftFunctionOnValueOrOnSomeBiSemiflatTap: Boolean = {
    var shouldBeInvoked: Int = 0

    def invokeLeft(): Id[Unit] =
      shouldBeInvoked = 1

    def invokeRight(): Id[Unit] =
      shouldBeInvoked = 2

    val m: Id[Option[Int]] = Some(1)

    m.valueOr(30)
      .biSemiflatTap(_ => invokeLeft(), _ => invokeRight())
      .run

    shouldBeInvoked != 1
  }

  def invokeLeftFunctionOnValueOrOnNoneBiSemiflatTap: Boolean = {
    var shouldBeInvoked: Int = 0

    def invokeLeft(): Id[Unit] =
      shouldBeInvoked = 1

    def invokeRight(): Id[Unit] =
      shouldBeInvoked = 2

    val m: Id[Option[Int]] = None

    m.valueOr(30)
      .biSemiflatTap(_ => invokeLeft(), _ => invokeRight())
      .run

    shouldBeInvoked == 1
  }

  def notInvokeLeftFunctionOnPositiveEnsureBiSemiflatTap: Boolean = {
    var shouldBeInvoked: Int = 0

    def invokeLeft(): Id[Unit] =
      shouldBeInvoked = 1

    def invokeRight(): Id[Unit] =
      shouldBeInvoked = 2

    val m: Id[Option[Int]] = Id(Some(10))

    m.valueOr(30)
      .ensure(_ => true, 30)
      .biSemiflatTap(_ => invokeLeft(), _ => invokeRight())
      .run

    shouldBeInvoked != 1
  }

  def invokeLeftFunctionOnNegativeEnsureBiSemiflatTap: Boolean = {
    var shouldBeInvoked: Int = 0

    def invokeLeft(): Id[Unit] =
      shouldBeInvoked = 1

    def invokeRight(): Id[Unit] =
      shouldBeInvoked = 2

    0.liftSealed[Id, Nothing]
      .ensure(_ => false, 30)
      .biSemiflatTap(_ => invokeLeft(), _ => invokeRight())
      .run

    shouldBeInvoked == 1
  }

  def invokeRightFunctionOnValueBiSemiflatTap: Boolean = {
    var shouldBeInvoked: Int = 0

    def invokeLeft(): Id[Unit] =
      shouldBeInvoked = 1

    def invokeRight(): Id[Unit] =
      shouldBeInvoked = 2

    0.liftSealed[Id, Nothing]
      .biSemiflatTap(_ => invokeLeft(), _ => invokeRight())
      .run

    shouldBeInvoked == 2
  }

  def invokeRightFunctionOnValueOrOnSomeBiSemiflatTap: Boolean = {
    var shouldBeInvoked: Int = 0

    def invokeLeft(): Id[Unit] =
      shouldBeInvoked = 1

    def invokeRight(): Id[Unit] =
      shouldBeInvoked = 2

    val m: Id[Option[Int]] = Some(1)

    m.valueOr(30)
      .biSemiflatTap(_ => invokeLeft(), _ => invokeRight())
      .run

    shouldBeInvoked == 2
  }

  def notInvokeRightFunctionOnValueOrOnNoneBiSemiflatTap: Boolean = {
    var shouldBeInvoked: Int = 0

    def invokeLeft(): Id[Unit] =
      shouldBeInvoked = 1

    def invokeRight(): Id[Unit] =
      shouldBeInvoked = 2

    val m: Id[Option[Int]] = None

    m.valueOr(30)
      .biSemiflatTap(_ => invokeLeft(), _ => invokeRight())
      .run

    shouldBeInvoked == 1
  }

  def invokeRightFunctionOnPositiveEnsureBiSemiflatTap: Boolean = {
    var shouldBeInvoked: Int = 0

    def invokeLeft(): Id[Unit] =
      shouldBeInvoked = 1

    def invokeRight(): Id[Unit] =
      shouldBeInvoked = 2

    val m: Id[Option[Int]] = Some(1)

    m.valueOr(30)
      .ensure(_ => true, 30)
      .biSemiflatTap(_ => invokeLeft(), _ => invokeRight())
      .run

    shouldBeInvoked == 2
  }

  def notInvokeRightFunctionOnNegativeEnsureBiSemiflatTap: Boolean = {
    var shouldBeInvoked: Int = 0

    def invokeLeft(): Id[Unit] =
      shouldBeInvoked = 1

    def invokeRight(): Id[Unit] =
      shouldBeInvoked = 2

    0.liftSealed[Id, Nothing]
      .ensure(_ => false, 30)
      .biSemiflatTap(_ => invokeLeft(), _ => invokeRight())
      .run

    shouldBeInvoked == 1
  }
}

object SealedSideEffectsSemiflatTap extends SealedSideEffectsSemiflatTap
