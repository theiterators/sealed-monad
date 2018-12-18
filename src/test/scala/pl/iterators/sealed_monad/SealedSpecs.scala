package pl.iterators.sealed_monad

class SealedSpecs extends SealedSuite {

  checkAll("Sealed", SealedTests[Option].tests[String, Int])

}
