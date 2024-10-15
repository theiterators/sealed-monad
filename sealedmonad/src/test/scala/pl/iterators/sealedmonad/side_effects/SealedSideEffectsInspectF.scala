package pl.iterators.sealedmonad.side_effects

import cats.Eval
import pl.iterators.sealedmonad.syntax.*

trait SealedSideEffectsInspectF {

  def simplyInvoke: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      Eval.later { invoked = true }

    def invokeOther() =
      Eval.later { invokedOther = true }

    0
      .seal[Eval]
      .inspectF {
        case Left(10) => invokeOther()
        case Left(20) => invokeOther()
        case Left(0)  => invoke()
      }
      .run
      .value

    invoked && !invokedOther
  }

  def simplyNotInvoke: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      Eval.later { invoked = true }

    def invokeOther() =
      Eval.later { invokedOther = true }

    30.seal[Eval]
      .inspectF {
        case Left(10) => invokeOther()
        case Left(20) => invokeOther()
        case Left(0)  => invoke()
      }
      .run
      .value

    !invoked && !invokedOther
  }

  def invokeOnValueOrOnNone: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      Eval.later { invoked = true }

    def invokeOther() =
      Eval.later {
        invokedOther = true
      }
    val m: Eval[Option[Int]] = Eval.later(None)

    m.valueOr(0)
      .inspectF {
        case Left(10) => invokeOther()
        case Left(20) => invokeOther()
        case Left(0)  => invoke()
      }
      .run
      .value

    invoked && !invokedOther
  }

  def notInvokeOnValueOrOnNone: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      Eval.later {
        invoked = true
      }

    def invokeOther() =
      Eval.later {
        invokedOther = true
      }

    val m: Eval[Option[Int]] = Eval.later(None)

    m.valueOr(30)
      .inspectF {
        case Left(10) => invokeOther()
        case Left(20) => invokeOther()
        case Left(0)  => invoke()
      }
      .run
      .value
    !invoked && !invokedOther
  }

  def invokeOnValueOrOnSome: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      Eval.later {
        invoked = true
      }

    def invokeOther() =
      Eval.later {
        invokedOther = true
      }

    val m: Eval[Option[Int]] = Eval.later(Some(0))

    m.valueOr(30)
      .inspectF {
        case Left(10) => invokeOther()
        case Left(20) => invokeOther()
        case Left(30) => invokeOther()
        case Right(0) => invoke()
      }
      .run
      .value

    invoked && !invokedOther
  }

  def notInvokeOnValueOrOnSome: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      Eval.later {
        invoked = true
      }

    def invokeOther() =
      Eval.later {
        invokedOther = true
      }

    val m: Eval[Option[Int]] = Eval.later(Some(0))

    m.valueOr(30)
      .inspectF {
        case Left(10) => invokeOther()
        case Left(20) => invokeOther()
        case Left(30) => invoke()
      }
      .run
      .value

    !invoked && !invokedOther
  }

  def invokeOnPositiveEnsure: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      Eval.later {
        invoked = true
      }

    def invokeOther() =
      Eval.later {
        invokedOther = true
      }

    0.liftSealed[Eval, Nothing]
      .ensure(_ => true, 30)
      .inspectF {
        case Left(10) => invokeOther()
        case Left(20) => invokeOther()
        case Left(30) => invokeOther()
        case Right(0) => invoke()
      }
      .run
      .value

    invoked && !invokedOther
  }

  def notInvokeOnPositiveEnsure: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      Eval.later {
        invoked = true
      }

    def invokeOther() =
      Eval.later {
        invokedOther = true
      }

    0.liftSealed[Eval, Nothing]
      .ensure(_ => true, 30)
      .inspectF {
        case Left(10) => invokeOther()
        case Left(20) => invokeOther()
        case Left(30) => invoke()
      }
      .run
      .value

    !invoked && !invokedOther
  }

  def invokeOnNegativeEnsure: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      Eval.later {
        invoked = true
      }

    def invokeOther() =
      Eval.later {
        invokedOther = true
      }

    10.liftSealed[Eval, Nothing]
      .ensure(_ => false, 40)
      .inspectF {
        case Left(10) => invokeOther()
        case Left(20) => invokeOther()
        case Left(30) => invokeOther()
        case Left(40) => invoke()
      }
      .run
      .value

    invoked && !invokedOther
  }

  def notInvokeOnNegativeEnsure: Boolean = {
    var invoked: Boolean      = false
    var invokedOther: Boolean = false

    def invoke() =
      Eval.later { invoked = true }

    def invokeOther() =
      Eval.later {
        invokedOther = true
      }

    0.liftSealed[Eval, Nothing]
      .ensure(_ => false, 30)
      .inspectF {
        case Left(10) => invokeOther()
        case Left(20) => invokeOther()
        case Left(30) => invokeOther()
        case Left(0)  => invoke()
      }
      .run
      .value

    !invoked && invokedOther
  }

  def invokeOnSomeValueOrWithNegativeEnsure: Boolean = {
    var shouldBeInvoked: Boolean    = false
    var shouldNotBeInvoked: Boolean = false

    def invoke() =
      Eval.later {
        shouldBeInvoked = true
      }

    def invokeOther() =
      Eval.later {
        shouldNotBeInvoked = true
      }

    val m: Eval[Option[Int]] = Eval.later(Some(0))

    m.valueOr(10)
      .ensure(_ => false, 20)
      .inspectF {
        case Left(0)  => invokeOther()
        case Left(10) => invokeOther()
        case Left(20) => invoke()
      }
      .run
      .value

    shouldBeInvoked && !shouldNotBeInvoked
  }

  def notInvokeOnSomeValueOrWithNegativeEnsure: Boolean = {
    var shouldBeInvoked: Boolean    = false
    var shouldNotBeInvoked: Boolean = false

    def invoke() =
      Eval.later { shouldBeInvoked = true }

    def invokeOther() =
      Eval.later { shouldNotBeInvoked = true }

    val m: Eval[Option[Int]] = Eval.later(Some(0))

    m.valueOr(10)
      .ensure(_ => false, 80)
      .inspectF {
        case Left(0)  => invokeOther()
        case Left(10) => invokeOther()
        case Left(20) => invoke()
      }
      .run
      .value

    !shouldBeInvoked && !shouldNotBeInvoked
  }
}

object SealedSideEffectsInspectF extends SealedSideEffectsInspectF
