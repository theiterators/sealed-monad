"use strict";(self.webpackChunkwebsite=self.webpackChunkwebsite||[]).push([[29],{5680:(e,n,t)=>{t.d(n,{xA:()=>u,yg:()=>h});var a=t(6540);function o(e,n,t){return n in e?Object.defineProperty(e,n,{value:t,enumerable:!0,configurable:!0,writable:!0}):e[n]=t,e}function i(e,n){var t=Object.keys(e);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);n&&(a=a.filter((function(n){return Object.getOwnPropertyDescriptor(e,n).enumerable}))),t.push.apply(t,a)}return t}function r(e){for(var n=1;n<arguments.length;n++){var t=null!=arguments[n]?arguments[n]:{};n%2?i(Object(t),!0).forEach((function(n){o(e,n,t[n])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(t)):i(Object(t)).forEach((function(n){Object.defineProperty(e,n,Object.getOwnPropertyDescriptor(t,n))}))}return e}function l(e,n){if(null==e)return{};var t,a,o=function(e,n){if(null==e)return{};var t,a,o={},i=Object.keys(e);for(a=0;a<i.length;a++)t=i[a],n.indexOf(t)>=0||(o[t]=e[t]);return o}(e,n);if(Object.getOwnPropertySymbols){var i=Object.getOwnPropertySymbols(e);for(a=0;a<i.length;a++)t=i[a],n.indexOf(t)>=0||Object.prototype.propertyIsEnumerable.call(e,t)&&(o[t]=e[t])}return o}var s=a.createContext({}),d=function(e){var n=a.useContext(s),t=n;return e&&(t="function"==typeof e?e(n):r(r({},n),e)),t},u=function(e){var n=d(e.components);return a.createElement(s.Provider,{value:n},e.children)},p="mdxType",c={inlineCode:"code",wrapper:function(e){var n=e.children;return a.createElement(a.Fragment,{},n)}},g=a.forwardRef((function(e,n){var t=e.components,o=e.mdxType,i=e.originalType,s=e.parentName,u=l(e,["components","mdxType","originalType","parentName"]),p=d(t),g=o,h=p["".concat(s,".").concat(g)]||p[g]||c[g]||i;return t?a.createElement(h,r(r({ref:n},u),{},{components:t})):a.createElement(h,r({ref:n},u))}));function h(e,n){var t=arguments,o=n&&n.mdxType;if("string"==typeof e||o){var i=t.length,r=new Array(i);r[0]=g;var l={};for(var s in n)hasOwnProperty.call(n,s)&&(l[s]=n[s]);l.originalType=e,l[p]="string"==typeof e?e:o,r[1]=l;for(var d=2;d<i;d++)r[d]=t[d];return a.createElement.apply(null,r)}return a.createElement.apply(null,t)}g.displayName="MDXCreateElement"},7121:(e,n,t)=>{t.r(n),t.d(n,{assets:()=>s,contentTitle:()=>r,default:()=>c,frontMatter:()=>i,metadata:()=>l,toc:()=>d});var a=t(8168),o=(t(6540),t(5680));const i={id:"motivations",title:"Motivations & Core Concepts",slug:"/motivations"},r=void 0,l={unversionedId:"motivations",id:"motivations",title:"Motivations & Core Concepts",description:"Why We Created Sealed Monad",source:"@site/../docs/motivations.md",sourceDirName:".",slug:"/motivations",permalink:"/sealed-monad/motivations",draft:!1,editUrl:"https://github.com/theiterators/sealed-monad/tree/master/docs/../docs/motivations.md",tags:[],version:"current",frontMatter:{id:"motivations",title:"Motivations & Core Concepts",slug:"/motivations"},sidebar:"docs",previous:{title:"Introduction to Sealed Monad",permalink:"/sealed-monad/"},next:{title:"Installation Guide",permalink:"/sealed-monad/installation"}},s={},d=[{value:"Why We Created Sealed Monad",id:"why-we-created-sealed-monad",level:2},{value:"Key Observations",id:"key-observations",level:3},{value:"1. Operation Results as ADTs",id:"1-operation-results-as-adts",level:4},{value:"2. Methods as Self-Contained Units",id:"2-methods-as-self-contained-units",level:4},{value:"3. No Arbitrary Error/Success Distinction",id:"3-no-arbitrary-errorsuccess-distinction",level:4},{value:"4. Method-Local Error Handling",id:"4-method-local-error-handling",level:4},{value:"5. For-Comprehension Friendly",id:"5-for-comprehension-friendly",level:4},{value:"6. Linear vs. Branching Logic",id:"6-linear-vs-branching-logic",level:4},{value:"Core Concepts of Sealed Monad",id:"core-concepts-of-sealed-monad",level:2},{value:"The Sealed Type",id:"the-sealed-type",level:3},{value:"The Execution Flow",id:"the-execution-flow",level:3},{value:"Key Operations",id:"key-operations",level:3},{value:"Extraction Operations",id:"extraction-operations",level:4},{value:"Validation Operations",id:"validation-operations",level:4},{value:"Transformation Operations",id:"transformation-operations",level:4},{value:"Side Effect Operations",id:"side-effect-operations",level:4},{value:"Comparing with Traditional Approaches",id:"comparing-with-traditional-approaches",level:3},{value:"Without Sealed Monad",id:"without-sealed-monad",level:4},{value:"With Sealed Monad",id:"with-sealed-monad",level:4}],u={toc:d},p="wrapper";function c(e){let{components:n,...t}=e;return(0,o.yg)(p,(0,a.A)({},u,t,{components:n,mdxType:"MDXLayout"}),(0,o.yg)("h2",{id:"why-we-created-sealed-monad"},"Why We Created Sealed Monad"),(0,o.yg)("p",null,"We created Sealed Monad after observing patterns and challenges in real-world business logic. We noticed that well-designed business logic often follows certain principles, but traditional error handling approaches made implementation verbose and hard to read."),(0,o.yg)("p",null,"Let's first define some domain models we'll use in our examples:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},"import scala.concurrent.Future\nimport cats.Monad\nimport cats.instances.future._\nimport cats.data.OptionT\n\n// Domain models\nsealed trait Provider\nobject Provider {\n  case object EmailPass extends Provider\n  case object OAuth extends Provider\n}\n\ncase class User(id: Long, email: String, archived: Boolean)\ncase class AuthMethod(userId: Long, provider: Provider)\n\n// Result ADT for our login operation\nsealed trait LoginResponse\nobject LoginResponse {\n  final case class LoggedIn(token: String) extends LoginResponse\n  case object InvalidCredentials extends LoginResponse\n  case object Deleted extends LoginResponse\n  case object ProviderAuthFailed extends LoginResponse\n}\n")),(0,o.yg)("h3",{id:"key-observations"},"Key Observations"),(0,o.yg)("h4",{id:"1-operation-results-as-adts"},"1. Operation Results as ADTs"),(0,o.yg)("p",null,"Well-designed services represent operation results as Algebraic Data Types (ADTs), usually with a sealed trait and several case classes/objects. This approach models different business outcomes explicitly and comprehensively."),(0,o.yg)("h4",{id:"2-methods-as-self-contained-units"},"2. Methods as Self-Contained Units"),(0,o.yg)("p",null,"Service methods are designed as closed units of code, each returning one value from the result ADT:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},"def login(email: String,\n          findUser: String => Future[Option[User]],\n          findAuthMethod: (Long, Provider) => Future[Option[AuthMethod]],\n          issueTokenFor: User => String,\n          checkAuthMethod: AuthMethod => Boolean): Future[LoginResponse] = ???\n")),(0,o.yg)("h4",{id:"3-no-arbitrary-errorsuccess-distinction"},"3. No Arbitrary Error/Success Distinction"),(0,o.yg)("p",null,'We found that distinguishing between "errors" and "valid results" is often arbitrary in business logic. For example, when a user tries to log in with a deleted account, is "Deleted" an error or a legitimate response? With Sealed Monad, everything is simply a response.'),(0,o.yg)("h4",{id:"4-method-local-error-handling"},"4. Method-Local Error Handling"),(0,o.yg)("p",null,"Global or module-based error handling can be harmful to application architecture. Different operations need different error-handling strategies. Sealed Monad encourages handling business outcomes at the method level where context is clear."),(0,o.yg)("h4",{id:"5-for-comprehension-friendly"},"5. For-Comprehension Friendly"),(0,o.yg)("p",null,"For-comprehensions provide a clean, sequential way to express business logic. Sealed Monad is designed to work seamlessly with for-comprehensions."),(0,o.yg)("h4",{id:"6-linear-vs-branching-logic"},"6. Linear vs. Branching Logic"),(0,o.yg)("p",null,"Traditional if-else or pattern-matching creates branching logic that becomes hard to follow. Sealed Monad aims to linearize the flow, making code more readable."),(0,o.yg)("h2",{id:"core-concepts-of-sealed-monad"},"Core Concepts of Sealed Monad"),(0,o.yg)("h3",{id:"the-sealed-type"},"The Sealed Type"),(0,o.yg)("p",null,"The core type in Sealed Monad is ",(0,o.yg)("inlineCode",{parentName:"p"},"Sealed[F[_], +A, +ADT]")," with three type parameters:"),(0,o.yg)("ul",null,(0,o.yg)("li",{parentName:"ul"},(0,o.yg)("inlineCode",{parentName:"li"},"F[_]"),": The effect type (e.g., ",(0,o.yg)("inlineCode",{parentName:"li"},"Future"),", ",(0,o.yg)("inlineCode",{parentName:"li"},"IO"),", ",(0,o.yg)("inlineCode",{parentName:"li"},"Id"),")"),(0,o.yg)("li",{parentName:"ul"},(0,o.yg)("inlineCode",{parentName:"li"},"A"),': The intermediate value type (values you work with in the "happy path")'),(0,o.yg)("li",{parentName:"ul"},(0,o.yg)("inlineCode",{parentName:"li"},"ADT"),': The final value or "result" type (typically a sealed trait hierarchy)')),(0,o.yg)("p",null,"Conceptually, ",(0,o.yg)("inlineCode",{parentName:"p"},"Sealed")," is like ",(0,o.yg)("inlineCode",{parentName:"p"},"EitherT")," but oriented toward a workflow that:"),(0,o.yg)("ol",null,(0,o.yg)("li",{parentName:"ol"},"Works with intermediate values (",(0,o.yg)("inlineCode",{parentName:"li"},"A"),") through map/flatMap"),(0,o.yg)("li",{parentName:"ol"},"Can short-circuit to a final result (",(0,o.yg)("inlineCode",{parentName:"li"},"ADT"),") at any point"),(0,o.yg)("li",{parentName:"ol"},"Must ultimately evaluate to a final value of type ",(0,o.yg)("inlineCode",{parentName:"li"},"ADT"))),(0,o.yg)("h3",{id:"the-execution-flow"},"The Execution Flow"),(0,o.yg)("p",null,"A typical Sealed Monad workflow:"),(0,o.yg)("ol",null,(0,o.yg)("li",{parentName:"ol"},"Start with values wrapped in effects (",(0,o.yg)("inlineCode",{parentName:"li"},"F[A]"),", ",(0,o.yg)("inlineCode",{parentName:"li"},"F[Option[A]]"),", etc.)"),(0,o.yg)("li",{parentName:"ol"},"Process these values, potentially short-circuiting with an ",(0,o.yg)("inlineCode",{parentName:"li"},"ADT")," value if validation fails"),(0,o.yg)("li",{parentName:"ol"},"Continue processing until reaching a final result"),(0,o.yg)("li",{parentName:"ol"},"Call ",(0,o.yg)("inlineCode",{parentName:"li"},".run")," to evaluate the computation to ",(0,o.yg)("inlineCode",{parentName:"li"},"F[ADT]"))),(0,o.yg)("h3",{id:"key-operations"},"Key Operations"),(0,o.yg)("p",null,"Sealed Monad provides several categories of operations:"),(0,o.yg)("h4",{id:"extraction-operations"},"Extraction Operations"),(0,o.yg)("p",null,"These help you work with ",(0,o.yg)("inlineCode",{parentName:"p"},"Option")," and other container types:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},"// Extract a value from an Option or return a specified ADT\nval user: Sealed[Future, User, LoginResponse] = \n  findUser(email).valueOr(LoginResponse.InvalidCredentials)\n")),(0,o.yg)("h4",{id:"validation-operations"},"Validation Operations"),(0,o.yg)("p",null,"These let you validate values and short-circuit on failure:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},"// Ensure user is not archived, or return Deleted response\nval activeUser: Sealed[Future, User, LoginResponse] = \n  user.ensure(!_.archived, LoginResponse.Deleted)\n")),(0,o.yg)("h4",{id:"transformation-operations"},"Transformation Operations"),(0,o.yg)("p",null,"These transform intermediate values:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},"// Map user to a token\nval token: Sealed[Future, String, LoginResponse] = \n  user.map(u => issueTokenFor(u))\n")),(0,o.yg)("h4",{id:"side-effect-operations"},"Side Effect Operations"),(0,o.yg)("p",null,"These let you perform side effects without affecting the computation:"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},'// Log the current state\nval loggedUser: Sealed[Future, User, LoginResponse] = \n  user.tap(u => println(s"Found user: ${u.email}"))\n')),(0,o.yg)("p",null,"All these operations work together to create clean, linear business logic that's easy to read and maintain."),(0,o.yg)("h3",{id:"comparing-with-traditional-approaches"},"Comparing with Traditional Approaches"),(0,o.yg)("p",null,"To demonstrate the value of Sealed Monad, let's compare two implementations of the same login logic:"),(0,o.yg)("h4",{id:"without-sealed-monad"},"Without Sealed Monad"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},"def login(email: String,\n          findUser: String => Future[Option[User]],\n          findAuthMethod: (Long, Provider) => Future[Option[AuthMethod]],\n          issueTokenFor: User => String,\n          checkAuthMethod: AuthMethod => Boolean): Future[LoginResponse] = {\n  findUser(email).flatMap {\n    case None =>\n      Future.successful(LoginResponse.InvalidCredentials)\n    case Some(user) if user.archived =>\n      Future.successful(LoginResponse.Deleted)\n    case Some(user) =>\n      findAuthMethod(user.id, Provider.EmailPass).map {\n        case None => \n          LoginResponse.ProviderAuthFailed\n        case Some(authMethod) if !checkAuthMethod(authMethod) => \n          LoginResponse.InvalidCredentials\n        case Some(_) => \n          LoginResponse.LoggedIn(issueTokenFor(user))\n      }\n  }\n}\n")),(0,o.yg)("h4",{id:"with-sealed-monad"},"With Sealed Monad"),(0,o.yg)("pre",null,(0,o.yg)("code",{parentName:"pre",className:"language-scala"},"import pl.iterators.sealedmonad.syntax._\n\ndef login(email: String,\n          findUser: String => Future[Option[User]],\n          findAuthMethod: (Long, Provider) => Future[Option[AuthMethod]],\n          issueTokenFor: User => String,\n          checkAuthMethod: AuthMethod => Boolean): Future[LoginResponse] = {\n  (for {\n    user <- findUser(email)\n      .valueOr(LoginResponse.InvalidCredentials)\n      .ensure(!_.archived, LoginResponse.Deleted)\n    \n    authMethod <- findAuthMethod(user.id, Provider.EmailPass)\n      .valueOr(LoginResponse.ProviderAuthFailed)\n      .ensure(checkAuthMethod, LoginResponse.InvalidCredentials)\n  } yield LoginResponse.LoggedIn(issueTokenFor(user))).run\n}\n")),(0,o.yg)("p",null,"The Sealed Monad version is more concise, easier to follow, and effectively communicates the business logic in a linear, step-by-step fashion."))}c.isMDXComponent=!0}}]);