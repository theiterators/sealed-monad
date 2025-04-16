---
id: installation
title: Installation Guide
slug: /installation
---

## Overview

Sealed Monad is a Scala library for business logic-oriented, for-comprehension-style error handling. This guide covers installation methods for various build tools and platforms.

## Requirements

### Scala Versions
Sealed Monad supports the following Scala versions:
- Scala 2.13.x
- Scala 3.x

### Platforms
Sealed Monad is available for:
- JVM
- Scala.js
- Scala Native

### Dependencies
Sealed Monad has the following dependencies:
- cats-core

## Installation

### SBT
Add the following to your `build.sbt` file:

```scala
libraryDependencies += "pl.iterators" %% "sealed-monad" % "1.3"
```

The `%%` operator automatically adds the appropriate Scala version suffix to the artifact name.

### Mill
Add this to your `build.sc` file:
```scala
import $ivy.`pl.iterators::sealed-monad:1.3`
```
Or, if using a module definition:
```scala
object myModule extends ScalaModule {
  def scalaVersion = "2.13.12" // or your Scala version
  def ivyDeps = Agg(
    ivy"pl.iterators::sealed-monad:1.3"
  )
}
```

### Maven
Add this to your `pom.xml` file:
```xml
<dependency>
    <groupId>pl.iterators</groupId>
    <artifactId>sealed-monad_${scala.binary.version}</artifactId>
    <version>1.3</version>
</dependency>
```
Replace `${scala.binary.version}` with `2.13` or `3`.

### Gradle (Kotlin DSL)
If using the Scala plugin in Gradle, add this to `build.gradle.kts`:
```kotlin
dependencies {
    implementation("pl.iterators:sealed-monad_${scalaBinaryVersion}:1.3")
}
```
Define `scalaBinaryVersion` in your `gradle.properties` or inline:
```kotlin
val scalaBinaryVersion = "2.13" // or "3"
```

## Importing Sealed Monad

To use Sealed Monad in your code, import it as follows:

### Basic Import
```scala
import pl.iterators.sealedmonad._
```

### Recommended Import (with syntax extensions)
```scala
import pl.iterators.sealedmonad.syntax._
```

## Verification

To verify that Sealed Monad is correctly installed and imported, you can run a simple test:

```scala
import pl.iterators.sealedmonad.syntax._
import cats.Id

// A simple sealed trait for responses
sealed trait Response
case class Success(value: String) extends Response
case object NotFound extends Response

// Test function using Sealed Monad
def test(input: Option[String]): Id[Response] = {
  (for {
    value <- input.valueOr[Response](NotFound)
  } yield Success(value)).run
}

// Try it out
val result1 = test(Some("Hello"))  // Should be Success("Hello")
val result2 = test(None)           // Should be NotFound
```

## Troubleshooting

### Common Issues

#### Missing Dependency
If you encounter errors like `object sealedmonad is not a member of package pl.iterators`, make sure you've added the correct dependency to your build file and that your build tool has resolved it.

#### Version Conflicts
If you encounter version conflicts with cats or other libraries, you may need to explicitly specify the versions:
```scala
dependencyOverrides += "org.typelevel" %% "cats-core" % "2.10.0"
```

#### Import Issues
If you're having trouble with imports, try the explicit imports mentioned above instead of the wildcard import.

### Compatibility Notes

Sealed Monad is built on top of cats and is designed to work seamlessly with cats-effect for effectful computations. It should be compatible with most libraries in the Typelevel ecosystem.

## Additional Resources

- [Official Documentation](https://theiterators.github.io/sealed-monad/)
- [GitHub Repository](https://github.com/theiterators/sealed-monad)
- [API Documentation](https://javadoc.io/doc/pl.iterators/sealed-monad_2.13/latest/index.html)

## License

Sealed Monad is licensed under the Apache 2.0 License.
