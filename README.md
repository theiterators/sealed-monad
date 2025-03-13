# sealed-monad

[![Maven Central](https://img.shields.io/maven-central/v/pl.iterators/sealed-monad_2.13.svg)]()
[![GitHub license](https://img.shields.io/badge/license-Apache2.0-blue.svg)](https://raw.githubusercontent.com/theiterators/sealed-monad/master/COPYING)
[![sealed-monad Scala version support](https://index.scala-lang.org/theiterators/sealed-monad/sealed-monad/latest-by-scala-version.svg)](https://index.scala-lang.org/theiterators/sealed-monad/sealed-monad)

Sealed Monad is a Scala library for elegant, business logic-oriented error handling. It enables you to write clear, linear code flows that even non-technical stakeholders can understand.

Think of it as `EitherT` on steroids with more human-readable method names - designed specifically for representing business outcomes in a declarative way.

## Key Benefits

- **Linear Code Flow**: Write top-down, sequential code that's easy to follow
- **Descriptive Error Handling**: No more nested pattern matching or complex conditionals
- **For-Comprehension Friendly**: Clean, pipeline-style code for complex validation workflows
- **ADT-Driven Design**: Naturally works with sealed trait hierarchies to model operation results
- **Business-Logic Focus**: Error handling that focuses on representing business outcomes, not technical errors

## Installation

Add the following to your `build.sbt`:

```scala
libraryDependencies += "pl.iterators" %% "sealed-monad" % "2.0.0"
```

Available for Scala 2.13.x and 3.x in JVM, ScalaNative and Scala.js flavours.

## Usage Example

Here's how Sealed Monad transforms complex business logic into clean, readable code:

```scala
def createTodo(userId: UUID, organizationId: UUID, request: CreateTodoRequest): IO[CreateTodoResponse] = {
  (for {
    // Find user or return UserNotFound response
    user <- userRepository
      .find(userId) // IO[Option[User]]
      .valueOr(CreateTodoResponse.UserNotFound) // extracts User or returns UserNotFound
      
    // Verify organization membership and permissions or return appropriate response
    _ <- organizationRepository
      .findFor(userId) // IO[Option[Organization]]
      .valueOr(CreateTodoResponse.UserNotInOrganization) 
      .ensure(_.canCreateTodos(user), CreateTodoResponse.UserNotAllowedToCreateTodos)
      
    // Check for duplicate todos or return TodoAlreadyExists response
    _ <- todoRepository
      .find(request.title) // IO[Option[Todo]]
      .ensure(_.isEmpty, CreateTodoResponse.TodoAlreadyExists)
      
    // Validate todo properties or return TodoTitleEmpty response
    _ <- Todo
      .from(request)
      .pure[IO] // IO[Todo]
      .ensure(_.title.nonEmpty, CreateTodoResponse.TodoTitleEmpty)
      
    // Create the todo
    todo <- todoRepository.insert(Todo(UUID.randomUUID(), request.title)).seal
  } yield CreateTodoResponse.Created(todo)).run // compile to IO[CreateTodoResponse]
}
```

Without Sealed Monad, this would typically require nested pattern matching, multiple `.flatMap` calls, or complex conditional structures that obscure the business logic.

## Who Should Use Sealed Monad?

Sealed Monad is particularly valuable if you work with:

- Complex validation workflows
- API responses with multiple possible outcomes
- Business logic with branching decisions
- Error-prone operations that need clean error handling

## Documentation

Please refer to the [docs site](https://theiterators.github.io/sealed-monad) for comprehensive documentation, including:

- [Introduction and Core Concepts](https://theiterators.github.io/sealed-monad/docs/intro)
- [Motivations & Design Philosophy](https://theiterators.github.io/sealed-monad/docs/motivations)
- [Practical Use Cases & Comparisons](https://theiterators.github.io/sealed-monad/docs/usecases)
- [API Reference](https://theiterators.github.io/sealed-monad/docs/api-reference)
- [Best Practices](https://theiterators.github.io/sealed-monad/docs/best-practices)

![logo](https://raw.githubusercontent.com/theiterators/sealed-monad/master/logo.png)

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](https://github.com/theiterators/sealed-monad/blob/master/LICENSE) file for details.