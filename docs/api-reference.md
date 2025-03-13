---
sidebar_position: 4
---

# API Reference

This page documents the core operations and syntax provided by Sealed Monad. The library offers a rich set of operators tailored to different use cases and scenarios.

## Core Type

The fundamental type in Sealed Monad is `Sealed[F[_], +A, +ADT]` with three type parameters:

- `F[_]`: The effect type (e.g., `Future`, `IO`, `Id`)
- `A`: The intermediate value type 
- `ADT`: The final value or "result" type (typically a sealed trait hierarchy)

## Creating Sealed Instances

### From Effects

```scala
// From F[A]
Sealed[F, A](fa: F[A]): Sealed[F, A, Nothing]
Sealed.apply[F, A](value: => F[A]): Sealed[F, A, Nothing]

// From pure values 
Sealed.liftF[F, A](value: A): Sealed[F, A, Nothing]
Sealed.unit[F]: Sealed[F, Unit, Nothing]

// From ADT values
Sealed.seal[F, ADT](value: ADT): Sealed[F, Nothing, ADT]
Sealed.result[F, ADT](value: => F[ADT]): Sealed[F, Nothing, ADT]
```

### From Options

```scala
// valueOr - extract value from Option or return ADT
Sealed.valueOr[F, A, ADT](fa: => F[Option[A]], orElse: => ADT): Sealed[F, A, ADT]
Sealed.valueOrF[F, A, ADT](fa: => F[Option[A]], orElse: => F[ADT]): Sealed[F, A, ADT]

// emptyOr - inverse of valueOr, returns Unit if Option is empty, otherwise ADT
Sealed.emptyOr[F, A, ADT](fa: => F[Option[A]], orElse: A => ADT): Sealed[F, Unit, ADT]
Sealed.emptyOrF[F, A, ADT](fa: => F[Option[A]], orElse: A => F[ADT]): Sealed[F, Unit, ADT]
```

### From Either

```scala
// handleError - convert F[Either[A, B]] to Sealed[F, B, ADT]
Sealed.handleError[F, A, B, ADT](fa: F[Either[A, B]])(f: A => ADT): Sealed[F, B, ADT]
```

## Syntax Extensions

Sealed Monad provides extension methods for various types:

### F[A] Extensions

```scala
// Create a Sealed instance from F[A]
fa.seal[ADT]: Sealed[F, A, ADT]

// Create with validation
fa.ensure[ADT](pred: A => Boolean, orElse: => ADT): Sealed[F, A, ADT]
fa.ensureF[ADT](pred: A => Boolean, orElse: => F[ADT]): Sealed[F, A, ADT]
fa.ensureNot[ADT](pred: A => Boolean, orElse: => ADT): Sealed[F, A, ADT]
fa.ensureNotF[ADT](pred: A => Boolean, orElse: => F[ADT]): Sealed[F, A, ADT]

// Create with validation that uses value
fa.ensureOr[ADT](pred: A => Boolean, orElse: A => ADT): Sealed[F, A, ADT]
fa.ensureOrF[ADT](pred: A => Boolean, orElse: A => F[ADT]): Sealed[F, A, ADT] 
fa.ensureNotOr[ADT](pred: A => Boolean, orElse: A => ADT): Sealed[F, A, ADT]
fa.ensureNotOrF[ADT](pred: A => Boolean, orElse: A => F[ADT]): Sealed[F, A, ADT]

// From Either
fa.attempt[B, ADT](f: A => Either[ADT, B]): Sealed[F, B, ADT]
fa.attemptF[B, ADT](f: A => F[Either[ADT, B]]): Sealed[F, B, ADT]
```

### F[Option[A]] Extensions

```scala
// Extract value from Option or return ADT  
fa.valueOr[ADT](orElse: => ADT): Sealed[F, A, ADT]
fa.valueOrF[ADT](orElse: => F[ADT]): Sealed[F, A, ADT]

// Return Unit if Option is empty, otherwise ADT from A
fa.emptyOr[ADT](orElse: A => ADT): Sealed[F, Unit, ADT]
fa.emptyOrF[ADT](orElse: A => F[ADT]): Sealed[F, Unit, ADT]
```

### F[Either[A, B]] Extensions

```scala
// Merge either values to ADT
fa.merge[ADT](f: Either[A, B] => ADT): Sealed[F, ADT, ADT]
fa.mergeF[ADT](f: Either[A, B] => F[ADT]): Sealed[F, ADT, ADT] 

// Handle error (left side)
fa.handleError[ADT](f: A => ADT): Sealed[F, B, ADT]

// Convert to Sealed (error becomes ADT)  
fa.fromEither: Sealed[F, B, A]
```

### Pure Value Extensions

```scala
// Seal a pure value
a.seal[F]: Sealed[F, Nothing, A]

// Lift a pure value to intermediate position
a.liftSealed[F, ADT]: Sealed[F, A, ADT]
```

### Either Extensions

```scala
// Convert Either to Sealed
either.rethrow[F]: Sealed[F, A, ADT]
```

