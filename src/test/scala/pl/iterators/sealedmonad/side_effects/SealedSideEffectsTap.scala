package pl.iterators.sealedmonad.side_effects

import cats.Id
import pl.iterators.sealedmonad.syntax._
import cats.instances.option._

trait SealedSideEffectsTap {

  def invokeTap: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke() =
      shouldBeInvoked = true

    0.liftSealed
      .tap(_ => invoke())
      .run

    shouldBeInvoked
  }

  def invokeWithPositiveTapWhen: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke(): Id[Unit] =
      shouldBeInvoked = true

    0.liftSealed
      .flatTapWhen(_ => true, _ => invoke())
      .run

    shouldBeInvoked
  }

  def notInvokeWithNegativeTapWhen: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke(): Id[Unit] =
      shouldBeInvoked = true

    0.liftSealed
      .flatTapWhen(_ => false, _ => invoke())
      .run

    !shouldBeInvoked
  }

  def invokeOnValueOrOnSome: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke() =
      shouldBeInvoked = true

    val m: Id[Option[Int]] = Some(1)

    m.valueOr(30)
      .tap(_ => invoke())
      .run

    shouldBeInvoked
  }

  def invokeOnValueOrOnSomeWithPositiveTapWhen: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke(): Id[Unit] =
      shouldBeInvoked = true

    val m: Id[Option[Int]] = Some(1)

    m.valueOr(30)
      .flatTapWhen(_ => true, _ => invoke())
      .run

    shouldBeInvoked
  }

  def notInvokeOnValueOrOnSomeWithNegativeTapWhen: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke(): Id[Unit] =
      shouldBeInvoked = true

    val m: Id[Option[Int]] = Some(1)

    m.valueOr(30)
      .flatTapWhen(_ => false, _ => invoke())
      .run

    !shouldBeInvoked
  }

  def notInvokeOnValueOrOnNone: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke() =
      shouldBeInvoked = true

    val m: Id[Option[Int]] = None

    m.valueOr(30)
      .tap(_ => invoke())
      .run

    !shouldBeInvoked
  }

  def notInvokeOnValueOrOnNoneWithPositiveTapWhen: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke(): Id[Unit] =
      shouldBeInvoked = true

    val m: Id[Option[Int]] = None

    m.valueOr(30)
      .flatTapWhen(_ => true, _ => invoke())
      .run

    !shouldBeInvoked
  }

  def notInvokeOnValueOrOnNoneWithNegativeTapWhen: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke(): Id[Unit] =
      shouldBeInvoked = true

    val m: Id[Option[Int]] = None

    m.valueOr(30)
      .flatTapWhen(_ => false, _ => invoke())
      .run

    !shouldBeInvoked
  }

  def invokeOnPositiveEnsure: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke() =
      shouldBeInvoked = true

    0.liftSealed
      .ensure(_ => true, 30)
      .tap(_ => invoke())
      .run

    shouldBeInvoked
  }

  def invokeOnPositiveEnsureWithPositiveTapWhen: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke(): Id[Unit] =
      shouldBeInvoked = true

    0.liftSealed
      .ensure(_ => true, 30)
      .flatTapWhen(_ => true, _ => invoke())
      .run

    shouldBeInvoked
  }

  def notInvokeOnPositiveEnsureWithNegativeTapWhen: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke(): Id[Unit] =
      shouldBeInvoked = true

    0.liftSealed
      .ensure(_ => true, 30)
      .flatTapWhen(_ => false, _ => invoke())
      .run

    !shouldBeInvoked
  }

  def notInvokeOnNegativeEnsure: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke() =
      shouldBeInvoked = true

    0.liftSealed
      .ensure(_ => false, 30)
      .tap(_ => invoke())
      .run

    !shouldBeInvoked
  }

  def notInvokeOnNegativeEnsureWithPositiveTapWhen: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke(): Id[Unit] =
      shouldBeInvoked = true

    0.liftSealed
      .ensure(_ => false, 30)
      .flatTapWhen(_ => true, _ => invoke())
      .run

    !shouldBeInvoked
  }

  def notInvokeOnNegativeEnsureWithNegativeTapWhen: Boolean = {
    var shouldBeInvoked: Boolean = false

    def invoke(): Id[Unit] =
      shouldBeInvoked = true

    0.liftSealed
      .ensure(_ => false, 30)
      .flatTapWhen(_ => false, _ => invoke())
      .run

    !shouldBeInvoked
  }
}

object SealedSideEffectsTap extends SealedSideEffectsTap
