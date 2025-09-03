# Changelog

### 3.0.1 (August 2025)

* fix loading cache nullability by @tKe in #50

### 2.1.2 (April 2025)

* Added return values for refresh/refreshAll

### 2.1.1 (April 2025)

* Updated refresh/refreshAll to be suspendable

### 2.1.0 (April 2025)

* Added refresh/refreshAll to LoadingCache

### 2.0.3 (January 2025)

* Fixes the same covariance issue for `asLoadingCache` and `asBulkLoadingCache`.

### 2.0.2 (December 2024)

* 2.0.0 had an issue with the covariance of the Caffeine builders for `asCache` when using `expireAfter`

### 2.0.1 (December 2024)

* 2.0.0 was unable to compile when using an eviction or removal listener. This release fixes that by adding
  `withEvictionListener` and `withRemovalListener` extension functions to the `Caffeine` builder.

### 2.0.0 (December 2024)

* Deprecated cacheBuilder in favor of extending `Caffeine.newBuilder`. See [README](README.md) for full details.
   * New extension functions for `Caffeine` builder support more natural coroutine operations.
* Deprecated `aedile-micrometer` module in favor of Micrometer's native support for Caffeine.
* Fixed coroutine cancellation propagation #30

### 1.3.1  (December 2023)

* Added `getOrNull` with compute that allows for the compute function to return nulls.

### 1.3.0 (November 2023)

* Deprecated `caffeineBuilder` for `cacheBuilder`. This new builder defaults to using the calling context for compute
  function excecution. To use the previous behaviour, use `cacheBuilder`, and set `useCallingContext` to false. The
  deprecated builder retains the previous behaviour.
* Added `getOrNull` as a non suspendable version of `getIfPresent`.
* Added `getAll` with bulk compute.

### 1.2.3 (June 2023)

* Added support for custom schedulers

### 1.2.2 (May 2023)

* Added support for `softValues`
* Added support for `removalListener`
* Updated dependencies on `caffeine` to 3.1.6 and `micrometer` (in the optional `aedile-micrometer` module) to 1.11.0

### 1.2.1 (May 2023)

* Added support for `buildAll`.

### 1.2.0 (December 2022)

* Added `contains` operation to return a Boolean if the cache contains a given key
* Added `invalidate` and `invalidateAll` which will block and clear the cache of a single entry or all entries
* Added `asDeferredMap` to return a representation of the Cache as a Map of `Deferred` values.
* Added operator overloaded `put` operation which inserts a precomputed value

### 1.1.2 (October 2022)

* Use supervisor scope for suspend computations #1

### 1.1.1 (September 2022)

* Support weak keys.

### 1.1.0 (September 2022)

* Support custom `CoroutineScope`.
* EvictionListener function should be suspendable.
* Support `Expiry` interface in builder.
* Support `StatsCounter` in builder.

### 1.0.2 (September 2022)

* `Cache` should support `getAll`

### 1.0.1 (September 2022)

* Support `LoadingCache` micrometer metrics.

### 1.0.0 (September 2022)

* First published release.
