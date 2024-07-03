# sealed-monad

[![Maven Central](https://img.shields.io/maven-central/v/pl.iterators/sealed-monad_2.13.svg)]()
[![GitHub license](https://img.shields.io/badge/license-Apache2.0-blue.svg)](https://raw.githubusercontent.com/theiterators/sealed-monad/master/COPYING)
[![sealed-monad Scala version support](https://index.scala-lang.org/theiterators/sealed-monad/sealed-monad/latest-by-scala-version.svg)](https://index.scala-lang.org/theiterators/sealed-monad/sealed-monad)

Scala library for nice business logic oriented, for-comprehension-style error handling. Write logic that even your manager can understand!

Or in more technical terms, think of EitherT on steroids, with more human-readable method names.

## Installation

Add the following to your `build.sbt`:

```scala
libraryDependencies += "pl.iterators" %% "sealed-monad" % "1.3.0"
```

Available for Scala 2.12.x, 2.13.x and 3.x.

## Usage

```scala
  def createTodo(userId: UUID, organizationId: UUID, request: CreateTodoRequest): IO[CreateTodoResponse] = {
    (for {
      user <- userRepository
        .find(userId) // IO[Option[User]]
        .valueOr(CreateTodoResponse.UserNotFound) // extracts User or returns UserNotFound
      _ <- organizationRepository
        .findFor(userId) // IO[Option[Organization]]
        .valueOr(CreateTodoResponse.UserNotInOrganization) // extracts Organization or returns UserNotInOrganization
        .ensure(_.canCreateTodos(user), CreateTodoResponse.UserNotAllowedToCreateTodos) // checks if user can create todos or returns UserNotAllowedToCreateTodos
      _ <- todoRepository
        .find(request.title) // IO[Option[Todo]]
        .ensure(_.isEmpty, CreateTodoResponse.TodoAlreadyExists) // checks if todo already exists or returns TodoAlreadyExists
      _ <- Todo
        .from(request)
        .pure[IO] // IO[Todo]
        .ensure(_.title.nonEmpty, CreateTodoResponse.TodoTitleEmpty) // checks if todo title is non-empty or returns TodoTitleEmpty
      todo <- todoRepository.insert(Todo(UUID.randomUUID(), request.title)).seal // todo created!
    } yield CreateTodoResponse.Created(todo)).run // compile to IO[CreateTodoResponse]
  }
```

## Documentation

Please refer to the [docs](https://theiterators.github.io/sealed-monad) site.

![logo](https://raw.githubusercontent.com/theiterators/sealed-monad/master/logo.png)

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](https://github.com/theiterators/sealed-monad/blob/master/LICENSE) file for details.
