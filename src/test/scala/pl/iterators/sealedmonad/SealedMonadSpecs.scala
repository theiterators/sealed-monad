package pl.iterators.sealedmonad

class SealedMonadSpecs extends SealedSuite {
  import SealedTestInstances.ADT
  import cats.laws.discipline.MonadTests
  checkAll("Sealed", MonadTests[Sealed[Option, ?, ADT]].monad[String, String, Int])

  test("biSemiflatMap consistent with leftSemiflatMap and semiflatMap") {
    forAll { (sealedMonad: Sealed[List, Either[ADT, String], ADT], fa: ADT => List[ADT], fb: String => List[Int]) =>
      assert(sealedMonad.biSemiflatMap(fa, fb) === (sealedMonad.leftSemiflatMap(fa).semiflatMap(fb)))
    }
  }

}
