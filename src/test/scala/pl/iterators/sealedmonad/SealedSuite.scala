package pl.iterators.sealedmonad

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

abstract class SealedSuite extends AnyFunSuite with FunSuiteDiscipline with Checkers with SealedTestInstances
