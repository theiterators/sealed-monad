package pl.iterators.sealedmonad

import pl.iterators.sealedmonad.side_effects.{
  SealedSideEffectsInspect,
  SealedSideEffectsInspectF,
  SealedSideEffectsSemiflatTap,
  SealedSideEffectsTap
}

class SealedSpecs extends SealedSuite {

  checkAll("Sealed", SealedTests[Option].tests[String, Int, String])

  test("inspect side effect not invoked") {
    assert(SealedSideEffectsInspect.simplyInvoke)
  }

  test("inspect side effect invoked") {
    assert(SealedSideEffectsInspect.simplyNotInvoke)
  }

  test("inspect with valueOr with None side effect invoked") {
    assert(SealedSideEffectsInspect.invokeOnValueOrOnNone)
  }

  test("inspect with valueOr with None side effect not invoked") {
    assert(SealedSideEffectsInspect.notInvokeOnValueOrOnNone)
  }

  test("inspect with valueOr with Some side effect invoked") {
    assert(SealedSideEffectsInspect.invokeOnValueOrOnSome)
  }

  test("inspect with valueOr with Some side effect not invoked") {
    assert(SealedSideEffectsInspect.notInvokeOnValueOrOnSome)
  }

  test("inspect with positive ensure side effect invoked") {
    assert(SealedSideEffectsInspect.invokeOnPositiveEnsure)
  }

  test("inspect with positive ensure side effect not invoked") {
    assert(SealedSideEffectsInspect.notInvokeOnPositiveEnsure)
  }

  test("inspect with negative ensure negative side effect invoked") {
    assert(SealedSideEffectsInspect.invokeOnNegativeEnsure)
  }

  test("inspect with negative ensure negative side effect not invoked") {
    assert(SealedSideEffectsInspect.notInvokeOnNegativeEnsure)
  }

  test("inspect with Some valueOr with negative ensure negative side effect invoked") {
    assert(SealedSideEffectsInspect.invokeOnSomeValueOrWithNegativeEnsure)
  }

  test("inspect with Some valueOr with negative ensure negative side effect not invoked") {
    assert(SealedSideEffectsInspect.notInvokeOnSomeValueOrWithNegativeEnsure)
  }

  test("inspectF side effect not invoked") {
    assert(SealedSideEffectsInspectF.simplyInvoke)
  }

  test("inspectF side effect invoked") {
    assert(SealedSideEffectsInspectF.simplyNotInvoke)
  }

  test("inspectF with valueOr with None side effect invoked") {
    assert(SealedSideEffectsInspectF.invokeOnValueOrOnNone)
  }

  test("inspectF with valueOr with None side effect not invoked") {
    assert(SealedSideEffectsInspectF.notInvokeOnValueOrOnNone)
  }

  test("inspectF with valueOr with Some side effect invoked") {
    assert(SealedSideEffectsInspectF.invokeOnValueOrOnSome)
  }

  test("inspectF with valueOr with Some side effect not invoked") {
    assert(SealedSideEffectsInspectF.notInvokeOnValueOrOnSome)
  }

  test("inspectF with positive ensure side effect invoked") {
    assert(SealedSideEffectsInspectF.invokeOnPositiveEnsure)
  }

  test("inspectF with positive ensure side effect not invoked") {
    assert(SealedSideEffectsInspectF.notInvokeOnPositiveEnsure)
  }

  test("inspectF with negative ensure negative side effect invoked") {
    assert(SealedSideEffectsInspectF.invokeOnNegativeEnsure)
  }

  test("inspectF with negative ensure negative side effect not invoked") {
    assert(SealedSideEffectsInspectF.notInvokeOnNegativeEnsure)
  }

  test("inspectF with Some valueOr with negative ensure negative side effect invoked") {
    assert(SealedSideEffectsInspectF.invokeOnSomeValueOrWithNegativeEnsure)
  }

  test("inspectF with Some valueOr with negative ensure negative side effect not invoked") {
    assert(SealedSideEffectsInspectF.notInvokeOnSomeValueOrWithNegativeEnsure)
  }

  test("tap side effect invoked") {
    assert(SealedSideEffectsTap.invokeTap)
  }

  test("tapWhenPositive side effect invoked") {
    assert(SealedSideEffectsTap.invokeWithPositiveTapWhen)
  }

  test("tapWhenNegative side effect not invoked") {
    assert(SealedSideEffectsTap.notInvokeWithNegativeTapWhen)
  }

  test("tap with valueOr with Some side effect invoked") {
    assert(SealedSideEffectsTap.invokeOnValueOrOnSome)
  }

  test("tapWhenPositive with valueOr with Some side effect invoked") {
    assert(SealedSideEffectsTap.invokeOnValueOrOnSomeWithPositiveTapWhen)
  }

