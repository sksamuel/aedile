# Aedile

![main](https://github.com/sksamuel/aedile/workflows/main/badge.svg)
[<img src="https://img.shields.io/maven-central/v/com.sksamuel.aedile/aedile-core.svg?label=latest%20release"/>](https://central.sonatype.com/search?q=aedile)
[<img src="https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fcom%2Fsksamuel%2Faedile%2Faedile-core%2Fmaven-metadata.xml&strategy=highestVersion&label=maven-snapshot">](https://central.sonatype.com/repository/maven-snapshots/com/sksamuel/aedile/aedile-core/maven-metadata.xml)

Aedile is a simple Kotlin wrapper for [Caffeine](https://github.com/ben-manes/caffeine) which prefers coroutines rather
than Java futures.

See [changelog](changelog.md)

## Features

* **Suspendable functions:** Rather than use Java's `CompletableFuture`s, all operations on Aedile are suspendable and
  executed in their own coroutines.
* **Backed by Caffeine:** This is not a new cache implementation with its own bugs and quirks, but a simple wrapper
  around Caffeine which has been used on the JVM for years.
* **Kotlin durations:** Specify expiration and refresh times in `kotlin.time.Duration` rather than Java durations.
* **Kotlin functions:** Wherever a function is required - eg eviction listener - Aedile supports Kotlin functions
  rather than Java's Function interface.

## Usage

Add Aedile to your build:

```groovy
implementation 'com.sksamuel.aedile:aedile-core:<version>'
```

Next, in your code, create a cache configuration using the standard Caffeine builder. Then, instead of using the
`buildAsync` methods that Caffeine provides, use the `asCache` or `asLoadingCache` methods that Aedile provides.

```kotlin
val cache = Caffeine.newBuilder().asCache<String, String>()
```

With this cache we can request values if present, or supply a suspendable function to compute them.

```kotlin
val value1 = cache.getIfPresent("foo") // value or null

val value2 = cache.get("foo") {
   delay(100) // look ma, we support suspendable functions!
   "value"
}
```

The `asLoadingCache` method supports a generic compute function which is used if no specific compute function is
provided.

```kotlin
val cache = Caffeine.newBuilder().asLoadingCache<String, String>() {
   delay(1) // look ma, we support suspendable functions!
   "value"
}

cache.get("foo") // uses default compute, will return "value"
cache.get("bar") { "other" } // uses specific compute function to return "other"
```

## Configuration

When creating the cache, Aedile wraps the standard Caffeine configuration options, adding extension functions to make
it easier to use Kotlin types - such as `kotlin.time.Duration` rather than Java's `Duration`.

For example:

```kotlin
val cache = Caffeine
   .newBuilder()
   .expireAfterWrite(1.hours) // supports kotlin.time.Duration
   .maximumSize(100) // standard Caffeine option
   .asCache<String, String>()
```

## Evictions

Caffeine provides different approaches to eviction:

* expireAfterAccess(duration): Expire entries after the specified duration has passed since the entry was last accessed
  by a read or write. This could be desirable if the cached data is bound to a session and expires due to inactivity.

* expireAfterWrite(duration): Expire entries after the specified duration has passed since the entry was created, or the
  most recent replacement of the value. This could be desirable if cached data grows stale after a certain amount of
  time.

* expireAfter(expiry): Pass an implementation of `Expiry` which has methods for specifying that expiry should occur
  either after a duration from insert, a duration from last refresh, or a duration from last read.

* invalidate / invalidateAll: Programatically remove entries based on their key(s) or remove all entries. In the case of
  a loading cache, any currently loading values may not be removed.

You can specify a suspendable function to listen to evictions using the `withEvictionListener` method.

```kotlin
val cache = Caffeine
   .newBuilder()
   .expireAfterWrite(1.hours) // supports kotlin.time.Duration
   .maximumSize(100) // standard Caffeine option
   .asCache<String, String>()
   .withEvictionListener { key, value, cause ->
      when (cause) {
         RemovalCause.SIZE -> println("Removed due to size constraints")
         else -> delay(100) // suspendable for no real reason, but just to show you can!!
      }
   }.asCache<String, String>()
```

## Removals

Similar to evictions, you can specify a suspendable function to listen to removals using the `withRemovalListener`
method.

```kotlin
val cache = Caffeine
   .newBuilder()
   .asCache<String, String>()
   .withRemovalListener { key, value, cause ->
      ...
   }.asCache<String, String>()
```

## Coroutine Context

Aedile will use the context from the calling function for executing the compute functions. You can
specify your own context by just switching the context like with any suspendable call.

```kotlin
val cache = Caffeine.newBuilder().asCache<String, String>()
val value = cache.get("foo") {
   withContext(Dispatchers.IO) {
      // blocking database call
   }
}
```
