package pl.iterators.sealedmonad.examples

import cats.Monad
import cats.instances.future._
import cats.instances.option._
import cats.syntax.applicative._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pl.iterators.sealedmonad.syntax._
import pl.iterators.sealedmonad.Sealed

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.concurrent.Await

class ApiReferenceTests extends AnyFunSuite with Matchers {

  implicit val ec: ExecutionContext = ExecutionContext.global

  // Test for the API operations from api-reference.md
  test("API operations from api-reference.md should work as expected") {
    // Following the examples in the documentation

    // Define our domain models as shown in the examples
    sealed trait Response
    case class Value(i: Int)  extends Response
    case object NotFound      extends Response
    case object InvalidInput  extends Response
    case object DatabaseError extends Response

    // Test data
    val value = 42

    // Test seal operation
    {
      // Example from docs: val sealedValue: Sealed[IO, Int, String] = IO.pure(42).seal[String]
      val sealedValue = Future.successful(value).seal[Response]

      // For run to work, we need to map the Int to a Response
      val result = Await.result(sealedValue.map(Value).run, 5.seconds)
      result shouldBe Value(value)
    }

    // Test valueOr operation
    {
      // Example from docs: val sealedUser: Sealed[IO, User, MyError] = maybeUser.valueOr(MyError.UserNotFound)
      val maybeValue: Future[Option[Int]] = Future.successful(Some(value))
      val sealedValue                     = maybeValue.valueOr[Response](NotFound)

      // For run to work, we need to map the Int to a Response
      val result = Await.result(sealedValue.map(Value).run, 5.seconds)
      result shouldBe Value(value)

      // Test with None
      val maybeNoValue: Future[Option[Int]] = Future.successful(None)
      val sealedNoValue                     = maybeNoValue.valueOr[Response](NotFound)

      val resultNoValue = Await.result(sealedNoValue.map(Value).run, 5.seconds)
      resultNoValue shouldBe NotFound
    }

    // Test ensure operation
    {
      // Example from docs: val activeUser: Sealed[IO, User, MyError] = user.ensure(u => !u.archived, MyError.UserInactive)
      val sealedValue = Future.successful(value).seal[Response]

      // Test with valid condition
      val validValue  = sealedValue.ensure(_ > 0, InvalidInput)
      val validResult = Await.result(validValue.map(Value).run, 5.seconds)
      validResult shouldBe Value(value)

      // Test with invalid condition
      val invalidValue  = sealedValue.ensure(_ > 100, InvalidInput)
      val invalidResult = Await.result(invalidValue.map(Value).run, 5.seconds)
      invalidResult shouldBe InvalidInput
    }

    // Test attempt operation
    {
      // Example from docs: val processedOrder: Sealed[IO, ProcessedOrder, OrderError] = order.attempt { order => ... }
      val sealedValue = Future.successful(value).seal[Response]

      // Test with successful attempt
      val successAttempt = sealedValue.attempt { v =>
        if (v > 0) Right(v * 2)
        else Left(InvalidInput)
      }

      val successResult = Await.result(successAttempt.map(Value).run, 5.seconds)
      successResult shouldBe Value(value * 2)

      // Test with failed attempt
      val failAttempt = sealedValue.attempt { v =>
        if (v > 100) Right(v)
        else Left(InvalidInput)
      }

      val failResult = Await.result(failAttempt.map(Value).run, 5.seconds)
      failResult shouldBe InvalidInput
    }

    // Test tap operation
    {
      // Example from docs: val loggedUser: Sealed[IO, User, UserError] = user.tap(u => println(s"User found: ${u.email}"))
      val sealedValue = Future.successful(value).seal[Response]

      var tapped = false
      val tappedValue = sealedValue.tap { v =>
        tapped = true
        v
      }

      val result = Await.result(tappedValue.map(Value).run, 5.seconds)
      result shouldBe Value(value)
      tapped shouldBe true
    }

    // Test complete operation
    {
      // Example from docs: val loginResponse: Sealed[IO, Nothing, LoginResponse] = user.complete(u => LoginResponse.LoggedIn(generateToken(u)))
      val sealedValue = Future.successful(value).seal[Response]

      // Complete with a Response
      val response = sealedValue.complete { v =>
        Value(v * 2)
      }

      val result = Await.result(response.run, 5.seconds)
      result shouldBe Value(value * 2)
    }

    // Test fromEither operation
    {
      // Example from docs: val sealedResult: Sealed[IO, Int, String] = result.fromEither
      val eitherResult: Future[Either[Response, Int]] = Future.successful(Right(value))
      val sealedResult                                = eitherResult.fromEither

      val result = Await.result(sealedResult.map(Value).run, 5.seconds)
      result shouldBe Value(value)

      // Test with Left
      val eitherError: Future[Either[Response, Int]] = Future.successful(Left(DatabaseError))
      val sealedError                                = eitherError.fromEither

      val errorResult = Await.result(sealedError.map(Value).run, 5.seconds)
      errorResult shouldBe DatabaseError
    }
  }
}
