# Changelog

### 1.2.2

* Added support for `softValues`
* Added support for `removalListener`
* Updated dependencies on `caffeine` to 3.1.6  and `micrometer` (in the optional `aedile-micrometer` module) to 1.11.0

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
