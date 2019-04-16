package pl.iterators.sealedmonad.examples

import scala.concurrent.Future

class AllExamplesSpecs extends ExamplesSuite with OptionsTestInstances {

  checkAll("Examples", AllTests[Future].examples)

}
