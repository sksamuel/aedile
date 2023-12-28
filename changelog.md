# Changelog

### 1.3.1

* Added `getOrNull` with compute that allows for the compute function to return nulls.

### 1.3.0

* Deprecated `caffeineBuilder` for `cacheBuilder`. This new builder defaults to using the calling context for compute
  function excecution. To use the previous behaviour, use `cacheBuilder`, and set `useCallingContext` to false. The
  deprecated builder retains the previous behaviour.
* Added `getOrNull` as a non suspendable version of `getIfPresent`.
* Added `getAll` with bulk compute.

### 1.2.3

* Added support for custom schedulers

### 1.2.2

* Added support for `softValues`
* Added support for `removalListener`
* Updated dependencies on `caffeine` to 3.1.6 and `micrometer` (in the optional `aedile-micrometer` module) to 1.11.0

### 1.2.1

* Added support for `buildAll`.

### 1.2.0

* Added `contains` operation to return a Boolean if the cache contains a given key
* Added `invalidate` and `invalidateAll` which will block and clear the cache of a single entry or all entries
* Added `asDeferredMap` to return a representation of the Cache as a Map of `Deferred` values.
* Added operator overloaded `put` operation which inserts a precomputed value

### 1.1.2

* Use supervisor scope for suspend computations #1

### 1.1.1

* Support weak keys.

### 1.1.0

* Support custom `CoroutineScope`.
* EvictionListener function should be suspendable.
* Support `Expiry` interface in builder.
* Support `StatsCounter` in builder.

### 1.0.2

* `Cache` should support `getAll`

### 1.0.1

* Support `LoadingCache` micrometer metrics.

### 1.0.0

* First published release.
