# Aedile

![main](https://github.com/sksamuel/aedile/workflows/main/badge.svg)
[<img src="https://img.shields.io/maven-central/v/com.sksamuel.aedile/aedile.svg?label=latest%20release"/>](http://search.maven.org/#search%7Cga%7C1%aedile)
[<img src="https://img.shields.io/nexus/s/https/oss.sonatype.org/com.sksamuel.aedile/aedile.svg?label=latest%20snapshot&style=plastic"/>](https://oss.sonatype.org/content/repositories/snapshots/com/sksamuel/aedile/aedile/)

Aedile is a simple Kotlin wrapper for [Caffeine](https://github.com/ben-manes/caffeine) which prefers coroutines rather
than Java futures.

See [changelog](changelog.md)

## Features

* **Suspendable functions:** Rather than use Java's `CompletableFuture`s, all operations on Aedile are suspendable and
  executed in their own coroutines.
* **Backed by Caffeine:** This is not a new cache implementation with its own bugs and quirks, but a simple wrapper
  around Caffeine which has been used on the JVM for years.

## Usage

Create cache builder with the `caffeineBuilder()` function, supplying the key / value types.

```kotlin
val cache = caffeineBuilder().build<String, String>()
```

With this cache we can request values if present, or supply a suspendable function to compute them.

```kotlin
val value1 = cache.getIfPresent("foo") // value or null

val value2 = cache.getOrPut("foo") {
   delay(100) // look ma, we support suspendable functions!
   "value"
}
```

The build function supports a generic compute function which is used if no specific compute function is provided.

```kotlin
val cache = caffeineBuilder().build<String, String> {
   delay(1)
   "value"
}

cache.get("foo") // uses default compute
cache.get("bar") { "other" } // uses specific compute function
```

## Specify Dispatchers

By default, Aedile will use `Dispatchers.IO` for executing the compute functions. You can specify your own
dispatcher by using `withDispatcher` when configuring the builder.

```kotlin
val cacheDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
val cache = caffeineBuilder().withDispatcher(cacheDispatcher).build<String, String>()
```


## Metrics

Aedile provides [Micrometer](https://micrometer.io) integration which simply delegates to the Caffeine micrometer
support. To use this, import the `com.sksamuel.aedile:aedile-micrometer` module, and bind to a micrometer registry:

```kotlin
AedileMetrics(cache, "my-cache-name").bindTo(registry)
```
