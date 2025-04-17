"use strict";(self.webpackChunkwebsite=self.webpackChunkwebsite||[]).push([[99],{5680:(e,n,a)=>{a.d(n,{xA:()=>p,yg:()=>g});var t=a(6540);function r(e,n,a){return n in e?Object.defineProperty(e,n,{value:a,enumerable:!0,configurable:!0,writable:!0}):e[n]=a,e}function o(e,n){var a=Object.keys(e);if(Object.getOwnPropertySymbols){var t=Object.getOwnPropertySymbols(e);n&&(t=t.filter((function(n){return Object.getOwnPropertyDescriptor(e,n).enumerable}))),a.push.apply(a,t)}return a}function i(e){for(var n=1;n<arguments.length;n++){var a=null!=arguments[n]?arguments[n]:{};n%2?o(Object(a),!0).forEach((function(n){r(e,n,a[n])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(a)):o(Object(a)).forEach((function(n){Object.defineProperty(e,n,Object.getOwnPropertyDescriptor(a,n))}))}return e}function s(e,n){if(null==e)return{};var a,t,r=function(e,n){if(null==e)return{};var a,t,r={},o=Object.keys(e);for(t=0;t<o.length;t++)a=o[t],n.indexOf(a)>=0||(r[a]=e[a]);return r}(e,n);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(t=0;t<o.length;t++)a=o[t],n.indexOf(a)>=0||Object.prototype.propertyIsEnumerable.call(e,a)&&(r[a]=e[a])}return r}var l=t.createContext({}),d=function(e){var n=t.useContext(l),a=n;return e&&(a="function"==typeof e?e(n):i(i({},n),e)),a},p=function(e){var n=d(e.components);return t.createElement(l.Provider,{value:n},e.children)},u="mdxType",c={inlineCode:"code",wrapper:function(e){var n=e.children;return t.createElement(t.Fragment,{},n)}},m=t.forwardRef((function(e,n){var a=e.components,r=e.mdxType,o=e.originalType,l=e.parentName,p=s(e,["components","mdxType","originalType","parentName"]),u=d(a),m=r,g=u["".concat(l,".").concat(m)]||u[m]||c[m]||o;return a?t.createElement(g,i(i({ref:n},p),{},{components:a})):t.createElement(g,i({ref:n},p))}));function g(e,n){var a=arguments,r=n&&n.mdxType;if("string"==typeof e||r){var o=a.length,i=new Array(o);i[0]=m;var s={};for(var l in n)hasOwnProperty.call(n,l)&&(s[l]=n[l]);s.originalType=e,s[u]="string"==typeof e?e:r,i[1]=s;for(var d=2;d<o;d++)i[d]=a[d];return t.createElement.apply(null,i)}return t.createElement.apply(null,a)}m.displayName="MDXCreateElement"},8068:(e,n,a)=>{a.r(n),a.d(n,{contentTitle:()=>i,default:()=>u,frontMatter:()=>o,metadata:()=>s,toc:()=>l});var t=a(8168),r=(a(6540),a(5680));const o={id:"faq",title:"Frequently Asked Questions",slug:"/faq"},i="Frequently Asked Questions",s={unversionedId:"faq",id:"faq",title:"Frequently Asked Questions",description:"This document addresses common questions about Sealed Monad, its use cases, and best practices.",source:"@site/../docs/faq.md",sourceDirName:".",slug:"/faq",permalink:"/sealed-monad/faq",editUrl:"https://github.com/theiterators/sealed-monad/tree/master/docs/../docs/faq.md",tags:[],version:"current",frontMatter:{id:"faq",title:"Frequently Asked Questions",slug:"/faq"},sidebar:"docs",previous:{title:"Migration Guide",permalink:"/sealed-monad/migration-guide"},next:{title:"Conclusion",permalink:"/sealed-monad/conclusion"}},l=[{value:"What Problem Does Sealed Monad Solve?",id:"what-problem-does-sealed-monad-solve",children:[],level:2},{value:"How Does Sealed Monad Differ from EitherT?",id:"how-does-sealed-monad-differ-from-eithert",children:[],level:2},{value:"When Should I Use Sealed Monad vs. Plain Either?",id:"when-should-i-use-sealed-monad-vs-plain-either",children:[],level:2},{value:"How Do I Handle Multiple Validation Steps?",id:"how-do-i-handle-multiple-validation-steps",children:[],level:2},{value:"How Do I Work with Optional Values?",id:"how-do-i-work-with-optional-values",children:[],level:2},{value:"How Do I Integrate Sealed Monad with Existing Code?",id:"how-do-i-integrate-sealed-monad-with-existing-code",children:[],level:2},{value:"How Do I Test Code that Uses Sealed Monad?",id:"how-do-i-test-code-that-uses-sealed-monad",children:[],level:2},{value:"Can I Use Sealed Monad with ZIO/Monix/Other Effect Libraries?",id:"can-i-use-sealed-monad-with-ziomonixother-effect-libraries",children:[],level:2},{value:"How Do I Debug Sealed Monad Code?",id:"how-do-i-debug-sealed-monad-code",children:[],level:2},{value:"How Does Sealed Monad Handle Performance?",id:"how-does-sealed-monad-handle-performance",children:[],level:2},{value:"Is Sealed Monad Compatible with Scala 3?",id:"is-sealed-monad-compatible-with-scala-3",children:[],level:2},{value:"Why Do I Get a Compilation Error with the <code>run</code> Method?",id:"why-do-i-get-a-compilation-error-with-the-run-method",children:[{value:"Common Solutions",id:"common-solutions",children:[],level:3},{value:"Required Imports",id:"required-imports",children:[],level:3}],level:2},{value:"Where Can I Learn More?",id:"where-can-i-learn-more",children:[],level:2}],d={toc:l},p="wrapper";function u(e){let{components:n,...a}=e;return(0,r.yg)(p,(0,t.A)({},d,a,{components:n,mdxType:"MDXLayout"}),(0,r.yg)("h1",{id:"frequently-asked-questions"},"Frequently Asked Questions"),(0,r.yg)("p",null,"This document addresses common questions about Sealed Monad, its use cases, and best practices."),(0,r.yg)("h2",{id:"what-problem-does-sealed-monad-solve"},"What Problem Does Sealed Monad Solve?"),(0,r.yg)("p",null,"Traditional error handling in Scala\u2014whether through nested conditionals, monad transformers like ",(0,r.yg)("inlineCode",{parentName:"p"},"EitherT")," or ",(0,r.yg)("inlineCode",{parentName:"p"},"OptionT"),", or even plain exceptions\u2014often leads to convoluted and hard-to-read code. Sealed Monad simplifies business logic by:"),(0,r.yg)("p",null,"\u2022 Representing all possible outcomes with a sealed trait (ADT)",(0,r.yg)("br",{parentName:"p"}),"\n","\u2022 Allowing error conditions to be handled locally in a single, top-down for-comprehension",(0,r.yg)("br",{parentName:"p"}),"\n","\u2022 Eliminating the need for deeply nested pattern-matching or explicit monad transformer stacking"),(0,r.yg)("h2",{id:"how-does-sealed-monad-differ-from-eithert"},"How Does Sealed Monad Differ from EitherT?"),(0,r.yg)("p",null,"Sealed Monad can be thought of as an enhanced version of EitherT with a more business-oriented API. The key differences are:"),(0,r.yg)("ol",null,(0,r.yg)("li",{parentName:"ol"},(0,r.yg)("p",{parentName:"li"},(0,r.yg)("strong",{parentName:"p"},"More expressive API"),": Method names like ",(0,r.yg)("inlineCode",{parentName:"p"},"valueOr"),", ",(0,r.yg)("inlineCode",{parentName:"p"},"ensure"),", and ",(0,r.yg)("inlineCode",{parentName:"p"},"attempt")," clearly communicate intent compared to EitherT's more abstract operations.")),(0,r.yg)("li",{parentName:"ol"},(0,r.yg)("p",{parentName:"li"},(0,r.yg)("strong",{parentName:"p"},"Uniform treatment of results"),': Sealed Monad doesn\'t distinguish between "errors" and "successes" conceptually\u2014everything is just a result of the operation.')),(0,r.yg)("li",{parentName:"ol"},(0,r.yg)("p",{parentName:"li"},(0,r.yg)("strong",{parentName:"p"},"Simplified composition"),": When working with domain-specific error types, Sealed Monad requires less boilerplate than EitherT to combine operations.")),(0,r.yg)("li",{parentName:"ol"},(0,r.yg)("p",{parentName:"li"},(0,r.yg)("strong",{parentName:"p"},"Focus on ADTs"),": Sealed Monad encourages modeling domain results as explicit ADTs rather than using generic error types."))),(0,r.yg)("h2",{id:"when-should-i-use-sealed-monad-vs-plain-either"},"When Should I Use Sealed Monad vs. Plain Either?"),(0,r.yg)("p",null,"Use Sealed Monad when:"),(0,r.yg)("ul",null,(0,r.yg)("li",{parentName:"ul"},"You need to compose operations that can fail in different ways"),(0,r.yg)("li",{parentName:"ul"},"You want to express business logic in a linear, step-by-step fashion"),(0,r.yg)("li",{parentName:"ul"},"Your operations work with effectful types like Future or IO"),(0,r.yg)("li",{parentName:"ul"},"You have complex validation workflows with multiple potential outcomes")),(0,r.yg)("p",null,"Plain Either is simpler and sufficient when:"),(0,r.yg)("ul",null,(0,r.yg)("li",{parentName:"ul"},"You're working with synchronous code without effects"),(0,r.yg)("li",{parentName:"ul"},"You have simple success/failure semantics"),(0,r.yg)("li",{parentName:"ul"},"You don't need to combine multiple operations with different error types")),(0,r.yg)("h2",{id:"how-do-i-handle-multiple-validation-steps"},"How Do I Handle Multiple Validation Steps?"),(0,r.yg)("p",null,"Sealed Monad excels at expressing multiple validation steps clearly. Here's an example:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},"import pl.iterators.sealedmonad.syntax._\nimport cats.effect.IO\n\ndef validateOrder(order: Order): IO[OrderValidationResult] = {\n  (for {\n    // Validate order has items\n    _ <- IO.pure(order.items.nonEmpty)\n           .ensure(identity, OrderValidationResult.EmptyOrder)\n    \n    // Validate all items are in stock\n    stockCheck <- inventoryService.checkStock(order.items).seal\n    _ <- IO.pure(stockCheck.allInStock)\n           .ensure(identity, OrderValidationResult.OutOfStock(stockCheck.outOfStockItems))\n    \n    // Validate payment information\n    _ <- validatePaymentInfo(order.payment)\n           .valueOr(OrderValidationResult.InvalidPayment)\n    \n    // Validate shipping address\n    _ <- validateShippingAddress(order.shippingAddress)\n           .valueOr(OrderValidationResult.InvalidShippingAddress)\n  } yield OrderValidationResult.Valid).run\n}\n")),(0,r.yg)("p",null,"Each validation step is clearly expressed, and the computation short-circuits as soon as any validation fails."),(0,r.yg)("h2",{id:"how-do-i-work-with-optional-values"},"How Do I Work with Optional Values?"),(0,r.yg)("p",null,"Use the ",(0,r.yg)("inlineCode",{parentName:"p"},"valueOr")," operator to handle optional values:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},"// Find a user by ID, with NotFound as the fallback\nuserRepository.findById(userId)  // IO[Option[User]]\n  .valueOr[UserResponse](UserResponse.NotFound)  // Sealed[IO, User, UserResponse]\n")),(0,r.yg)("p",null,"If you need to provide an effectful fallback, use ",(0,r.yg)("inlineCode",{parentName:"p"},"valueOrF"),":"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},'// Find a user by ID, with logging and NotFound as the fallback\nuserRepository.findById(userId)\n  .valueOrF(\n    logger.warn(s"User not found: $userId") *> \n    IO.pure(UserResponse.NotFound)\n  )\n')),(0,r.yg)("h2",{id:"how-do-i-integrate-sealed-monad-with-existing-code"},"How Do I Integrate Sealed Monad with Existing Code?"),(0,r.yg)("p",null,"If you're integrating with existing code, you can:"),(0,r.yg)("ol",null,(0,r.yg)("li",{parentName:"ol"},(0,r.yg)("p",{parentName:"li"},(0,r.yg)("strong",{parentName:"p"},"Wrap Option-returning functions"),":"),(0,r.yg)("pre",{parentName:"li"},(0,r.yg)("code",{parentName:"pre",className:"language-scala"},"legacyService.findUserById(id)  // Future[Option[User]]\n  .valueOr(UserError.NotFound)  // Sealed[Future, User, UserError]\n"))),(0,r.yg)("li",{parentName:"ol"},(0,r.yg)("p",{parentName:"li"},(0,r.yg)("strong",{parentName:"p"},"Wrap Either-returning functions"),":"),(0,r.yg)("pre",{parentName:"li"},(0,r.yg)("code",{parentName:"pre",className:"language-scala"},"legacyService.validateInput(data)  // Future[Either[String, ValidatedData]]\n  .fromEither  // Sealed[Future, ValidatedData, String]\n"))),(0,r.yg)("li",{parentName:"ol"},(0,r.yg)("p",{parentName:"li"},(0,r.yg)("strong",{parentName:"p"},"Use attempt for exception-handling code"),":"),(0,r.yg)("pre",{parentName:"li"},(0,r.yg)("code",{parentName:"pre",className:"language-scala"},"IO.delay(legacyService.riskyOperation())  // IO[Result]\n  .attempt {\n    case Right(result) => Right(result)\n    case Left(ex: NotFoundException) => Left(Error.NotFound)\n    case Left(ex) => Left(Error.Unknown(ex.getMessage))\n  }\n")))),(0,r.yg)("h2",{id:"how-do-i-test-code-that-uses-sealed-monad"},"How Do I Test Code that Uses Sealed Monad?"),(0,r.yg)("p",null,"Testing Sealed Monad code is straightforward because it works with standard effect types:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},'import org.scalatest.flatspec.AnyFlatSpec\nimport org.scalatest.matchers.should.Matchers\nimport cats.effect.unsafe.implicits.global\n\nclass UserServiceSpec extends AnyFlatSpec with Matchers {\n  \n  "UserService.findUser" should "return Success when user exists" in {\n    val service = new UserService(\n      repository = mockRepository(existingUserId = Some("user-123"))\n    )\n    \n    val result = service.findUser("user-123").unsafeRunSync()\n    result shouldBe UserResponse.Success(User("user-123", "test@example.com"))\n  }\n  \n  it should "return NotFound when user doesn\'t exist" in {\n    val service = new UserService(\n      repository = mockRepository(existingUserId = None)\n    )\n    \n    val result = service.findUser("invalid").unsafeRunSync()\n    result shouldBe UserResponse.NotFound\n  }\n  \n  // Helper to create mock repository\n  private def mockRepository(existingUserId: Option[String]): UserRepository = \n    new UserRepository {\n      def findById(id: String): IO[Option[User]] = \n        IO.pure(\n          if (existingUserId.contains(id)) Some(User(id, "test@example.com"))\n          else None\n        )\n    }\n}\n')),(0,r.yg)("h2",{id:"can-i-use-sealed-monad-with-ziomonixother-effect-libraries"},"Can I Use Sealed Monad with ZIO/Monix/Other Effect Libraries?"),(0,r.yg)("p",null,"Yes! Sealed Monad is built on cats-core and works with any effect type that has a Monad instance. This includes:"),(0,r.yg)("ul",null,(0,r.yg)("li",{parentName:"ul"},"Cats Effect IO"),(0,r.yg)("li",{parentName:"ul"},"ZIO"),(0,r.yg)("li",{parentName:"ul"},"Monix Task"),(0,r.yg)("li",{parentName:"ul"},"Standard library Future"),(0,r.yg)("li",{parentName:"ul"},"Any other effect type with a cats Monad instance")),(0,r.yg)("h2",{id:"how-do-i-debug-sealed-monad-code"},"How Do I Debug Sealed Monad Code?"),(0,r.yg)("p",null,"Sealed Monad provides several operators for debugging:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},'import pl.iterators.sealedmonad.syntax._\nimport cats.effect.IO\n\ndef processOrder(orderId: String): IO[OrderResponse] = {\n  (for {\n    // Find order with debug logging\n    order <- orderRepository.findById(orderId)\n               .valueOr(OrderResponse.NotFound)\n               .inspect {\n                 case Right(o) => println(s"Found order: $orderId")\n                 case Left(OrderResponse.NotFound) => println(s"Order not found: $orderId")\n               }\n    \n    // Process with side-effect logging\n    result <- processOrderItems(order.items)\n                .tap(r => println(s"Processed ${r.size} items"))\n  } yield OrderResponse.Success(order.id)).run\n}\n')),(0,r.yg)("p",null,"The ",(0,r.yg)("inlineCode",{parentName:"p"},"inspect")," operator lets you observe the current state, while ",(0,r.yg)("inlineCode",{parentName:"p"},"tap")," and ",(0,r.yg)("inlineCode",{parentName:"p"},"flatTap")," allow you to perform side effects without affecting the computation."),(0,r.yg)("h2",{id:"how-does-sealed-monad-handle-performance"},"How Does Sealed Monad Handle Performance?"),(0,r.yg)("p",null,"Sealed Monad adds minimal overhead compared to direct monadic operations. For most business logic, the clarity and maintainability benefits far outweigh any performance considerations."),(0,r.yg)("p",null,"If you have performance-critical code, consider:"),(0,r.yg)("ol",null,(0,r.yg)("li",{parentName:"ol"},"Only use Sealed Monad for the business logic portions that benefit from clear error handling"),(0,r.yg)("li",{parentName:"ol"},"For hot paths with simple success/failure semantics, use more direct approaches"),(0,r.yg)("li",{parentName:"ol"},"Profile your application to identify actual bottlenecks before optimizing")),(0,r.yg)("h2",{id:"is-sealed-monad-compatible-with-scala-3"},"Is Sealed Monad Compatible with Scala 3?"),(0,r.yg)("p",null,"Yes, Sealed Monad is compatible with both Scala 2.13.x and Scala 3.x."),(0,r.yg)("h2",{id:"why-do-i-get-a-compilation-error-with-the-run-method"},"Why Do I Get a Compilation Error with the ",(0,r.yg)("inlineCode",{parentName:"h2"},"run")," Method?"),(0,r.yg)("p",null,"The ",(0,r.yg)("inlineCode",{parentName:"p"},"run")," method in Sealed Monad has a specific type constraint: the success type ",(0,r.yg)("inlineCode",{parentName:"p"},"A")," must be a subtype of the error type ",(0,r.yg)("inlineCode",{parentName:"p"},"ADT"),". This is enforced by the type parameter bound:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},"def run[ADT1 >: ADT](implicit ev: A <:< ADT1, F: Monad[F]): F[ADT1]\n")),(0,r.yg)("p",null,"This constraint exists because the ",(0,r.yg)("inlineCode",{parentName:"p"},"run")," method needs to return a single type that can represent both successful and error outcomes."),(0,r.yg)("h3",{id:"common-solutions"},"Common Solutions"),(0,r.yg)("ol",null,(0,r.yg)("li",{parentName:"ol"},(0,r.yg)("p",{parentName:"li"},(0,r.yg)("strong",{parentName:"p"},"Make your success type extend your error type"),":"),(0,r.yg)("pre",{parentName:"li"},(0,r.yg)("code",{parentName:"pre",className:"language-scala"},"sealed trait Response\ncase class Success(value: Int) extends Response\ncase object NotFound extends Response\n\n// Now Success <:< Response, so this works:\nval result: Future[Response] = sealedValue.run\n"))),(0,r.yg)("li",{parentName:"ol"},(0,r.yg)("p",{parentName:"li"},(0,r.yg)("strong",{parentName:"p"},"Map your intermediate value to a result type before calling ",(0,r.yg)("inlineCode",{parentName:"strong"},"run")),":"),(0,r.yg)("pre",{parentName:"li"},(0,r.yg)("code",{parentName:"pre",className:"language-scala"},"sealed trait Response\ncase class Success(value: Int) extends Response\ncase object NotFound extends Response\n\n// Map Int to Success before calling run\nval result: Future[Response] = sealedInt.map(Success).run\n"))),(0,r.yg)("li",{parentName:"ol"},(0,r.yg)("p",{parentName:"li"},(0,r.yg)("strong",{parentName:"p"},"Use pattern matching after the computation"),":"),(0,r.yg)("pre",{parentName:"li"},(0,r.yg)("code",{parentName:"pre",className:"language-scala"},"// Instead of calling run directly\nval either: Future[Either[Error, Int]] = sealedInt.map(Right(_)).getOrElse(Left(Error))\n\n// Then pattern match on the result\neither.map {\n  case Right(value) => handleSuccess(value)\n  case Left(error) => handleError(error)\n}\n")))),(0,r.yg)("h3",{id:"required-imports"},"Required Imports"),(0,r.yg)("p",null,"When working with Sealed Monad, make sure you have all the necessary imports:"),(0,r.yg)("pre",null,(0,r.yg)("code",{parentName:"pre",className:"language-scala"},"import pl.iterators.sealedmonad.syntax._  // For extension methods\nimport cats.instances.future._  // For Future instances\nimport cats.syntax.applicative._  // For pure method\n")),(0,r.yg)("p",null,"The ",(0,r.yg)("inlineCode",{parentName:"p"},"cats.syntax.applicative._")," import is particularly important when using methods like ",(0,r.yg)("inlineCode",{parentName:"p"},"pure")," on primitive values."),(0,r.yg)("h2",{id:"where-can-i-learn-more"},"Where Can I Learn More?"),(0,r.yg)("ul",null,(0,r.yg)("li",{parentName:"ul"},(0,r.yg)("a",{parentName:"li",href:"https://github.com/theiterators/sealed-monad"},"GitHub Repository")),(0,r.yg)("li",{parentName:"ul"},(0,r.yg)("a",{parentName:"li",href:"https://javadoc.io/doc/pl.iterators/sealed-monad_2.13/latest/index.html"},"API Documentation")),(0,r.yg)("li",{parentName:"ul"},(0,r.yg)("a",{parentName:"li",href:"https://www.youtube.com/watch?v=uZ7IFQTYPic"},"Marcin Rze\u017anicki's Talk: Reach ADT or Die")," - Learn about the design philosophy behind Sealed Monad")))}u.isMDXComponent=!0}}]);