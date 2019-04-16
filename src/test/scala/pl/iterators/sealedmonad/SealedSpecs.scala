package pl.iterators.sealedmonad

class SealedSpecs extends SealedSuite {

  checkAll("Sealed", SealedTests[Option].tests[String, Int])

}
