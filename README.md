# Aedile

![main](https://github.com/sksamuel/aedile/workflows/main/badge.svg)
[<img src="https://img.shields.io/maven-central/v/com.sksamuel.aedile/aedile-core.svg?label=latest%20release"/>](http://search.maven.org/#search%7Cga%7C1%aedile)
[<img src="https://img.shields.io/nexus/s/https/oss.sonatype.org/com.sksamuel.aedile/aedile-core.svg?label=latest%20snapshot&style=plastic"/>](https://oss.sonatype.org/content/repositories/snapshots/com/sksamuel/aedile/aedile-core/)

Aedile is a simple Kotlin wrapper for [Caffeine](https://github.com/ben-manes/caffeine) which prefers coroutines rather
than Java futures.

See [changelog](changelog.md)

## Features

* **Suspendable functions:** Rather than use Java's `CompletableFuture`s, all operations on Aedile are suspendable and
  executed in their own coroutines.
* **Backed by Caffeine:** This is not a new cache implementation with its own bugs and quirks, but a simple wrapper
  around Caffeine which has been used on the JVM for years.
* **Kotlin durations:** Specify expiration and refresh times in `kotlin.time.Duration` rather than Java durations.
* **Kotlin functions:** Whereever a function is required - eg eviction listener - Aedile supports Kotlin functions
  rather than Java's Function interface.

## Usage

Add Aedile to your build:

```groovy
implementation 'com.sksamuel.aedile:aedile-core:<version>'
```

Next, in your code, create a cache through the cahce builder with the `caffeineBuilder()` function,
supplying the key / value types.

```kotlin
val cache = caffeineBuilder<String, String>().build()
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
val cache = caffeineBuilder<String, String>().build {
   delay(1)
   "value"
}

cache.get("foo") // uses default compute
cache.get("bar") { "other" } // uses specific compute function
```

## Configuration

When creating the cache, Aedile supports most Caffeine configuration options. The exception is `softValues`
and `weakValues` which are not supported with asynchronous operations. Since Aedile's purpose is to support coroutines,
these options are ignored.

To configure the builder we supply a configuration lambda:

```kotlin
val cache = caffeineBuilder<String, String> {
   maximumSize = 100
   initialCapacity = 10
}.build()
```

## Evictions

Caffeine provides different approaches to timed eviction:

* expireAfterAccess(duration): Expire entries after the specified duration has passed since the entry was last accessed
  by a read or a write. This could be desirable if the cached data is bound to a session and expires due to inactivity.

* expireAfterWrite(duration): Expire entries after the specified duration has passed since the entry was created, or the
  most recent replacement of the value. This could be desirable if cached data grows stale after a certain amount of
  time.

* expireAfter(expiry): Pass an implementation of `Expiry` which has methods for specifying that expiry should occur
  either a duration from insert, a duration from last refresh, or a duration from last read.

You can specify a suspendable function to listen to evictions:

```kotlin
val cache = caffeineBuilder<String, String> {
   evictionListener = { key, value, cause -> when (cause) {
      RemovalCause.SIZE -> println("Removed due to size constraints")
      else -> delay(100) // suspendable for no real reason, but just to show you can!!
   } }
}.build()
```

## Specify Dispatchers

By default, Aedile will use `Dispatchers.IO` for executing the compute functions. You can specify your own
dispatcher by specifying when configuring the builder.

```kotlin
val cacheDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
val cache = caffeineBuilder<String, String>() {
   this.dispatcher = cacheDispatcher
}.build()
```

You can also specify a custom `CoroutineScope` if required. Note that this scope should not be cancelled or closed while
the cache is in use.

## Metrics

Aedile provides [Micrometer](https://micrometer.io) integration which simply delegates to the Caffeine micrometer
support. To use this, import the `com.sksamuel.aedile:aedile-micrometer` module, and bind to a micrometer registry:

```kotlin
CacheMetrics(cache, "my-cache-name").bindTo(registry)
// or
LoadingCacheMetrics(cache, "my-cache-name").bindTo(registry)
```
