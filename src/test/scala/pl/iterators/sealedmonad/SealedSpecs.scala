package pl.iterators.sealedmonad

import pl.iterators.sealedmonad.side_effects.SealedSideEffectsInspect
import pl.iterators.sealedmonad.side_effects.SealedSideEffectsTap

class SealedSpecs extends SealedSuite {

  checkAll("Sealed", SealedTests[Option].tests[String, Int])

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
}
