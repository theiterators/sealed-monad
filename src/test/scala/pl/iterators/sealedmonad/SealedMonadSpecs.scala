package pl.iterators.sealedmonad

class SealedMonadSpecs extends SealedSuite {

  import SealedTestInstances.ADT
  import cats.laws.discipline.MonadTests

  checkAll("Sealed", MonadTests[Sealed[Option, *, ADT]].monad[String, String, Int])

}