## Transformations

Once you have a `Sealed` instance, you can use these operations:

### Basic Transformations

```scala
// Transform intermediate value
sealed.map[B](f: A => B): Sealed[F, B, ADT]
sealed.flatMap[B, ADT1 >: ADT](f: A => Sealed[F, B, ADT1]): Sealed[F, B, ADT1]
sealed.void: Sealed[F, Unit, ADT]

// With effects
sealed.semiflatMap[B](f: A => F[B]): Sealed[F, B, ADT]
sealed.leftSemiflatMap[ADT1 >: ADT](f: ADT => F[ADT1]): Sealed[F, A, ADT1] 
sealed.biSemiflatMap[B, ADT1 >: ADT](fa: ADT => F[ADT1], fb: A => F[B]): Sealed[F, B, ADT1]
```

### Completion Operations

```scala
// Complete with final ADT value
sealed.complete[ADT1 >: ADT](f: A => ADT1): Sealed[F, Nothing, ADT1]
sealed.completeWith[ADT1 >: ADT](f: A => F[ADT1]): Sealed[F, Nothing, ADT1]
```

### Either Conversions

```scala
// Convert to/from Either
sealed.rethrow[B, ADT1 >: ADT]: Sealed[F, B, ADT1] // when A is Either[ADT1, B]
sealed.either: Sealed[F, Either[ADT, A], ADT]

// Work with Either
sealed.attempt[B, ADT1 >: ADT](f: A => Either[ADT1, B]): Sealed[F, B, ADT1]
sealed.attemptF[B, ADT1 >: ADT](f: A => F[Either[ADT1, B]]): Sealed[F, B, ADT1]
```

### Folding

```scala
// Apply different functions based on current state
sealed.foldM[B, ADT1 >: ADT](left: ADT => Sealed[F, B, ADT1], right: A => Sealed[F, B, ADT1]): Sealed[F, B, ADT1]
```

### Side Effects

```scala
// Inspect current state
sealed.inspect(pf: PartialFunction[Either[ADT, A], Any]): Sealed[F, A, ADT]
sealed.inspectF(pf: PartialFunction[Either[ADT, A], F[Any]]): Sealed[F, A, ADT]

// Apply side effects on intermediate value
sealed.tap[B](f: A => B): Sealed[F, A, ADT]
sealed.flatTap[B](f: A => F[B]): Sealed[F, A, ADT]
sealed.flatTapWhen[B](cond: A => Boolean, f: A => F[B]): Sealed[F, A, ADT]
sealed.flatTapWhenNot[B](cond: A => Boolean, f: A => F[B]): Sealed[F, A, ADT]

// Apply side effects on ADT
sealed.leftSemiflatTap[C](f: ADT => F[C]): Sealed[F, A, ADT]

// Apply side effects on either value
sealed.biSemiflatTap[B, C](fa: ADT => F[C], fb: A => F[B]): Sealed[F, A, ADT]
```

### Validation

```scala
// Ensure condition is met
sealed.ensure[ADT1 >: ADT](pred: A => Boolean, orElse: => ADT1): Sealed[F, A, ADT1]
sealed.ensureF[ADT1 >: ADT](pred: A => Boolean, orElse: => F[ADT1]): Sealed[F, A, ADT1]

// Ensure condition is not met
sealed.ensureNot[ADT1 >: ADT](pred: A => Boolean, orElse: => ADT1): Sealed[F, A, ADT1]
sealed.ensureNotF[ADT1 >: ADT](pred: A => Boolean, orElse: => F[ADT1]): Sealed[F, A, ADT1]

// With access to A in orElse
sealed.ensureOr[ADT1 >: ADT](pred: A => Boolean, orElse: A => ADT1): Sealed[F, A, ADT1]
sealed.ensureOrF[ADT1 >: ADT](pred: A => Boolean, orElse: A => F[ADT1]): Sealed[F, A, ADT1]
sealed.ensureNotOr[ADT1 >: ADT](pred: A => Boolean, orElse: A => ADT1): Sealed[F, A, ADT1]
sealed.ensureNotOrF[ADT1 >: ADT](pred: A => Boolean, orElse: A => F[ADT1]): Sealed[F, A, ADT1]
```

### Evaluation

```scala
// Evaluate to final result
sealed.run[ADT1 >: ADT](implicit ev: A <:< ADT1, F: Monad[F]): F[ADT1]
```

## Instances

```scala
// Monad instance for Sealed
implicit def sealedMonad[F[_], ADT]: Monad[Sealed[F, _, ADT]]
```

## Usage Best Practices

1. **Start with effects** - Begin your computation with extension methods like `.valueOr`, `.ensure`, or `.seal`
2. **Chain with for-comprehensions** - Use for-comprehensions to chain operations
3. **End with run** - Complete your computation with `.run` to get the final `F[ADT]`
4. **Structure complex logic in steps** - Break down complex flows into well-named helper methods
5. **Use descriptive ADTs** - Design your ADT to clearly communicate all possible outcomes