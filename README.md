# Sealed Monad Documentation

[![Maven Central](https://img.shields.io/maven-central/v/pl.iterators/sealed-monad_2.13.svg)]()
[![GitHub license](https://img.shields.io/badge/license-Apache2.0-blue.svg)](https://raw.githubusercontent.com/theiterators/sealed-monad/master/COPYING)
[![sealed-monad Scala version support](https://index.scala-lang.org/theiterators/sealed-monad/sealed-monad/latest-by-scala-version.svg)](https://index.scala-lang.org/theiterators/sealed-monad/sealed-monad)

---

![logo](https://raw.githubusercontent.com/theiterators/sealed-monad/master/logo.png)

Welcome to the comprehensive documentation for the Sealed Monad project. This documentation provides an in-depth look at the project's design, features, and usage, and is structured to help both beginners and experienced Scala developers get started quickly.

## Table of Contents
- [Overview](#overview)
- [Installation](#installation)
- [Quick Start Guide](#quick-start-guide)
- [Advanced Features](#advanced-features)
- [API Reference](#api-reference)
- [Core Concepts](#core-concepts)
- [Usage Examples](#usage-examples)
- [FAQ](#faq)
- [Best Practices](#best-practices)

## Overview
The Sealed Monad project provides a robust framework for handling computations with a sealed design pattern, enabling safer and more predictable code execution in Scala. This documentation is designed for clarity and ease of navigation, ensuring that all key topics are accessible from a single entry point.

## Installation
To install the Sealed Monad library, follow these steps:

1. Ensure you have Scala installed (version 2.13+ is recommended).
2. Add the following dependency to your sbt configuration:
   ```scala
   libraryDependencies += "pl.iterators" %% "sealed-monad" % "2.0.0"
   ```
3. Update your project dependencies by running:
   ```bash
   sbt update
   ```

Available for Scala 2.13.x and 3.x in JVM, ScalaNative and Scala.js flavours.

## Quick Start Guide
Get started quickly with a simple example. Below is an example Scala program that demonstrates how to use the Sealed Monad:

```scala
// Example Scala code demonstrating the usage of the Sealed Monad
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

Follow these steps to run the example:
1. Create a new Scala project.
2. Add the dependency as described in the Installation section.
3. Copy the example code into your main application file.
4. Build and run the project using sbt.

## Advanced Features
This documentation incorporates the enhanced documentation detailing advanced features of the Sealed Monad, including integration patterns, error handling improvements, and custom extensions. For the complete explanation, please refer to the [Advanced Features](https://theiterators.github.io/sealed-monad/docs/advanced-features) section.

## API Reference
In the API Reference section, you will find a detailed reference for all classes, objects, and methods provided by the Sealed Monad framework. Refer to the [API Reference](https://theiterators.github.io/sealed-monad/docs/api-reference) for complete details.

## Core Concepts
Understand the core concepts behind the Sealed Monad, including its design patterns and functional programming principles. The complete discussion is available in the [Core Concepts](https://theiterators.github.io/sealed-monad/docs/core-concepts) section.

## Usage Examples
Complete and tested usage examples are provided to help you integrate the Sealed Monad into your projects effectively. Please see the [Usage Examples](https://theiterators.github.io/sealed-monad/docs/usage-examples) section for code examples and detailed explanations.

## FAQ
For common questions and troubleshooting tips, refer to the [FAQ](https://theiterators.github.io/sealed-monad/docs/faq) section.

## Best Practices
Learn about the best practices for implementing and using the Sealed Monad in production environments. Detailed suggestions are available in the [Best Practices](https://theiterators.github.io/sealed-monad/docs/best-practices) section.

## Complete Documentation
For the complete and up-to-date documentation, please visit our [documentation site](https://theiterators.github.io/sealed-monad/docs/introduction).


## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](https://github.com/theiterators/sealed-monad/blob/master/LICENSE) file for details.

This documentation has been compiled by integrating the refined materials from previous documentation jobs. For any further questions or suggestions, please reach out to the maintainers.
