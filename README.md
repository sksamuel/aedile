# aedile

A simple Kotlin wrapper for [Caffeine](https://github.com/ben-manes/caffeine) which prefers coroutines rather than Java
futures.

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
   "bar"
}
```
