package pl.iterators.sealed_monad.examples

import scala.concurrent.Future

class AllExamplesSpecs extends ExamplesSuite with OptionsTestInstances {

  checkAll("Examples", AllTests[Future].examples)

}