  test("tapWhenNegative with valueOr with Some side effect not invoked") {
    assert(SealedSideEffectsTap.notInvokeOnValueOrOnSomeWithNegativeTapWhen)
  }

  test("tap with valueOr with None side effect not invoked") {
    assert(SealedSideEffectsTap.notInvokeOnValueOrOnNone)
  }

  test("tapWhenNPositive with valueOr with None side effect not invoked") {
    assert(SealedSideEffectsTap.notInvokeOnValueOrOnNoneWithPositiveTapWhen)
  }

  test("tapWhenNegative with valueOr with None side effect not invoked") {
    assert(SealedSideEffectsTap.notInvokeOnValueOrOnNoneWithNegativeTapWhen)
  }

  test("tap with positive ensure side effect invoked") {
    assert(SealedSideEffectsTap.invokeOnPositiveEnsure)
  }

  test("tapWhenPositive with positive ensure side effect invoked") {
    assert(SealedSideEffectsTap.invokeOnPositiveEnsureWithPositiveTapWhen)
  }

  test("tapWhenNegative with positive ensure side effect not invoked") {
    assert(SealedSideEffectsTap.notInvokeOnPositiveEnsureWithNegativeTapWhen)
  }

  test("tap with negative ensure side effect invoked") {
    assert(SealedSideEffectsTap.notInvokeOnNegativeEnsure)
  }

  test("tapWhenPositive with negative ensure side effect invoked") {
    assert(SealedSideEffectsTap.notInvokeOnNegativeEnsureWithPositiveTapWhen)
  }

  test("tapWhenNegative with negative ensure side effect not invoked") {
    assert(SealedSideEffectsTap.notInvokeOnNegativeEnsureWithNegativeTapWhen)
  }

  test("do not invoke leftSemiflatTap on value") {
    assert(SealedSideEffectsSemiflatTap.notInvokeOnValueLeftSemiflatTap)
  }

  test("do not invoke leftSemiflatTap on Some(_).valueOr") {
    assert(SealedSideEffectsSemiflatTap.notInvokeOnValueOrOnSomeLeftSemiflatTap)
  }

  test("invoke leftSemiflatTap on None.valueOr") {
    assert(SealedSideEffectsSemiflatTap.invokeOnValueOrOnNoneLeftSemiflatTap)
  }

  test("do not invoke leftSemiflatTap on positive ensure check") {
    assert(SealedSideEffectsSemiflatTap.notInvokeOnPositiveEnsureLeftSemiflatTap)
  }

  test("invoke leftSemiflatTap on negative ensure check") {
    assert(SealedSideEffectsSemiflatTap.invokeOnNegativeEnsureLeftSemiflatTap)
  }

  test("do not invoke left function of biSemiflatTap on value") {
    assert(SealedSideEffectsSemiflatTap.notInvokeLeftFunctionOnValueBiSemiflatTap)
  }

  test("do not invoke left function of biSemiflatTap on Some(_).valueOr") {
    assert(SealedSideEffectsSemiflatTap.notInvokeLeftFunctionOnValueOrOnSomeBiSemiflatTap)
  }

  test("invoke left function of biSemiflatTap on None.valueOr") {
    assert(SealedSideEffectsSemiflatTap.invokeLeftFunctionOnValueOrOnNoneBiSemiflatTap)
  }

  test("do not invoke left function of biSemiflatTap on positive ensure check") {
    assert(SealedSideEffectsSemiflatTap.notInvokeLeftFunctionOnPositiveEnsureBiSemiflatTap)
  }

  test("invoke left function of biSemiflatTap on negative ensure check") {
    assert(SealedSideEffectsSemiflatTap.invokeLeftFunctionOnNegativeEnsureBiSemiflatTap)
  }

  test("invoke right function of biSemiflatTap on value") {
    assert(SealedSideEffectsSemiflatTap.invokeRightFunctionOnValueBiSemiflatTap)
  }

  test("invoke right function of biSemiflatTap on Some(_).valueOr") {
    assert(SealedSideEffectsSemiflatTap.invokeRightFunctionOnValueOrOnSomeBiSemiflatTap)
  }

  test("do not invoke right function of biSemiflatTap on None.valueOr") {
    assert(SealedSideEffectsSemiflatTap.notInvokeRightFunctionOnValueOrOnNoneBiSemiflatTap)
  }

  test("invoke right function of biSemiflatTap on positive ensure check") {
    assert(SealedSideEffectsSemiflatTap.invokeRightFunctionOnPositiveEnsureBiSemiflatTap)
  }

  test("do not invoke right function of biSemiflatTap on negative ensure check") {
    assert(SealedSideEffectsSemiflatTap.notInvokeRightFunctionOnNegativeEnsureBiSemiflatTap)
  }

}
