---
id: intro
title: Introduction
slug: /
---

# Introduction to Sealed Monad

## What is Sealed Monad?

Sealed Monad is a functional programming pattern and Scala library designed to provide an alternative approach to error handling and composition of computations. It addresses limitations in traditional approaches like `Either[T]` and Monad Error by viewing computations as tree structures with enforced patterns.

At its core, Sealed Monad can be thought of as "EitherT on steroids, with more human-readable method names." It provides a clean, intuitive, and type-safe way to handle errors in functional programming, particularly in complex business logic scenarios.

## Purpose

The primary purpose of Sealed Monad is to solve several common challenges in functional error handling:

1. **Eliminating Boilerplate**: When combining functions that return different error types, traditional approaches require boilerplate code to map errors to a common type. Sealed Monad eliminates this need.

2. **Enforcing Error Handling**: Sealed Monad ensures that all error cases are handled at compile time, preventing runtime surprises.

3. **Improving Readability**: By providing intuitive combinators and a for-comprehension-friendly interface, Sealed Monad makes error handling code more readable and maintainable.

4. **Facilitating Composition**: Sealed Monad makes it easier to compose functions with different error types without explicit handling.

## Key Advantages

### 1. Type Safety

Sealed Monad enforces handling of all error cases at compile time through its type system. This "reach ADT or die" approach ensures that you have to prove you've handled all possible error cases, preventing runtime surprises.

### 2. Reduced Boilerplate

With Sealed Monad, there's no need to map between different error types when combining functions. This significantly reduces boilerplate code and makes your codebase cleaner and more maintainable.

### 3. Familiar Interface

Sealed Monad provides combinators similar to `Either[T]`, making it easy for developers familiar with functional programming in Scala to adopt. It works seamlessly with Scala's for-comprehensions.

### 4. Controlled Flow

By viewing computations as tree structures where:
- Leaves are instances of a response ADT (Algebraic Data Type)
- Intermediate nodes represent ongoing computations
- Reaching an ADT short-circuits the computation (similar to Either's left case)

Sealed Monad ensures that computations follow the desired pattern and handle errors appropriately.

### 5. Business Logic Oriented

Sealed Monad is specifically designed for business logic scenarios where error handling is complex and critical. It provides a more expressive and maintainable way to handle errors in these contexts.

## Core Concept: Computations as Trees

One of the fundamental concepts in Sealed Monad is viewing computations as tree structures:

- **Leaves** are instances of a response ADT (Algebraic Data Type)
- **Intermediate nodes** represent ongoing computations
- When you reach an ADT, the computation short-circuits (similar to Either's left case)

This tree-based view allows for more controlled and expressive error handling, particularly in complex business logic scenarios.

## When to Use Sealed Monad

Sealed Monad is particularly useful in scenarios where:

- You have complex business logic with multiple error types
- Type safety and compile-time error handling are critical
- You want to reduce boilerplate in error handling code
- You need a more expressive and maintainable approach to error handling

## Getting Started

To start using Sealed Monad in your Scala projects, check out the [Installation Guide](installation) and [Core Concepts](core-concepts) documentation.

## Further Reading

These blog posts provide additional context on why structured error handling and clean design matter:

- [Code Complexity Metrics: Writing Clean, Maintainable Software](https://www.iteratorshq.com/blog/code-complexity-metrics-writing-clean-maintainable-software/) - Learn about code complexity
- [Achieving Data Integrity: Data Validation & Enforcing Constraints](https://www.iteratorshq.com/blog/achieving-data-integrity-data-validation-enforcing-constraints/) - Discover how to enforce data integrity
- [Building Bulletproof Software by Using Error Handling](https://www.iteratorshq.com/blog/building-bulletproof-software-by-using-error-handling/) - Understand proper error handling
