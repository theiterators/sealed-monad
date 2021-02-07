package pl.iterators.sealedmonad.side_effects

import cats.Id
import pl.iterators.sealedmonad.syntax._
import cats.catsInstancesForId

trait SealedSideEffectsInspect {

  def simplyInvoke: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      invoked = true

    def invokeOther() =
      invokedOther = true

    0.seal.inspect {
      case Left(10) => invokeOther()
      case Left(20) => invokeOther()
      case Left(0)  => invoke()
    }.run

    invoked && !invokedOther
  }

  def simplyNotInvoke: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      invoked = true

    def invokeOther() =
      invokedOther = true
    30.seal.inspect {
      case Left(10) => invokeOther()
      case Left(20) => invokeOther()
      case Left(0)  => invoke()
    }.run

    !invoked && !invokedOther
  }

  def invokeOnValueOrOnNone: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      invoked = true

    def invokeOther() =
      invokedOther = true

    val m: Id[Option[Int]] = None

    m.valueOr(0)
      .inspect {
        case Left(10) => invokeOther()
        case Left(20) => invokeOther()
        case Left(0)  => invoke()
      }
      .run

    invoked && !invokedOther
  }

  def notInvokeOnValueOrOnNone: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      invoked = true

    def invokeOther() =
      invokedOther = true

    val m: Id[Option[Int]] = None

    m.valueOr(30)
      .inspect {
        case Left(10) => invokeOther()
        case Left(20) => invokeOther()
        case Left(0)  => invoke()
      }
      .run

    !invoked && !invokedOther
  }

  def invokeOnValueOrOnSome: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      invoked = true

    def invokeOther() =
      invokedOther = true

    val m: Id[Option[Int]] = Some(0)

    m.valueOr(30)
      .inspect {
        case Left(10) => invokeOther()
        case Left(20) => invokeOther()
        case Left(30) => invokeOther()
        case Right(0) => invoke()
      }
      .run

    invoked && !invokedOther
  }

  def notInvokeOnValueOrOnSome: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      invoked = true

    def invokeOther() =
      invokedOther = true

    val m: Id[Option[Int]] = Some(0)

    m.valueOr(30)
      .inspect {
        case Left(10) => invokeOther()
        case Left(20) => invokeOther()
        case Left(30) => invoke()
      }
      .run

    !invoked && !invokedOther
  }

  def invokeOnPositiveEnsure: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      invoked = true

    def invokeOther() =
      invokedOther = true

    0.liftSealed
      .ensure(_ => true, 30)
      .inspect {
        case Left(10) => invokeOther()
        case Left(20) => invokeOther()
        case Left(30) => invokeOther()
        case Right(0) => invoke()
      }
      .run
    invoked && !invokedOther
  }

  def notInvokeOnPositiveEnsure: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      invoked = true

    def invokeOther() =
      invokedOther = true

    0.liftSealed
      .ensure(_ => true, 30)
      .inspect {
        case Left(10) => invokeOther()
        case Left(20) => invokeOther()
        case Left(30) => invoke()
      }
      .run

    !invoked && !invokedOther
  }

  def invokeOnNegativeEnsure: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      invoked = true

    def invokeOther() =
      invokedOther = true

    10.liftSealed
      .ensure(_ => false, 40)
      .inspect {
        case Left(10) => invokeOther()
        case Left(20) => invokeOther()
        case Left(30) => invokeOther()
        case Left(40) => invoke()
      }
      .run

    invoked && !invokedOther
  }

  def notInvokeOnNegativeEnsure: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      invoked = true

    def invokeOther() =
      invokedOther = true

    0.liftSealed
      .ensure(_ => false, 30)
      .inspect {
        case Left(10) => invokeOther()
        case Left(20) => invokeOther()
        case Left(30) => invokeOther()
        case Left(0)  => invoke()
      }
      .run

    !invoked && invokedOther
  }

  def invokeOnSomeValueOrWithNegativeEnsure: Boolean = {
    var shouldBeInvoked: Boolean    = false
    var shouldNotBeInvoked: Boolean = false

    def invoke() =
      shouldBeInvoked = true

    def invokeOther() =
      shouldNotBeInvoked = true

    val m: Id[Option[Int]] = Some(0)

    m.valueOr(10)
      .ensure(_ => false, 20)
      .inspect {
        case Left(0)  => invokeOther()
        case Left(10) => invokeOther()
        case Left(20) => invoke()
      }
      .run

    shouldBeInvoked && !shouldNotBeInvoked
  }

  def notInvokeOnSomeValueOrWithNegativeEnsure: Boolean = {
    var shouldBeInvoked: Boolean    = false
    var shouldNotBeInvoked: Boolean = false

    def invoke() =
      shouldBeInvoked = true

    def invokeOther() =
      shouldNotBeInvoked = true

    val m: Id[Option[Int]] = Some(0)

    m.valueOr(10)
      .ensure(_ => false, 80)
      .inspect {
        case Left(0)  => invokeOther()
        case Left(10) => invokeOther()
        case Left(20) => invoke()
      }
      .run

    !shouldBeInvoked && !shouldNotBeInvoked
  }
}

object SealedSideEffectsInspect extends SealedSideEffectsInspect
